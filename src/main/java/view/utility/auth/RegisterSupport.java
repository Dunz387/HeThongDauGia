package view.utility.auth;

import javafx.application.Platform;
import javafx.stage.Stage;
import network.ClientNetworkManager;
import shared.Protocol;
import view.utility.display.AlertHelper;
import view.utility.navigation.SceneManager;
import view.utility.validation.ValidationHelper;

public final class RegisterSupport {
    private RegisterSupport() {
    }

    public static void register(String username, String password, String role, Stage stage) {
        if (!validate(username, password)) {
            return;
        }

        String request = Protocol.REQ_REGISTER + Protocol.DELIMITER + username
                + Protocol.DELIMITER + password + Protocol.DELIMITER + role;
        registerResponse(stage);
        ClientNetworkManager.getInstance().sendData(request);
    }

    private static boolean validate(String username, String password) {
        if (ValidationHelper.isEmpty(username) || ValidationHelper.isEmpty(password)) {
            AlertHelper.showWarning("Lỗi", "Vui lòng nhập đủ tài khoản và mật khẩu!");
            return false;
        }

        if (!ValidationHelper.isValidUsername(username)) {
            AlertHelper.showWarning("Tên đăng nhập không hợp lệ",
                    "Tên đăng nhập phải từ 3-20 ký tự, chỉ chứa chữ cái, số và dấu gạch dưới.");
            return false;
        }

        if (!ValidationHelper.isStrongPassword(password)) {
            AlertHelper.showWarning("Mật khẩu yếu",
                    "Mật khẩu phải từ 8 ký tự trở lên, bao gồm ít nhất 1 chữ cái và 1 chữ số.");
            return false;
        }
        return true;
    }

    private static void registerResponse(Stage stage) {
        ClientNetworkManager.getInstance().clearListeners(Protocol.REQ_REGISTER);
        ClientNetworkManager.getInstance().registerListener(Protocol.REQ_REGISTER, response -> {
            String[] parts = response.split(Protocol.DELIMITER);
            Platform.runLater(() -> handleRegisterResponse(parts, stage));
        });
    }

    private static void handleRegisterResponse(String[] parts, Stage stage) {
        if (parts.length > 1 && parts[1].equals(Protocol.RES_SUCCESS)) {
            AlertHelper.showInfo("Thành công", "Đăng ký thành công! Hãy tiến hành đăng nhập.");
            SceneManager.switchScene(stage, "/view/auth/Login.fxml", "Login");
            return;
        }

        AlertHelper.showError("Thất bại", parts.length >= 3 ? parts[2] : "Tài khoản đã tồn tại!");
    }
}
