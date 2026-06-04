package view.utility.menu;

import javafx.scene.control.Label;
import javafx.stage.Stage;
import network.client.ClientNetworkManager;
import network.session.SessionManager;
import view.utility.auction.AuctionNetworkHelper;
import view.utility.display.AlertHelper;
import view.utility.navigation.SceneManager;
import view.utility.navigation.WindowManager;

public final class MenuNavigationSupport {
    private MenuNavigationSupport() {
    }

    public static void openRoomMenu(Label anchor) {
        SceneManager.goToRoomMenu(stage(anchor));
    }

    public static void openCreateAuction(Label anchor) {
        if (!SessionManager.getInstance().isSeller()) {
            AlertHelper.showWarning("Quyền truy cập", "Chỉ người bán (Seller) mới có thể tạo phiên đấu giá!");
            return;
        }
        WindowManager.openCreateItemWindow(stage(anchor));
    }

    public static void logoutToLogin(Label anchor) {
        ClientNetworkManager.getInstance().logout();
        SceneManager.switchScene(stage(anchor), "/view/auth/Login.fxml", "Login");
    }

    public static void goToRegister(Label anchor) {
        SceneManager.switchScene(stage(anchor), "/view/auth/Register.fxml", "Register");
    }

    public static void goToAssetsList(Label anchor) {
        SceneManager.switchScene(stage(anchor), "/view/menu/AssetsList.fxml", "Danh Sách Tài Sản");
    }

    public static void goToBaseMenu(Label anchor) {
        SceneManager.switchScene(stage(anchor), "/view/menu/BaseMenu.fxml", "Base Menu");
    }

    public static void refreshAssetsList() {
        AuctionNetworkHelper.requestAuctionList();
    }

    private static Stage stage(Label anchor) {
        return (Stage) anchor.getScene().getWindow();
    }
}
