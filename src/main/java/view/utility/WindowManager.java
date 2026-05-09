package view.utility;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * Lớp utility để quản lý việc mở các cửa sổ dialog/popup
 * Tránh lặp code khi mở cửa sổ
 */
public class WindowManager {

    /**
     * Mở cửa sổ dialog từ file FXML
     * @param fxmlPath đường dẫn file FXML (VD: "/view/AuctionRoomUI/BidOrSellChoice.fxml")
     * @param title tiêu đề cửa sổ
     */
    public static void openWindow(String fxmlPath, String title) {
        try {
            FXMLLoader loader = new FXMLLoader(WindowManager.class.getResource(fxmlPath));
            Parent root = loader.load();
            Stage stage = new Stage();
            stage.setScene(new Scene(root));
            stage.setTitle(title);
            stage.show();
        } catch (Exception e) {
            System.err.println("Lỗi khi mở cửa sổ: " + fxmlPath);
            e.printStackTrace();
        }
    }

    /**
     * Mở cửa sổ dialog BidOrSellChoice (chọn đặt giá hoặc đăng bán)
     */
    public static void openBidOrSellChoiceWindow() {
        openWindow("/view/AuctionRoomUI/BidOrSellChoice.fxml", "Lựa chọn hành động");
    }

    /**
     * Mở cửa sổ thông tin cá nhân
     */
    public static void openUserProfileWindow() {
        openWindow("/view/BaseMenuUI/UserInforView.fxml", "Thông tin cá nhân");
    }

    /**
     * Mở cửa sổ tạo item (Đăng bán sản phẩm)
     */
    public static void openCreateItemWindow() {
        openWindow("/view/SellerUI/CreateItem.fxml", "Tạo phiên đấu giá mới");
    }
}
