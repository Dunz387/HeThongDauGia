package server;

import model.auction.Auction;
import model.auction.AuctionObserver;
import model.auction.AuctionStatus;
import model.item.Item;
import model.user.Bidder;
import model.user.Seller;

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

    public static void main(String[] args) {
        AuctionServer server = new AuctionServer();
        server.startServer();
    }

    public void startServer() {
        Scanner scanner = new Scanner(System.in);
        System.out.println("=== HỆ THỐNG ĐẤU GIÁ ĐA NĂNG ===");

        // 1. CHỌN THỂ LOẠI
        System.out.println("Các loại hàng hỗ trợ: [ELECTRONICS, ART, VEHICLE]");
        System.out.print("Chọn loại hàng: ");
        String type = scanner.nextLine().toUpperCase();

        // 2. NHẬP THÔNG TIN CHUNG
        System.out.print("Tên sản phẩm: ");
        String name = scanner.nextLine();
        System.out.print("Mô tả: ");
        String desc = scanner.nextLine();
        System.out.print("Giá khởi điểm ($): ");
        double price = Double.parseDouble(scanner.nextLine());

        // 3. NHẬP THÔNG TIN RIÊNG
        String extra1 = "";
        int extra2 = 0;

        switch (type) {
            case "ELECTRONICS":
                System.out.print("Nhập hãng : ");
                extra1 = scanner.nextLine();
                System.out.print("Số tháng bảo hành: ");
                extra2 = Integer.parseInt(scanner.nextLine());
                break;
            case "ART":
                System.out.print("Tên họa sĩ : ");
                extra1 = scanner.nextLine();
                System.out.print("Năm sáng tác: ");
                extra2 = Integer.parseInt(scanner.nextLine());
                break;
            case "VEHICLE":
                System.out.print("Loại động cơ: ");
                extra1 = scanner.nextLine();
                System.out.print("Số KM đã đi : ");
                extra2 = Integer.parseInt(scanner.nextLine());
                break;
            default:
                System.out.println("Loại không hợp lệ, mặc định chọn ELECTRONICS.");
                type = "ELECTRONICS";
        }

        // 4. KHỞI TẠO ĐỐI TƯỢNG
        Seller admin = new Seller("S01", "HeThong", "123", 0.0);
        Item item = Item.createItem(type, "ID-" + System.currentTimeMillis(), name, desc, admin, extra1, extra2);

        currentAuction = new Auction("AUC-" + System.currentTimeMillis(), item, price, 50.0, LocalDateTime.now().plusMinutes(2));
        currentAuction.setStatus(AuctionStatus.RUNNING);
        currentAuction.addObserver(this);

        // 5. KÍCH HOẠT ĐỒNG HỒ ĐẾM NGƯỢC (Đã sửa logic đếm ngược)
        startCountdownTimer(2);

        // 6. [BỔ SUNG QUAN TRỌNG]: MỞ CỔNG MẠNG VÀ ĐÓN CLIENT
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("\n[SERVER] Đã mở cổng " + PORT + ". Đang chờ người chơi vào...");

            while (true) {
                // Lệnh này sẽ dừng lại để đợi Client (blocking),
                // nhưng đồng hồ đếm ngược vẫn chạy vì nó nằm ở Thread riêng.
                Socket socket = serverSocket.accept();
                System.out.println("Có người chơi mới kết nối: " + socket.getInetAddress());

                // Chuyển việc chăm sóc Client cho luồng riêng
                ClientHandler handler = new ClientHandler(socket, this, currentAuction);
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
                    // Chỉ thông báo mỗi 30s hoặc 5 giây cuối
                    if (timeRemaining % 30 == 0 || timeRemaining <= 5) {
                        broadcast("\n⏱️ [THỜI GIAN] Còn lại: " + (timeRemaining / 60) + "p " + (timeRemaining % 60) + "s");
                    }
                    Thread.sleep(1000);
                    timeRemaining--;
                }

                currentAuction.setStatus(AuctionStatus.FINISHED);
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
        broadcast(auction);
        broadcast("\n[THÔNG BÁO] " + topBidderName + " vừa đặt giá mới: $" + newPrice);
    }

    public synchronized void broadcast(Object data) {
        for (ClientHandler client : clients) {
            client.sendData(data);
        }
    }

    public synchronized void removeClient(ClientHandler client) {
        clients.remove(client);
    }
}