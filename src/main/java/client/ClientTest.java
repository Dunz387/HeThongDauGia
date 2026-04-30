package client;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;

public class ClientTest {
    public static void main(String[] args) {
        System.out.println("Đang kết nối đến Server...");

        // Cố gắng gõ cửa Server ở địa chỉ localhost (máy mình) và cổng 8080
        try (Socket socket = new Socket("localhost", 8080);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             Scanner scanner = new Scanner(System.in)) {

            System.out.println("✅ Kết nối thành công! Hãy gõ tin nhắn (Gõ QUIT để thoát):");

            // Tạo một luồng ngầm để liên tục nghe Server nói gì
            new Thread(() -> {
                try {
                    String serverResponse;
                    while ((serverResponse = in.readLine()) != null) {
                        System.out.println(">> Server báo: " + serverResponse);
                    }
                } catch (Exception e) {
                    System.out.println("Đã ngắt kết nối khỏi Server.");
                }
            }).start();

            // Luồng chính để người dùng gõ phím gửi lên Server
            while (true) {
                String myMessage = scanner.nextLine();
                out.println(myMessage);
                if (myMessage.equalsIgnoreCase("QUIT")) {
                    break;
                }
            }

        } catch (Exception e) {
            System.err.println("❌ Không tìm thấy Server. Bạn đã chạy ServerMain chưa?");
        }
    }
}