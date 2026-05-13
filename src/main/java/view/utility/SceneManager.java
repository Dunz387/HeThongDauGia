package view.utility;

import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

/**
 * Quản lý việc chuyển đổi Scene (thay đổi giao diện nhưng giữ nguyên cửa sổ)
 * Sử dụng cho việc chuyển màn hình chính (Login -> Register -> BaseMenu, v.v)
 */
public class SceneManager {

    /**
     * Chuyển Scene dựa trên Stage
     * @param stage    Cửa sổ hiện tại
     * @param fxmlPath Đường dẫn tuyệt đối đến file FXML mới
     * @param title    Tiêu đề mới cho cửa sổ
     */
    public static void switchScene(Stage stage, String fxmlPath, String title) {
        if (stage == null) {
            System.err.println("⚠️ Cảnh báo: Stage bị null khi cố gắng chuyển sang " + fxmlPath);
            return;
        }
        try {
            FXMLLoader loader = new FXMLLoader(SceneManager.class.getResource(fxmlPath));
            Parent root = loader.load();

            // Giữ nguyên kích thước cửa sổ hiện tại khi chuyển scene
            double width = stage.getScene() != null ? stage.getScene().getWidth() : 1050;
            double height = stage.getScene() != null ? stage.getScene().getHeight() : 680;
            Scene scene = new Scene(root, width, height);
            stage.setScene(scene);

            if (title != null && !title.trim().isEmpty()) {
                stage.setTitle(title);
            }

            stage.show();

        } catch (IOException e) {
            System.err.println("❌ Không thể tải màn hình từ đường dẫn: " + fxmlPath);
            e.printStackTrace();
        }
    }

