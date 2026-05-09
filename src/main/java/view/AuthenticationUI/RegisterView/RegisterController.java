package view.AuthenticationUI.RegisterView;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import network.ClientNetworkManager;
import shared.Protocol;
import view.utility.SceneManager;

public class RegisterController {

    @FXML private TextField txtUsername;
    @FXML private TextField txtPassword;

    @FXML
    private void registerButtonClicked(ActionEvent event) {
        String username = txtUsername.getText();
        String password = txtPassword.getText();

        if (username == null || username.trim().isEmpty() || password == null || password.trim().isEmpty()) {
            showAlert("Lỗi", "Vui lòng nhập đủ tài khoản và mật khẩu!", Alert.AlertType.WARNING);
            return;
        }

        // 1. Chuẩn bị yêu cầu đăng ký
        String request = Protocol.REQ_REGISTER + Protocol.DELIMITER + username.trim() + Protocol.DELIMITER + password.trim();

        // 2. ĐĂNG KÝ CALLBACK: "Khi nào Server trả lời lệnh REGISTER, hãy chạy đoạn code này trên UI"
        ClientNetworkManager.getInstance().registerListener(Protocol.REQ_REGISTER, (response) -> {
            String[] parts = response.split(Protocol.SEPARATOR);
            Platform.runLater(() -> {
                if (parts.length > 1 && parts[1].equals(Protocol.RES_SUCCESS)) {
                    showAlert("Thành công", "Đăng ký thành công! Hãy tiến hành đăng nhập.", Alert.AlertType.INFORMATION);

                    // Đăng ký xong thì tự động quay về màn hình Login
                    Stage stage = (Stage) txtUsername.getScene().getWindow();
                    SceneManager.switchScene(stage, "/view/AuthenticationUI/LoginView/Login.fxml", "Login");
                } else {
                    showAlert("Thất bại", parts.length >= 3 ? parts[2] : "Tài khoản đã tồn tại!", Alert.AlertType.ERROR);
                }
            });
        });

        // 3. Gửi lệnh đi và kết thúc hàm (Không còn Thread.sleep bắt UI phải chờ nữa)
        ClientNetworkManager.getInstance().sendData(request);
    }

    // Hàm gắn vào nút/chữ "Quay lại Đăng nhập"
    @FXML
    private void backToLoginLinkClicked(ActionEvent event) {
        Stage stage = (Stage) ((javafx.scene.Node) event.getSource()).getScene().getWindow();
        SceneManager.switchScene(stage, "/view/AuthenticationUI/LoginView/Login.fxml", "Login");
    }

    private void showAlert(String title, String content, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}