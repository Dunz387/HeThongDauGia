package view.utility.auth;

import javafx.application.Platform;
import javafx.stage.Stage;
import network.ClientNetworkManager;
import network.NotificationManager;
import network.SessionManager;
import shared.Protocol;
import view.utility.display.AlertHelper;
import view.utility.navigation.SceneManager;
import view.utility.validation.ValidationHelper;

import java.util.logging.Logger;

public final class LoginSupport {
    private static final Logger LOGGER = Logger.getLogger(LoginSupport.class.getName());

    private LoginSupport() {
    }

    public static void login(String username, String password, Stage stage) {
        if (!validate(username, password)) {
            return;
        }

        String request = Protocol.REQ_LOGIN + Protocol.DELIMITER + username + Protocol.DELIMITER + password;
        registerLoginResponse(stage);
        sendLoginRequest(request);
    }

    private static boolean validate(String username, String password) {
        if (ValidationHelper.isEmpty(username) || ValidationHelper.isEmpty(password)) {
            AlertHelper.showWarning("Lỗi", "Vui lòng nhập đủ tài khoản và mật khẩu!");
            return false;
        }
        return true;
    }

    private static void registerLoginResponse(Stage stage) {
        ClientNetworkManager.getInstance().clearListeners(Protocol.REQ_LOGIN);
        ClientNetworkManager.getInstance().registerListener(Protocol.REQ_LOGIN, response -> {
            String[] parts = response.split(Protocol.DELIMITER);
            Platform.runLater(() -> handleLoginResponse(parts, stage));
        });
    }

    private static void handleLoginResponse(String[] parts, Stage stage) {
        if (parts.length > 1 && parts[1].equals(Protocol.RES_SUCCESS)) {
            String role = parts.length >= 3 ? parts[2] : "";
            String userId = parts.length >= 4 ? parts[3] : "";
            String userName = parts.length >= 5 ? parts[4] : "";
            double balance = parts.length >= 6 ? Double.parseDouble(parts[5]) : 0.0;

            SessionManager.getInstance().setSession(userId, userName, role, balance);
            NotificationManager.getInstance().loadFromDatabase(userId);
            routeAfterLogin(stage, role);
            return;
        }

        AlertHelper.showWarning("Thất bại", parts.length >= 3 ? parts[2] : "Sai thông tin đăng nhập!");
    }

    private static void routeAfterLogin(Stage stage, String role) {
        if ("ADMIN".equals(role)) {
            SceneManager.goToAdminDashboard(stage);
        } else {
            SceneManager.switchScene(stage, "/view/menu/BaseMenu.fxml", "Trang Chủ");
        }
    }

    private static void sendLoginRequest(String request) {
        new Thread(() -> {
            try {
                if (!ClientNetworkManager.getInstance().isConnected()) {
                    LOGGER.info("Đang thử kết nối lại tới Server...");
                    if (!ClientNetworkManager.getInstance().connect("localhost", 8080)) {
                        Platform.runLater(() -> AlertHelper.showError("Lỗi",
                                "Không thể kết nối tới Server. Hãy đảm bảo Server đang chạy!"));
                        return;
                    }
                }

                if (!ClientNetworkManager.getInstance().sendData(request)) {
                    Platform.runLater(() -> AlertHelper.showError("Lỗi",
                            "Không thể gửi yêu cầu đăng nhập. Thử lại sau!"));
                }
            } catch (Exception e) {
                Platform.runLater(() -> AlertHelper.showError("Lỗi hệ thống",
                        "Đã xảy ra lỗi khi đăng nhập: " + e.getMessage()));
            }
        }).start();
    }
}
