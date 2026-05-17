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
import java.util.logging.Level;
import java.util.logging.Logger;

public class AuctionServer implements AuctionObserver {
    private static final Logger LOGGER = Logger.getLogger(AuctionServer.class.getName());
    private static final int PORT = 8080;
    private List<ClientHandler> clients = new CopyOnWriteArrayList<>();
    private AuctionManager manager;
    private volatile boolean isRunning = true;
    private final java.util.concurrent.ExecutorService broadcastExecutor = java.util.concurrent.Executors.newFixedThreadPool(4);

    // Room tracking: AuctionID -> Set of ClientHandlers currently in that room
    private java.util.Map<String, java.util.Set<ClientHandler>> roomParticipants = new java.util.concurrent.ConcurrentHashMap<>();

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
        manager.setAuctionFinishedCallback((Auction finishedAuction, User originalSeller) -> {
            String winnerName = (finishedAuction.getHighestBidder() != null)
                    ? finishedAuction.getHighestBidder().getUsername() : "Không có";
            double finalPrice = finishedAuction.getCurrentPrice();

            String finishMessage = Protocol.BROADCAST_AUCTION_FINISHED + Protocol.DELIMITER
                    + finishedAuction.getId() + Protocol.DELIMITER
                    + winnerName + Protocol.DELIMITER
                    + finalPrice;

            broadcast(finishMessage);
            broadcastAuctionList();

            // Cập nhật số dư realtime cho Người thắng và Người bán
            Bidder winner = finishedAuction.getHighestBidder();
            if (winner != null) {
                sendBalanceUpdateToUser(winner.getId());
            }
            if (originalSeller instanceof Seller) {
                sendBalanceUpdateToUser(originalSeller.getId());
            }
        });



        new Thread(() -> {
            Scanner s = new Scanner(System.in);
            LOGGER.info("Hệ thống Server đã sẵn sàng. Gõ 'exit' để tắt.");
            while (s.hasNextLine()) {
                if ("exit".equalsIgnoreCase(s.nextLine())) System.exit(0);
            }
        }).start();

        try (ServerSocket serverSocket = new ServerSocket()) {
            serverSocket.setReuseAddress(true);
            serverSocket.bind(new InetSocketAddress(PORT));
            LOGGER.info("[SERVER] Đang lắng nghe tại cổng " + PORT);

            while (isRunning) {
                Socket socket = serverSocket.accept();
                LOGGER.info("Có kết nối mới: " + socket.getInetAddress());
                ClientHandler handler = new ClientHandler(socket, this, manager);
                clients.add(handler);
                new Thread(handler).start();
            }
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Lỗi khi khởi chạy Server", e);
        }
    }

    public void removeClient(ClientHandler c) {
        clients.remove(c);
    }

    // Hàm Broadcast gửi một thông điệp (chuỗi) tới tất cả Client đang online
    public void broadcast(String message) {
        broadcastExecutor.submit(() -> {
            for (ClientHandler client : clients) {
                client.sendData(message);
            }
        });
    }

    // THÊM MỚI: Hàm Broadcast gửi DANH SÁCH tài sản mới nhất cho tất cả Client
    public void broadcastAuctionList() {
        broadcastExecutor.submit(() -> {
            List<Auction> list = manager.getAllAuctions();
            for (ClientHandler client : clients) {
                client.sendData(Protocol.RES_AUCTION_LIST);
                client.sendData(list);
            }
        });
    }

    // THÊM MỚI: Hàm Broadcast gửi DANH SÁCH USER mới nhất cho tất cả Client (Real-time cho Admin)
    public void broadcastUserList() {
        broadcastExecutor.submit(() -> {
            List<User> list = manager.getAllUsers();
            for (ClientHandler client : clients) {
                client.sendData(Protocol.RES_USER_LIST);
                client.sendData(list);
            }
        });
    }

    // --- ROOM MANAGEMENT ---
    public void joinRoom(String auctionId, ClientHandler client) {
        roomParticipants.computeIfAbsent(auctionId, k -> java.util.Collections.synchronizedSet(new java.util.HashSet<>()))
                        .add(client);
        broadcastParticipantsCount(auctionId);
    }

    public void leaveRoom(String auctionId, ClientHandler client) {
        java.util.Set<ClientHandler> participants = roomParticipants.get(auctionId);
        if (participants != null) {
            participants.remove(client);
            if (participants.isEmpty()) {
                roomParticipants.remove(auctionId);
            } else {
                broadcastParticipantsCount(auctionId);
            }
        }
    }

    public void broadcastParticipantsCount(String auctionId) {
        java.util.Set<ClientHandler> participants = roomParticipants.get(auctionId);
        int count = (participants != null) ? participants.size() : 0;
        String message = Protocol.BROADCAST_PARTICIPANTS + Protocol.DELIMITER + auctionId + Protocol.DELIMITER + count;
        broadcast(message);
    }

    public void sendBalanceUpdateToUser(String userId) {
        for (ClientHandler client : clients) {
            User user = client.getLoggedInUser();
            if (user != null && user.getId().equals(userId)) {
                double balance = 0.0;
                if (user instanceof Bidder) {
                    balance = ((Bidder) user).getAvailableBalance();
                } else if (user instanceof Seller) {
                    balance = ((Seller) user).getBalance();
                }
                client.sendData(Protocol.RES_UPDATE_BALANCE + Protocol.DELIMITER + balance);
            }
        }
    }

    /**
     * Kiểm tra xem một User đã có phiên đăng nhập nào đang hoạt động chưa.
     */
    public boolean isUserLoggedIn(String userId) {
        if (userId == null) return false;
        for (ClientHandler client : clients) {
            User u = client.getLoggedInUser();
            if (u != null && userId.equals(u.getId())) {
                return true;
            }
        }
        return false;
    }

    // Xử lý sự kiện khi có giá mới (từ AuctionObserver)
    @Override
    public void update(Auction auction, double newPrice, String topBidderName, Bidder previousBidder) {
        String message = Protocol.BROADCAST_NEW_BID + Protocol.DELIMITER +
                auction.getId() + Protocol.DELIMITER +
                newPrice + Protocol.DELIMITER +
                topBidderName;

        LOGGER.info("📢 [BROADCAST] Đã phát sóng giá mới: " + message);
        broadcast(message);

        // T11: Cập nhật số dư cho người vừa bị vượt giá (nếu có)
        if (previousBidder != null) {
            sendBalanceUpdateToUser(previousBidder.getId());
        }
    }

    @Override
    public void onTimeExtended(Auction auction, int addedSeconds) {
        String message = Protocol.BROADCAST_TIME_EXTENDED + Protocol.DELIMITER +
                auction.getId() + Protocol.DELIMITER + addedSeconds;
        LOGGER.info("📢 [BROADCAST] Gia hạn phiên đấu giá " + auction.getId() + " thêm " + addedSeconds + "s");
        broadcast(message);
    }
}