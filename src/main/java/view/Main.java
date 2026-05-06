package view;

import network.ClientNetworkManager;
import view.AuthenticationUI.LoginView.Login;

public class Main {
    public static void main(String[] args) {

        // --- PHẦN THÊM MỚI: KẾT NỐI MẠNG ---
        System.out.println("Đang kết nối đến Server...");
        boolean isConnected = ClientNetworkManager.getInstance().connect("localhost", 8080);

        if (!isConnected) {
            System.out.println("❌ LỖI: Không tìm thấy Server. Vui lòng chạy AuctionServer.java trước rồi khởi động lại App!");
            return; // Dừng khởi chạy giao diện nếu không có mạng
        }
        // ------------------------------------

        // Run Login: (Khởi chạy màn hình đầu tiên)
        Login.main(args);

        // Run Register: (Đã đóng)
        // Register.main(args);

        // Run BaseMenu: (Đã đóng vì phải Login thành công mới được vào)
        // BaseMenu.main(args);

        // Run AssertsListView: (Đã đóng)
        // AssertsListView.main(args);
    }
}