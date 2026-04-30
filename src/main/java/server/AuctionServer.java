package server;

import model.auction.Auction;
import model.auction.AuctionObserver;
import model.auction.AuctionStatus;
import model.item.Item;
import model.user.Bidder;
import model.user.Seller;
import service.AuctionManager;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class AuctionServer implements AuctionObserver {
    private static final int PORT = 8080;
    private Auction currentAuction;
    private List<ClientHandler> clients = new ArrayList<>();
    private AuctionManager manager; // Bổ sung sếp tổng

    public static void main(String[] args) {
        AuctionServer server = new AuctionServer();
        server.startServer();
    }

    public void startServer() {
        // Đánh thức Database
        manager = AuctionManager.getInstance();

        Scanner scanner = new Scanner(System.in);
        System.out.println("=== HỆ THỐNG ĐẤU GIÁ ĐA NĂNG ===");

        // Các bước nhập liệu của bạn được giữ nguyên
        System.out.println("Các loại hàng hỗ trợ: [ELECTRONICS, ART, VEHICLE]");
        System.out.print("Chọn loại hàng: ");
        String type = scanner.nextLine().toUpperCase();

        System.out.print("Tên sản phẩm: ");
        String name = scanner.nextLine();
        System.out.print("Mô tả: ");
        String desc = scanner.nextLine();
        System.out.print("Giá khởi điểm ($): ");
        double price = Double.parseDouble(scanner.nextLine());

        String extra1 = "Default";
        int extra2 = 0;
        // Bỏ qua Switch-case hiển thị ở đây cho ngắn gọn, bạn giữ nguyên code cũ phần nhập extra nhé!

        Seller admin = new Seller("S01", "HeThong", "123", 0.0);
        manager.registerUser(admin); // Lưu Seller vào DB

        Item item = Item.createItem(type, "ID-" + System.currentTimeMillis(), name, desc, admin, extra1, extra2);

        currentAuction = new Auction("AUC-" + System.currentTimeMillis(), item, price, 50.0, LocalDateTime.now().plusMinutes(2));
        currentAuction.setStatus(AuctionStatus.RUNNING);
        currentAuction.addObserver(this);

        // ĐĂNG KÝ PHIÊN ĐẤU GIÁ VÀO DATABASE
        manager.registerAuction(currentAuction);

        startCountdownTimer(2);

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("\n[SERVER] Đã mở cổng " + PORT + ". Đang chờ người chơi vào...");

            while (true) {
                Socket socket = serverSocket.accept();
                System.out.println("Có người chơi mới kết nối: " + socket.getInetAddress());

                // Chuyển việc chăm sóc Client cho luồng riêng, truyền thêm manager vào
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

                // Gọi manager để chốt sổ (lưu Database, trừ tiền)
                manager.concludeAuction(currentAuction);

                broadcast("\n [HỆ THỐNG] HẾT GIỜ! PHIÊN ĐẤU GIÁ KẾT THÚC.");
                Bidder winner = currentAuction.getHighestBidder();
                if (winner != null) {
                    broadcast(" NGƯỜI THẮNG: " + winner.getUsername() + " với mức giá $" + currentAuction.getCurrentPrice());
                } else {
                    broadcast(" Không có ai đặt giá. Đấu giá thất bại.");
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });
        timerThread.start();
    }

    @Override
    public void update(Auction auction, double newPrice, String topBidderName) {
        broadcast("\n[THÔNG BÁO TỪ BÀN ĐẤU QUỐC TẾ] " + topBidderName + " vừa nâng giá lên: $" + newPrice);
    }

    public synchronized void broadcast(String message) {
        for (ClientHandler client : clients) {
            client.sendData(message);
        }
    }

    public synchronized void removeClient(ClientHandler client) {
        clients.remove(client);
    }
}