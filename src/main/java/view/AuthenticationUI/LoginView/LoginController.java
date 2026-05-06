package view.AuthenticationUI.LoginView;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import network.ClientNetworkManager;
import shared.Protocol;
import view.SceneManager;

public class LoginController {

    // Khai báo 2 ô nhập liệu (Nhớ kiểm tra bên file FXML xem đã đặt fx:id đúng tên này chưa nhé)
    @FXML private TextField txtUsername;
    @FXML private TextField txtPassword;

    @FXML
    private void loginButtonClicked(ActionEvent event) {
        String username = txtUsername.getText();
        String password = txtPassword.getText();

        if (username == null || username.trim().isEmpty() || password == null || password.trim().isEmpty()) {
            showAlert("Lỗi", "Vui lòng nhập đủ tài khoản và mật khẩu!");
            return;
        }

        // 1. Gửi lệnh Login qua Trạm mạng mà bạn đã viết
        String request = Protocol.REQ_LOGIN + Protocol.DELIMITER + username.trim() + Protocol.DELIMITER + password.trim();
        ClientNetworkManager.getInstance().sendData(request);

        // 2. Chờ Server phản hồi
        new Thread(() -> {
            try {
                String response = null;
                int timeout = 50; // Đợi tối đa 5 giây

                while (response == null && timeout > 0) {
                    response = ClientNetworkManager.getInstance().getLastResponse();
                    Thread.sleep(100);
                    timeout--;
                }

                // 3. Có kết quả thì chuyển màn hình
                if (response != null) {
                    String[] parts = response.split(Protocol.SEPARATOR);
                    Platform.runLater(() -> {
                        if (parts.length > 1 && parts[1].equals(Protocol.RES_SUCCESS)) {
                            Stage stage = (Stage) txtUsername.getScene().getWindow();
                            SceneManager.switchScene(stage, "/view/BaseMenuUI/BaseMenu.fxml", "Trang Chủ");
                        } else {
                            showAlert("Thất bại", parts.length >= 3 ? parts[2] : "Sai thông tin đăng nhập!");
                        }
                    });
                } else {
                    Platform.runLater(() -> showAlert("Lỗi Mạng", "Không nhận được phản hồi từ Server."));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    @FXML
    private void registerLinkClicked(ActionEvent event) {
        Stage stage = (Stage) ((javafx.scene.Node) event.getSource()).getScene().getWindow();
        SceneManager.switchScene(stage, "/view/AuthenticationUI/RegisterView/Register.fxml", "Register");
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}