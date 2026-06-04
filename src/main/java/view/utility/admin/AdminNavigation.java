package view.utility.admin;

import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import network.client.ClientNetworkManager;
import view.utility.navigation.SceneManager;

public final class AdminNavigation {
    private AdminNavigation() {
    }

    public static void goToDashboard(HBox menuBar) {
        SceneManager.goToAdminDashboard(stageOf(menuBar));
    }

    public static void goToUserManagement(HBox menuBar) {
        SceneManager.goToAdminUserManagement(stageOf(menuBar));
    }

    public static void goToAuctionManagement(HBox menuBar) {
        SceneManager.goToAdminAuctionManagement(stageOf(menuBar));
    }

    public static void logout(HBox menuBar) {
        ClientNetworkManager.getInstance().logout();
        SceneManager.goToLogin(stageOf(menuBar));
    }

    private static Stage stageOf(HBox menuBar) {
        return (Stage) menuBar.getScene().getWindow();
    }
}
