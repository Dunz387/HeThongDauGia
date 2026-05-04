package server;

import model.auction.Auction;
import model.auction.AuctionObserver;
import model.auction.AuctionStatus;
import model.item.Item;
import model.item.ItemFactory;
import model.user.Bidder;
import model.user.Seller;
import service.AuctionManager;

import java.io.IOException;
import java.net.InetSocketAddress; // Cần thêm import này cho bind
import java.net.ServerSocket;
import java.net.Socket;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.CopyOnWriteArrayList;

public class AuctionServer implements AuctionObserver {
    private static final int PORT = 8080;
    private Auction currentAuction;
    private List<ClientHandler> clients = new CopyOnWriteArrayList<>();
    private AuctionManager manager;

    public static void main(String[] args) {
        new AuctionServer().startServer();
    }

    public void startServer() {
        // TỐI ƯU 1: Shutdown Hook để dọn dẹp dữ liệu khi tắt
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\n[HỆ THỐNG] Đang đóng Server và giải phóng tài nguyên...");
            if (manager != null) manager.stopManager();
        }));

        manager = AuctionManager.getInstance();
        Scanner scanner = new Scanner(System.in);
        System.out.println("=== HỆ THỐNG ĐẤU GIÁ ĐA NĂNG ===");

        // --- Logic nhập liệu giữ nguyên ---
        System.out.print("Chọn loại hàng [ELECTRONICS, ART, VEHICLE]: ");
        String type = scanner.nextLine().toUpperCase();
        System.out.print("Tên sản phẩm: ");
        String name = scanner.nextLine();
        System.out.print("Mô tả: ");
        String desc = scanner.nextLine();
        System.out.print("Giá khởi điểm ($): ");
        double price = Double.parseDouble(scanner.nextLine());

        Seller admin = new Seller("S01", "HeThong", "123", 0.0);
        manager.registerUser(admin);

        Item item = ItemFactory.createItem(type, "ID-" + System.currentTimeMillis(), name, desc, admin, "Default", 0);
        currentAuction = new Auction("AUC-" + System.currentTimeMillis(), item, price, 50.0, LocalDateTime.now().plusMinutes(2));
        currentAuction.setStatus(AuctionStatus.RUNNING);
        currentAuction.addObserver(this);
        manager.registerAuction(currentAuction);

        // TỐI ƯU 2: Luồng đếm ngược là Daemon
        startCountdownTimer(2);

        // TỐI ƯU 3: NÚT TỰ HỦY NỘI BỘ - Gõ 'exit' để tắt Server sạch sẽ
        new Thread(() -> {
            Scanner cmdScanner = new Scanner(System.in);
            while (cmdScanner.hasNextLine()) {
                if ("exit".equalsIgnoreCase(cmdScanner.nextLine())) {
                    System.out.println("[HỆ THỐNG] Nhận lệnh EXIT. Đang tự hủy an toàn...");
                    System.exit(0); // Lệnh này sẽ kích hoạt Shutdown Hook ở trên
                }
            }
        }).start();

        // TỐI ƯU 4: SET REUSE ADDRESS - Chiếm lại cổng ngay lập tức dù tắt lỗi[cite: 2]
        try (ServerSocket serverSocket = new ServerSocket()) {
            serverSocket.setReuseAddress(true); // Cho phép dùng lại Port 8080 ngay lập tức[cite: 2]
            serverSocket.bind(new InetSocketAddress(PORT));

            System.out.println("\n[SERVER] Đã mở cổng " + PORT + ". Gõ 'exit' để kết thúc.");

            while (true) {
                Socket socket = serverSocket.accept();
                System.out.println("Có người chơi mới kết nối: " + socket.getInetAddress());
                ClientHandler handler = new ClientHandler(socket, this, currentAuction, manager);
                clients.add(handler);
                new Thread(handler).start();
            }
        } catch (IOException e) {
            System.err.println("Lỗi Server: " + e.getMessage());
        }
    }

    private void startCountdownTimer(int minutes) {
        Thread timerThread = new Thread(() -> {
            int timeRemaining = minutes * 60;
            try {
                while (timeRemaining > 0) {
                    if (timeRemaining % 30 == 0 || timeRemaining <= 5) {
                        broadcast("\n⏱️ [THỜI GIAN] Còn lại: " + (timeRemaining / 60) + "p " + (timeRemaining % 60) + "s");
                    }
                    Thread.sleep(1000);
                    timeRemaining--;
                }
                manager.concludeAuction(currentAuction);
                broadcast("\n [HỆ THỐNG] HẾT GIỜ! PHIÊN ĐẤU GIÁ KẾT THÚC.");

                Bidder winner = currentAuction.getHighestBidder();
                if (winner != null) {
                    broadcast(" NGƯỜI THẮNG: " + winner.getUsername() + " với mức giá $" + currentAuction.getCurrentPrice());
                } else {
                    broadcast(" Không có ai đặt giá. Đấu giá thất bại.");
                }
            } catch (InterruptedException e) { e.printStackTrace(); }
        });
        timerThread.setDaemon(true); // Đảm bảo luồng đếm ngược tự tắt
        timerThread.start();
    }

    @Override
    public void update(Auction auction, double newPrice, String topBidderName) {
        broadcast("\n[THÔNG BÁO] " + topBidderName + " vừa nâng giá lên: $" + newPrice);
    }

    public synchronized void broadcast(String message) {
        for (ClientHandler client : clients) client.sendData(message);
    }

    public synchronized void removeClient(ClientHandler client) {
        clients.remove(client);
    }
}