package server;

import model.auction.Auction;
import model.auction.AuctionObserver;
import model.user.Bidder;
import model.user.Seller;
import model.user.User;
import service.AuctionManager;
import shared.Protocol;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.CopyOnWriteArrayList;

public class AuctionServer implements AuctionObserver {
    private static final int PORT = 8080;
    private List<ClientHandler> clients = new CopyOnWriteArrayList<>();
    private AuctionManager manager;

    public static void main(String[] args) {
        new AuctionServer().startServer();
    }

    public void startServer() {
        manager = AuctionManager.getInstance();

        // Lắng nghe tất cả các phiên đấu giá đã load từ Database
        for (Auction auction : manager.getAllAuctions()) {
            auction.addObserver(this);
        }

        // Đăng ký callback để broadcast AUCTION_FINISHED khi phiên kết thúc tự động
        manager.setAuctionFinishedCallback((finishedAuction) -> {
            String winnerName = (finishedAuction.getHighestBidder() != null)
                    ? finishedAuction.getHighestBidder().getUsername() : "Không có";
            double finalPrice = finishedAuction.getCurrentPrice();

            String finishMessage = Protocol.BROADCAST_AUCTION_FINISHED + Protocol.DELIMITER
                    + finishedAuction.getId() + Protocol.DELIMITER
                    + winnerName + Protocol.DELIMITER
                    + finalPrice;

            System.out.println("📢 [BROADCAST] Phiên đấu giá kết thúc: " + finishedAuction.getId()
                    + " | Người thắng: " + winnerName + " | Giá: $" + finalPrice);
            broadcast(finishMessage);

            // Cập nhật danh sách cho tất cả Client
            broadcastAuctionList();

            // T11: Cập nhật số dư cho Người thắng và Người bán
            Bidder winner = finishedAuction.getHighestBidder();
            if (winner != null) {
                sendBalanceUpdateToUser(winner.getId(), winner.getBalance());
            }
            Seller seller = (Seller) finishedAuction.getItem().getOwner();
            if (seller != null) {
                sendBalanceUpdateToUser(seller.getId(), seller.getBalance());
            }
        });

        // T10: Đăng ký callback để broadcast ROUND_FINISHED
        manager.setRoundFinishedCallback((finishedRoundAuction) -> {
            String roundMessage = Protocol.BROADCAST_ROUND_FINISHED + Protocol.DELIMITER + finishedRoundAuction.getId();
            System.out.println("📢 [BROADCAST] Vòng đấu giá kết thúc: " + finishedRoundAuction.getId());
            broadcast(roundMessage);
        });

        new Thread(() -> {
            Scanner s = new Scanner(System.in);
            System.out.println("Hệ thống Server đã sẵn sàng. Gõ 'exit' để tắt.");
            while (s.hasNextLine()) {
                if ("exit".equalsIgnoreCase(s.nextLine())) System.exit(0);
            }
        }).start();

        try (ServerSocket serverSocket = new ServerSocket()) {
            serverSocket.setReuseAddress(true);
            serverSocket.bind(new InetSocketAddress(PORT));
            System.out.println("[SERVER] Đang lắng nghe tại cổng " + PORT);

            while (true) {
                Socket socket = serverSocket.accept();
                System.out.println("Có kết nối mới: " + socket.getInetAddress());
                ClientHandler handler = new ClientHandler(socket, this, manager);
                clients.add(handler);
                new Thread(handler).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void removeClient(ClientHandler c) {
        clients.remove(c);
    }

    // Hàm Broadcast gửi một thông điệp (chuỗi) tới tất cả Client đang online
    public void broadcast(String message) {
        for (ClientHandler client : clients) {
            client.sendData(message);
        }
    }

    // THÊM MỚI: Hàm Broadcast gửi DANH SÁCH tài sản mới nhất cho tất cả Client
    public void broadcastAuctionList() {
        List<Auction> list = manager.getAllAuctions();
        for (ClientHandler client : clients) {
            client.sendData(Protocol.RES_AUCTION_LIST);
            client.sendData(list);
        }
    }

    // THÊM MỚI: Hàm Broadcast gửi DANH SÁCH USER mới nhất cho tất cả Client (Real-time cho Admin)
    public void broadcastUserList() {
        List<User> list = manager.getAllUsers();
        for (ClientHandler client : clients) {
            client.sendData(Protocol.RES_USER_LIST);
            client.sendData(list);
        }
    }

    public void sendBalanceUpdateToUser(String userId, double balance) {
        for (ClientHandler client : clients) {
            if (client.getLoggedInUser() != null && client.getLoggedInUser().getId().equals(userId)) {
                client.sendData(Protocol.RES_UPDATE_BALANCE + Protocol.DELIMITER + balance);
            }
        }
    }

    // Xử lý sự kiện khi có giá mới (từ AuctionObserver)
    @Override
    public void update(Auction auction, double newPrice, String topBidderName) {
        String message = Protocol.BROADCAST_NEW_BID + Protocol.DELIMITER +
                auction.getId() + Protocol.DELIMITER +
                newPrice + Protocol.DELIMITER +
                topBidderName;

        System.out.println("📢 [BROADCAST] Đã phát sóng giá mới: " + message);
        broadcast(message);
    }

    @Override
    public void onTimeExtended(Auction auction, int addedSeconds) {
        String message = Protocol.BROADCAST_TIME_EXTENDED + Protocol.DELIMITER +
                auction.getId() + Protocol.DELIMITER + addedSeconds;
        System.out.println("📢 [BROADCAST] Gia hạn phiên đấu giá " + auction.getId() + " thêm " + addedSeconds + "s");
        broadcast(message);
    }
}