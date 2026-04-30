package client;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;

public class AuctionClient {
    private static final String SERVER_IP = "127.0.0.1";
    private static final int PORT = 8080;

    public static void main(String[] args) {
        try {
            Socket socket = new Socket(SERVER_IP, PORT);
            System.out.println("✅ Đã kết nối thành công tới máy chủ đấu giá!");

            // Dùng luồng Text
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            Scanner scanner = new Scanner(System.in);
            System.out.print("Nhập tên của bạn để tham gia đấu giá: ");
            String displayName = scanner.nextLine();

            // Gửi tên này lên Server
            out.println(displayName);

            // LUỒNG 1: CHẠY NGẦM ĐỂ NGHE SERVER NÓI
            Thread listenerThread = new Thread(() -> {
                try {
                    String serverMessage;
                    while ((serverMessage = in.readLine()) != null) {
                        System.out.println(serverMessage);
                    }
                } catch (Exception e) {
                    System.out.println("\nĐã ngắt kết nối khỏi máy chủ.");
                    System.exit(0);
                }
            });
            listenerThread.start();

            // LUỒNG 2: ĐỌC BÀN PHÍM VÀ GỬI GIÁ TIỀN LÊN SERVER
            while (true) {
                String input = scanner.nextLine();
                if (input.equalsIgnoreCase("exit")) {
                    out.println("exit");
                    break;
                }
                out.println(input);
            }

            socket.close();
        } catch (Exception e) {
            System.err.println("❌ Không thể kết nối. Vui lòng bật Server trước!");
        }
    }
}