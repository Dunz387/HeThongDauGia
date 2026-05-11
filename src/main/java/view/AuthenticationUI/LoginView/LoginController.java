package view.AuthenticationUI.LoginView;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import network.ClientNetworkManager;
import shared.Protocol;
import view.utility.SceneManager;

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

        // 1. Chuẩn bị yêu cầu
        String request = Protocol.REQ_LOGIN + Protocol.DELIMITER + username.trim() + Protocol.DELIMITER + password.trim();

        // 2. ĐĂNG KÝ CALLBACK: "Khi nào Server trả lời lệnh LOGIN, hãy chạy đoạn code này trên UI"
        ClientNetworkManager.getInstance().registerListener(Protocol.REQ_LOGIN, (response) -> {
            String[] parts = response.split(Protocol.SEPARATOR);
            Platform.runLater(() -> {
                if (parts.length > 1 && parts[1].equals(Protocol.RES_SUCCESS)) {
                    Stage stage = (Stage) txtUsername.getScene().getWindow();
                    // THÊM MỚI: Phân luồng theo Role - Admin vào Dashboard, User vào BaseMenu
                    String role = parts.length >= 3 ? parts[2] : "";
                    if ("ADMIN".equals(role)) {
                        SceneManager.goToAdminDashboard(stage);
                    } else {
                        SceneManager.switchScene(stage, "/view/BaseMenuUI/BaseMenu.fxml", "Trang Chủ");
                    }
                } else {
                    showAlert("Thất bại", parts.length >= 3 ? parts[2] : "Sai thông tin đăng nhập!");
                }
            });
        });

        // 3. Gửi đi và xong việc (Không còn Thread.sleep bắt UI phải chờ nữa)
        ClientNetworkManager.getInstance().sendData(request);
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