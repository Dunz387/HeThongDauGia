package view;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import network.ClientNetworkManager;

import java.util.logging.Logger;

/**
 * Entry point chính của ứng dụng.
 * Kết hợp khởi tạo mạng, xử lý ngoại lệ, và khởi chạy giao diện Login.
 */
public class Main extends Application {
    private static final Logger LOGGER = Logger.getLogger(Main.class.getName());

    @Override
    public void start(Stage primaryStage) throws Exception {
        Parent root = FXMLLoader.load(java.util.Objects.requireNonNull(getClass().getResource("/view/auth/Login.fxml")));
        Scene scene = new Scene(root, 950, 560);

        primaryStage.setTitle("Login");
        primaryStage.setMinWidth(700);
        primaryStage.setMinHeight(450);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        // Xử lý ngoại lệ toàn cục
        shared.GlobalExceptionHandler.setupHandler();

        // Kết nối mạng
        LOGGER.info("Đang kết nối đến Server...");
        boolean isConnected = ClientNetworkManager.getInstance().connect("localhost", 8080);

        if (!isConnected) {
            LOGGER.severe(
                    "❌ LỖI: Không tìm thấy Server. Vui lòng chạy AuctionServer.java trước rồi khởi động lại App!");
            return;
        }

        // Khởi chạy giao diện Login
        launch(args);
    }
}