package client;

import model.auction.Auction;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Scanner;

public class AuctionClient {
    private static final String SERVER_IP = "127.0.0.1";
    private static final int PORT = 8080;

    public static void main(String[] args) {
        try {
            Socket socket = new Socket(SERVER_IP, PORT);
            System.out.println("✅ Đã kết nối thành công tới máy chủ đấu giá!");

            // KHỞI TẠO LUỒNG OBJECT (Client tạo Out trước, flush ngay)
            ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
            out.flush();
            ObjectInputStream in = new ObjectInputStream(socket.getInputStream());

            Scanner scanner = new Scanner(System.in);
            System.out.print("Nhập tên của bạn để tham gia đấu giá: ");
            String displayName = scanner.nextLine();

            // Gửi tên dạng String Object
            out.writeObject(displayName);
            out.flush();

            // LUỒNG 1: CHẠY NGẦM ĐỂ NGHE SERVER NÓI
            Thread listenerThread = new Thread(() -> {
                try {
                    Object serverData;
                    while ((serverData = in.readObject()) != null) {

                        // Nếu là chuỗi thông báo bình thường
                        if (serverData instanceof String) {
                            System.out.println(serverData);
                        }
                        // NẾU LÀ OBJECT AUCTION, BÓC TÁCH RA ĐỂ HIỂN THỊ
                        else if (serverData instanceof Auction) {
                            Auction receivedAuction = (Auction) serverData;
                            System.out.println("\n[📦 NHẬN DỮ LIỆU TỪ SERVER]");
                            System.out.println("- Món hàng: " + receivedAuction.getItem().getName());
                            System.out.println("- Giá khởi điểm: $" + receivedAuction.getCurrentPrice()); // Ban đầu bằng starting price
                            System.out.println("- Loại: " + receivedAuction.getItem().getDetails());
                            System.out.println("--------------------------------\n");
                        }
                    }
                } catch (Exception e) {
                    System.out.println("\nĐã ngắt kết nối khỏi máy chủ.");
                    System.exit(0);
                }
            });
            listenerThread.start();

            // LUỒNG 2: ĐỌC BÀN PHÍM VÀ GỬI LÊN SERVER
            while (true) {
                String input = scanner.nextLine();
                out.writeObject(input);
                out.flush();

                if (input.equalsIgnoreCase("exit")) {
                    break;
                }
            }

            socket.close();
        } catch (Exception e) {
            System.err.println("❌ Không thể kết nối. Vui lòng bật Server trước!");
        }
    }
}