    /**
     * Chuyển Scene và trả về Controller để caller có thể truyền dữ liệu vào.
     * @param stage    Cửa sổ hiện tại
     * @param fxmlPath Đường dẫn tuyệt đối đến file FXML mới
     * @param title    Tiêu đề mới cho cửa sổ
     * @return Controller của Scene vừa load, hoặc null nếu lỗi
     */
    public static <T> T switchSceneAndGetController(Stage stage, String fxmlPath, String title) {
        if (stage == null) {
            System.err.println("⚠️ Cảnh báo: Stage bị null khi cố gắng chuyển sang " + fxmlPath);
            return null;
        }
        try {
            FXMLLoader loader = new FXMLLoader(SceneManager.class.getResource(fxmlPath));
            Parent root = loader.load();

            double width = stage.getScene() != null ? stage.getScene().getWidth() : 1050;
            double height = stage.getScene() != null ? stage.getScene().getHeight() : 680;
            Scene scene = new Scene(root, width, height);
            stage.setScene(scene);

            if (title != null && !title.trim().isEmpty()) {
                stage.setTitle(title);
            }

            stage.show();
            return loader.getController();

        } catch (IOException e) {
            System.err.println("❌ Không thể tải màn hình từ đường dẫn: " + fxmlPath);
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Chuyển Scene dựa trên ActionEvent
     * @param event    Sự kiện click để lấy ra cửa sổ hiện tại
     * @param fxmlPath Đường dẫn tuyệt đối đến file FXML mới
     * @param title    Tiêu đề mới cho cửa sổ
     */
    public static void switchScene(ActionEvent event, String fxmlPath, String title) {
        // Lấy Stage từ sự kiện của Nút bấm
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        
        // Gọi lại phương thức trên để thực hiện việc chuyển cảnh
        switchScene(stage, fxmlPath, title);
    }

    // ===== PHƯƠNG THỨC TIỆN LỢI CHO CÁC MÀNG HÌNH CHÍNH =====
    
    /**
     * Chuyển tới màn hình Login
     */
    public static void goToLogin(Stage stage) {
        switchScene(stage, "/view/AuthenticationUI/LoginView/Login.fxml", "Login");
    }

    /**
     * Chuyển tới màn hình Register
     */
    public static void goToRegister(Stage stage) {
        switchScene(stage, "/view/AuthenticationUI/RegisterView/Register.fxml", "Register");
    }

    /**
     * Chuyển tới màn hình Base Menu (Trang chủ)
     */
    public static void goToBaseMenu(Stage stage) {
        switchScene(stage, "/view/BaseMenuUI/BaseMenu.fxml", "Trang Chủ");
    }

    /**
     * Chuyển tới danh sách tài sản
     */
    public static void goToAssetsList(Stage stage) {
        switchScene(stage, "/view/BaseMenuUI/AssetsList.fxml", "Danh Sách Tài Sản");
    }

    /**
     * Chuyển tới danh sách phòng đấu giá
     */
    public static void goToRoomMenu(Stage stage) {
        switchScene(stage, "/view/AuctionRoomUI/RoomMenuChoice.fxml", "Danh Sách Phòng Đấu Giá");
    }

    /**
     * Chuyển tới phòng đấu giá (InRoomView) - KHÔNG truyền auctionId (legacy)
     */
    public static void goToInRoom(Stage stage) {
        switchScene(stage, "/view/AuctionRoomUI/InRoomView.fxml", "Phòng Đấu Giá");
    }

    /**
     * Chuyển tới phòng đấu giá (InRoomView) VÀ truyền auctionId vào Controller.
     * @param stage     Cửa sổ hiện tại
     * @param auctionId ID phiên đấu giá cần tham gia
     */
    public static void goToInRoom(Stage stage, String auctionId) {
        view.AuctionRoomUI.InRoomController controller =
                switchSceneAndGetController(stage, "/view/AuctionRoomUI/InRoomView.fxml", "Phòng Đấu Giá");
        if (controller != null) {
            controller.setAuctionId(auctionId);
        }
    }

    public static void goToCreateItem(Stage stage) {
        switchScene(stage, "/view/SellerUI/CreateItem.fxml", "Đăng bán sản phẩm");
    }

    /**
     * Chuyển tới phòng đấu giá dành cho Seller (chỉ theo dõi) - KHÔNG truyền auctionId (legacy)
     */
    public static void goToSellerInRoom(Stage stage) {
        switchScene(stage, "/view/AuctionRoomUI/SellerInRoomView.fxml", "Phòng Đấu Giá - Người Bán");
    }

    /**
     * Chuyển tới phòng đấu giá dành cho Seller VÀ truyền auctionId vào Controller.
     * @param stage     Cửa sổ hiện tại
     * @param auctionId ID phiên đấu giá cần theo dõi
     */
    public static void goToSellerInRoom(Stage stage, String auctionId) {
        view.AuctionRoomUI.SellerInRoomController controller =
                switchSceneAndGetController(stage, "/view/AuctionRoomUI/SellerInRoomView.fxml", "Phòng Đấu Giá - Người Bán");
        if (controller != null) {
            controller.setAuctionId(auctionId);
        }
    }

    // ===== MÀN HÌNH QUẢN TRỊ (ADMIN) =====

    /**
     * Chuyển tới Admin Dashboard
     */
    public static void goToAdminDashboard(Stage stage) {
        switchScene(stage, "/view/AdminUI/AdminDashboard.fxml", "Admin - Tổng Quan");
    }

    /**
     * Chuyển tới màn hình Quản lý Người Dùng (Admin)
     */
    public static void goToAdminUserManagement(Stage stage) {
        switchScene(stage, "/view/AdminUI/AdminUserManagement.fxml", "Admin - Quản Lý Người Dùng");
    }

    /**
     * Chuyển tới màn hình Quản lý Phiên Đấu Giá (Admin)
     */
    public static void goToAdminAuctionManagement(Stage stage) {
        switchScene(stage, "/view/AdminUI/AdminAuctionManagement.fxml", "Admin - Quản Lý Đấu Giá");
    }
}

