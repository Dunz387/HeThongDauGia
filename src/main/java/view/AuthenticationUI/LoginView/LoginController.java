package view.AuthenticationUI.LoginView;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import network.ClientNetworkManager;
import network.SessionManager;
import shared.Protocol;
import view.utility.AlertHelper;
import view.utility.SceneManager;

public class LoginController {

    // Khai báo 2 ô nhập liệu (Nhớ kiểm tra bên file FXML xem đã đặt fx:id đúng tên này chưa nhé)
    @FXML private TextField txtUsername;
    @FXML private TextField txtPassword;

    @FXML
    private void loginButtonClicked(ActionEvent event) {
        String username = txtUsername.getText().trim();
        String password = txtPassword.getText().trim();

        if (view.utility.ValidationHelper.isEmpty(username) || view.utility.ValidationHelper.isEmpty(password)) {
            AlertHelper.showWarning("Lỗi", "Vui lòng nhập đủ tài khoản và mật khẩu!");
            return;
        }

        // T7: Lấy Stage ngay tại đây từ sự kiện để tránh lỗi Null sau này trong callback
        Stage stage = (Stage) ((javafx.scene.Node) event.getSource()).getScene().getWindow();

        // 1. Chuẩn bị yêu cầu
        String request = Protocol.REQ_LOGIN + Protocol.DELIMITER + username.trim() + Protocol.DELIMITER + password.trim();

        // 2. ĐĂNG KÝ CALLBACK
        // T7: Xóa các listener cũ để tránh việc callback chạy nhiều lần (Memory leak & Bug)
        ClientNetworkManager.getInstance().clearListeners(Protocol.REQ_LOGIN);
        ClientNetworkManager.getInstance().registerListener(Protocol.REQ_LOGIN, (response) -> {
            String[] parts = response.split(Protocol.SEPARATOR);
            Platform.runLater(() -> {
                if (parts.length > 1 && parts[1].equals(Protocol.RES_SUCCESS)) {
                    // Parse thông tin user từ response
                    String role = parts.length >= 3 ? parts[2] : "";
                    String userId = parts.length >= 4 ? parts[3] : "";
                    String userName = parts.length >= 5 ? parts[4] : "";
                    double balance = parts.length >= 6 ? Double.parseDouble(parts[5]) : 0.0;

                    // Lưu thông tin phiên đăng nhập
                    SessionManager.getInstance().setSession(userId, userName, role, balance);

                    // T10: Load lại lịch sử thông báo của tài khoản từ DB
                    network.NotificationManager.getInstance().loadFromDatabase(userId);

                    // Phân luồng theo Role
                    if ("ADMIN".equals(role)) {
                        SceneManager.goToAdminDashboard(stage);
                    } else {
                        SceneManager.switchScene(stage, "/view/BaseMenuUI/BaseMenu.fxml", "Trang Chủ");
                    }
                } else {
                    AlertHelper.showWarning("Thất bại", parts.length >= 3 ? parts[2] : "Sai thông tin đăng nhập!");
                }
            });
        });

        // 3. Gửi đi
        ClientNetworkManager.getInstance().sendData(request);
    }

    @FXML
    private void registerLinkClicked(ActionEvent event) {
        Stage stage = (Stage) ((javafx.scene.Node) event.getSource()).getScene().getWindow();
        SceneManager.switchScene(stage, "/view/AuthenticationUI/RegisterView/Register.fxml", "Register");
    }
}