package view;

import network.ClientNetworkManager; // Thêm import trạm thu phát mạng
import view.AuthenticationUI.LoginView.Login; // Đã bỏ comment
// import view.AuthenticationUI.RegisterView.Register;
import view.BaseMenuUI.BaseMenu; // Giữ nguyên import phòng hờ

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

        // Run Login: (Đã mở để chạy đầu tiên)
        Login.main(args);

        // Run Register:
        // Register.main(args);

        // Run BaseMenu: (Đã đóng lại vì phải Login thành công mới được vào)
        // BaseMenu.main(args);

    }
}