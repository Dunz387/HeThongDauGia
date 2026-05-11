package view.utility;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
/**
 * Quản lý việc mở cửa sổ mới (Popup) mà không ảnh hưởng đến cửa sổ chính
 * Sử dụng cho các cửa sổ phụ như: Lựa chọn hành động, Thông tin cá nhân, Tạo phiên đấu giá, v.v
 */
public class WindowManager {

    // THÊM MỚI: Hàm mở cửa sổ có hỗ trợ truyền cửa sổ cha (owner)
    public static void openWindow(String fxmlPath, String title, Stage owner) {
        try {
            FXMLLoader loader = new FXMLLoader(WindowManager.class.getResource(fxmlPath));
            Parent root = loader.load();
            Stage stage = new Stage();
            
            // Thiết lập cửa sổ cha cho popup
            if (owner != null) {
                stage.initOwner(owner);
            }
            
            stage.setScene(new Scene(root));
            stage.setTitle(title);
            stage.show();
        } catch (Exception e) {
            System.err.println("Lỗi khi mở cửa sổ: " + fxmlPath);
            e.printStackTrace();
        }
    }

    public static void openWindow(String fxmlPath, String title) {
        openWindow(fxmlPath, title, null);
    }

    public static void openBidOrSellChoiceWindow(Stage owner) {
        openWindow("/view/AuctionRoomUI/BidOrSellChoice.fxml", "Lựa chọn hành động", owner);
    }

    public static void openUserProfileWindow() {
        openWindow("/view/BaseMenuUI/UserInforView.fxml", "Thông tin cá nhân");
    }

    public static void openCreateItemWindow(Stage owner) {
        openWindow("/view/SellerUI/CreateItem.fxml", "Tạo phiên đấu giá mới", owner);
    }

    public static void openCreateItemWindow() {
        openWindow("/view/SellerUI/CreateItem.fxml", "Tạo phiên đấu giá mới", null);
    }

    public static void openSellerAuctionListWindow(Stage owner) {
        openWindow("/view/SellerUI/SellerAuctionList.fxml", "Phiên đấu giá của bạn", owner);
    }
}