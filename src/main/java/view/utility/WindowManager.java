package view.utility;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.util.logging.Level;
import java.util.logging.Logger;
/**
 * Quản lý việc mở cửa sổ mới (Popup) mà không ảnh hưởng đến cửa sổ chính
 * Sử dụng cho các cửa sổ phụ như: Lựa chọn hành động, Thông tin cá nhân, Tạo phiên đấu giá, v.v
 */
public class WindowManager {
    private static final Logger LOGGER = Logger.getLogger(WindowManager.class.getName());

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
            LOGGER.log(Level.SEVERE, "Lỗi khi mở cửa sổ: " + fxmlPath, e);
        }
    }

    public static void openWindow(String fxmlPath, String title) {
        openWindow(fxmlPath, title, null);
    }



    public static void openUserProfileWindow() {
        openWindow("/view/menu/UserInforView.fxml", "Thông tin cá nhân");
    }

    public static void openCreateItemWindow(Stage owner) {
        openWindow("/view/seller/CreateItem.fxml", "Tạo phiên đấu giá mới", owner);
    }

    public static void openCreateItemWindow() {
        openWindow("/view/seller/CreateItem.fxml", "Tạo phiên đấu giá mới", null);
    }

    public static void openSellerAuctionListWindow(Stage owner) {
        openWindow("/view/seller/SellerAuctionList.fxml", "Phiên đấu giá của bạn", owner);
    }

    // THÊM MỚI: Mở phòng đấu giá (dưới dạng popup)
    public static void openInRoomWindow(model.auction.Auction auction, Stage owner) {
        try {
            FXMLLoader loader = new FXMLLoader(WindowManager.class.getResource("/view/auction/InRoomView.fxml"));
            Parent root = loader.load();
            view.controller.auction.InRoomController controller = loader.getController();
            controller.setAuction(auction);
            
            Stage stage = new Stage();
            if (owner != null) {
                stage.initOwner(owner);
            }
            stage.setScene(new Scene(root));
            stage.setTitle("Phòng Đấu Giá: " + auction.getItem().getName());
            // Đóng cửa sổ: chỉ dọn dẹp, JavaFX tự đóng window
            stage.setOnCloseRequest(e -> controller.cleanupRoom());
            stage.show();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Lỗi khi mở phòng đấu giá", e);
        }
    }

    // THÊM MỚI: Mở phòng đấu giá dưới góc độ người bán/Admin (dưới dạng popup)
    public static void openSellerInRoomWindow(model.auction.Auction auction, Stage owner) {
        try {
            FXMLLoader loader = new FXMLLoader(WindowManager.class.getResource("/view/auction/SellerInRoomView.fxml"));
            Parent root = loader.load();
            view.controller.auction.SellerInRoomController controller = loader.getController();
            controller.setAuction(auction);
            
            Stage stage = new Stage();
            if (owner != null) {
                stage.initOwner(owner);
            }
            stage.setScene(new Scene(root));
            stage.setTitle("Quản Lý Phòng Đấu Giá: " + auction.getItem().getName());
            // Đóng cửa sổ: chỉ dọn dẹp, JavaFX tự đóng window
            stage.setOnCloseRequest(e -> controller.cleanupRoom());
            stage.show();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Lỗi khi mở quản lý phòng đấu giá", e);
        }
    }
}