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

    // Lưu danh sách người dùng trong từng phòng đấu giá
    private java.util.Map<String, java.util.Set<ClientHandler>> roomParticipants = new java.util.concurrent.ConcurrentHashMap<>();

    public static void main(String[] args) {
        new AuctionServer().startServer();
    }

    public void startServer() {
        manager = AuctionManager.getInstance();

        // Theo dõi tất cả phiên đấu giá từ Database
        for (Auction auction : manager.getAllAuctions()) {
            auction.addObserver(this);
        }

        // Gửi thông báo khi phiên đấu giá kết thúc
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

            // Cập nhật số dư cho người thắng và người bán
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

    // Gửi thông báo tới tất cả người dùng đang online
    public void broadcast(String message) {
        broadcastExecutor.submit(() -> {
            for (ClientHandler client : clients) {
                client.sendData(message);
            }
        });
    }

    // Cập nhật danh sách phiên đấu giá cho tất cả người dùng
    public void broadcastAuctionList() {
        broadcastExecutor.submit(() -> {
            List<Auction> list = manager.getAllAuctions();
            for (ClientHandler client : clients) {
                client.sendData(Protocol.RES_AUCTION_LIST);
                client.sendData(list);
            }
        });
    }

    // Cập nhật danh sách người dùng cho Admin
    public void broadcastUserList() {
        broadcastExecutor.submit(() -> {
            List<User> list = service.UserService.getInstance().getAllUsers();
            for (ClientHandler client : clients) {
                client.sendData(Protocol.RES_USER_LIST);
                client.sendData(list);
            }
        });
    }

    // --- QUẢN LÝ PHÒNG ĐẤU GIÁ ---
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
        StringBuilder sb = new StringBuilder(Protocol.BROADCAST_PARTICIPANTS)
                .append(Protocol.DELIMITER).append(auctionId)
                .append(Protocol.DELIMITER).append(count);
        if (participants != null) {
            for (ClientHandler client : participants) {
                User user = client.getLoggedInUser();
                if (user != null) {
                    sb.append(Protocol.DELIMITER).append(user.getUsername());
                }
            }
        }
        broadcast(sb.toString());
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

    // Kiểm tra người dùng đã đăng nhập chưa
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

    // Xử lý khi có lượt đặt giá mới
    @Override
    public void update(Auction auction, double newPrice, String topBidderName, Bidder previousBidder) {
        String message = Protocol.BROADCAST_NEW_BID + Protocol.DELIMITER +
                auction.getId() + Protocol.DELIMITER +
                newPrice + Protocol.DELIMITER +
                topBidderName;

        LOGGER.info("📢 [BROADCAST] Đã phát sóng giá mới: " + message);
        broadcast(message);

        // Hoàn tiền cho người vừa bị vượt giá
        if (previousBidder != null) {
            sendBalanceUpdateToUser(previousBidder.getId());
        }
        
        // Trừ tiền người đang giữ giá cao nhất
        if (auction.getHighestBidder() != null) {
            sendBalanceUpdateToUser(auction.getHighestBidder().getId());
        }
    }

    @Override
    public void onTimeExtended(Auction auction, int addedSeconds) {
        String message = Protocol.BROADCAST_TIME_EXTENDED + Protocol.DELIMITER +
                auction.getId() + Protocol.DELIMITER + addedSeconds;
        LOGGER.info("📢 [BROADCAST] Gia hạn phiên đấu giá " + auction.getId() + " thêm " + addedSeconds + "s");
        broadcast(message);
    }

    @Override
    public void onAutoBidExpired(Auction auction, Bidder bidder) {
        for (ClientHandler client : clients) {
            User u = client.getLoggedInUser();
            if (u != null && u.getId().equals(bidder.getId())) {
                client.sendData(Protocol.REQ_AUTOBID + Protocol.DELIMITER + Protocol.RES_SUCCESS + Protocol.DELIMITER + "CANCEL");
                break;
            }
        }
    }

    // Đuổi tất cả người dùng khỏi phòng
    public void broadcastRoomKicked(String auctionId, String reason) {
        java.util.Set<ClientHandler> participants = roomParticipants.get(auctionId);
        if (participants != null) {
            String message = Protocol.BROADCAST_ROOM_KICKED + Protocol.DELIMITER + auctionId + Protocol.DELIMITER + reason;
            for (ClientHandler client : participants) {
                client.sendData(message);
            }
            // Không xóa roomParticipants ở đây vì Client sẽ tự gửi lệnh LEAVE_ROOM
        }
    }

    // Đuổi một người dùng cụ thể khỏi phòng
    public void kickUserFromRoom(String auctionId, String targetUsername) {
        java.util.Set<ClientHandler> participants = roomParticipants.get(auctionId);
        if (participants != null) {
            for (ClientHandler client : participants) {
                User u = client.getLoggedInUser();
                if (u != null && targetUsername.equals(u.getUsername())) {
                    String message = Protocol.BROADCAST_ROOM_KICKED + Protocol.DELIMITER + auctionId + Protocol.DELIMITER + "Bạn đã bị quản trị viên đuổi khỏi phòng!";
                    client.sendData(message);
                    LOGGER.info("Bị đuổi: " + u.getUsername() + " khỏi phòng " + auctionId);
                    // Không xóa khỏi participants ở đây vì Client sẽ nhận thông báo và gửi LEAVE_ROOM
                    break; // Giả sử username là duy nhất
                }
            }
        }
    }

    // Ép người dùng đăng xuất (khi bị khóa tài khoản)
    public void forceLogoutUser(String targetUserId) {
        for (ClientHandler client : clients) {
            User u = client.getLoggedInUser();
            if (u != null && targetUserId.equals(u.getId())) {
                String message = Protocol.BROADCAST_FORCE_LOGOUT + Protocol.DELIMITER + "Tài khoản của bạn đã bị cấm bởi hệ thống!";
                client.sendData(message);
                LOGGER.info("Force logout: " + u.getUsername());
                break;
            }
        }
    }
}