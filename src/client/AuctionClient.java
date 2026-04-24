package client;

import model.auction.Auction;
import model.item.Item;
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
            System.out.println("Đã kết nối thành công tới máy chủ đấu giá!");

            ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
            ObjectInputStream in = new ObjectInputStream(socket.getInputStream());

            Scanner scanner = new Scanner(System.in);
            System.out.print("Nhập tên của bạn để tham gia đấu giá: ");
            String displayName = scanner.nextLine();

            // Gửi tên này lên Server ngay lập tức
            out.writeObject(displayName);
            out.flush();
            // LUỒNG 1: CHẠY NGẦM ĐỂ NHẬN DỮ LIỆU (Bao gồm Item tùy chọn)
            Thread listenerThread = new Thread(() -> {
                try {
                    while (true) {
                        Object data = in.readObject();

                        if (data instanceof Auction) {
                            Auction auc = (Auction) data;
                            System.out.println(" THÔNG TIN PHIÊN ĐẤU GIÁ: " + auc.getId());

                            // Client khui hộp và tự động lấy thông tin món đồ Server vừa tạo
                            Item item = auc.getItem();
                            if (item != null) {
                                System.out.println(" Sản phẩm: " + item.getName());
                                System.out.println(" Mô tả: " + item.getDescription());
                                System.out.println(" Chi tiết: " + item.getDetails());
                                System.out.println(" Người bán: " + item.getOwner().getUsername());
                            }
                            System.out.println(" Trạng thái: " + auc.getStatus());
                            System.out.println(" Giá hiện tại: $" + auc.getCurrentPrice());
                            if (auc.getHighestBidder() != null) {
                                System.out.println(" Người dẫn đầu: " + auc.getHighestBidder().getUsername());
                            } else {
                                System.out.println(" Người dẫn đầu: Chưa có ai");
                            }

                            System.out.print("Nhập giá tiền bạn muốn đặt (VD: 36) hoặc 'exit': ");

                        } else if (data instanceof String) {
                            System.out.println((String) data);
                            System.out.print("Nhập giá tiền bạn muốn đặt (VD: 36) hoặc 'exit': ");
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
                if (input.equalsIgnoreCase("exit")) {
                    break;
                }
                // Gửi số lên cho Server xử lý
                out.writeObject(input);
                out.flush();
            }

            socket.close();
        } catch (Exception e) {
            System.err.println("Không thể kết nối. Vui lòng bật Server trước!");
        }
    }
}