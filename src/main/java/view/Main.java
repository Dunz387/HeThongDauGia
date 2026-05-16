package view;

import network.ClientNetworkManager;
import view.AuthenticationUI.LoginView.Login;
import java.util.logging.Logger;
// import view.AuthenticationUI.RegisterView.Register;
// import view.BaseMenuUI.BaseMenu;
// import view.BaseMenuUI.AssetsListView;

public class Main {
    private static final Logger LOGGER = Logger.getLogger(Main.class.getName());
    public static void main(String[] args) {
        // --- PHẦN THÊM MỚI: XỬ LÝ NGOẠI LỆ TOÀN CỤC ---
        view.utility.GlobalExceptionHandler.setupHandler();

        // --- PHẦN THÊM MỚI: KẾT NỐI MẠNG ---
        LOGGER.info("Đang kết nối đến Server...");
        boolean isConnected = ClientNetworkManager.getInstance().connect("localhost", 8080);

        if (!isConnected) {
            LOGGER.severe("❌ LỖI: Không tìm thấy Server. Vui lòng chạy AuctionServer.java trước rồi khởi động lại App!");
            return; // Dừng khởi chạy giao diện nếu không có mạng
        }
        // ------------------------------------

        // Run Login: (Khởi chạy màn hình đầu tiên)
        Login.main(args);

        // Run Register: (Đã đóng)
        // Register.main(args);

        // Run BaseMenu: (Đã đóng vì phải Login thành công mới được vào)
        // BaseMenu.main(args);

        // Run AssetsListView: (Đã đóng)
        // AssetsListView.main(args);
    }
}