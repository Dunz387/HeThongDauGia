package view.utility.auth;

import javafx.application.Platform;
import javafx.stage.Stage;
import javafx.stage.Window;
import network.ClientNetworkManager;
import network.SessionManager;
import shared.Protocol;
import view.utility.display.AlertHelper;
import view.utility.navigation.SceneManager;

import java.util.ArrayList;
import java.util.List;

public final class ForceLogoutSupport {
    private ForceLogoutSupport() {
    }

    public static void registerForceLogoutListener() {
        ClientNetworkManager.getInstance().clearListeners(Protocol.BROADCAST_FORCE_LOGOUT);
        ClientNetworkManager.getInstance().registerListener(Protocol.BROADCAST_FORCE_LOGOUT, message -> {
            String[] parts = message.split(Protocol.DELIMITER);
            Platform.runLater(() -> handleForceLogout(parts));
        });
    }

    private static void handleForceLogout(String[] parts) {
        SessionManager.getInstance().clearSession();
        Stage mainWindow = closeOwnedWindowsAndFindMain();
        if (mainWindow != null) {
            SceneManager.goToLogin(mainWindow);
            AlertHelper.showError("Thông báo",
                    parts.length >= 2 ? parts[1] : "Tài khoản của bạn đã bị đăng xuất!");
        }
    }

    private static Stage closeOwnedWindowsAndFindMain() {
        Stage mainWindow = null;
        List<Window> openWindows = new ArrayList<>(Window.getWindows());
        for (Window window : openWindows) {
            if (window instanceof Stage stage) {
                if (stage.getOwner() != null) {
                    stage.close();
                } else if (mainWindow == null && stage.isShowing()) {
                    mainWindow = stage;
                }
            }
        }
        return mainWindow;
    }
}
