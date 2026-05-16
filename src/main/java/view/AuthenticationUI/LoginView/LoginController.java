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
    public void initialize() {
        txtUsername.setOnAction(event -> txtPassword.requestFocus());
        txtPassword.setOnAction(this::loginButtonClicked);
    }

    @FXML
    private void loginButtonClicked(ActionEvent event) {
        String username = txtUsername.getText().trim();
        String password = txtPassword.getText().trim();

        if (view.utility.ValidationHelper.isEmpty(username) || view.utility.ValidationHelper.isEmpty(password)) {
            AlertHelper.showWarning("Lỗi", "Vui lòng nhập đủ tài khoản và mật khẩu!");
            return;
        }

        Stage stage = (Stage) ((javafx.scene.Node) event.getSource()).getScene().getWindow();
        String request = Protocol.REQ_LOGIN + Protocol.DELIMITER + username + Protocol.DELIMITER + password;

        // T7: Xóa các listener cũ
        ClientNetworkManager.getInstance().clearListeners(Protocol.REQ_LOGIN);
        ClientNetworkManager.getInstance().registerListener(Protocol.REQ_LOGIN, (response) -> {
            String[] parts = response.split(Protocol.SEPARATOR);
            Platform.runLater(() -> {
                if (parts.length > 1 && parts[1].equals(Protocol.RES_SUCCESS)) {
                    String role = parts.length >= 3 ? parts[2] : "";
                    String userId = parts.length >= 4 ? parts[3] : "";
                    String userName = parts.length >= 5 ? parts[4] : "";
                    double balance = parts.length >= 6 ? Double.parseDouble(parts[5]) : 0.0;

                    SessionManager.getInstance().setSession(userId, userName, role, balance);
                    network.NotificationManager.getInstance().loadFromDatabase(userId);

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

        // Chạy kết nối và gửi dữ liệu trong thread riêng để không treo UI
        new Thread(() -> {
            try {
                if (!ClientNetworkManager.getInstance().isConnected()) {
                    LOGGER.info("🔄 Đang thử kết nối lại tới Server...");
                    if (!ClientNetworkManager.getInstance().connect("localhost", 8080)) {
                        Platform.runLater(() -> AlertHelper.showError("Lỗi", "Không thể kết nối tới Server. Hãy đảm bảo Server đang chạy!"));
                        return;
                    }
                }
                
                boolean sent = ClientNetworkManager.getInstance().sendData(request);
                if (!sent) {
                    Platform.runLater(() -> AlertHelper.showError("Lỗi", "Không thể gửi yêu cầu đăng nhập. Thử lại sau!"));
                }
            } catch (Exception e) {
                Platform.runLater(() -> AlertHelper.showError("Lỗi hệ thống", "Đã xảy ra lỗi khi đăng nhập: " + e.getMessage()));
            }
        }).start();
    }
    
    private static final java.util.logging.Logger LOGGER = java.util.logging.Logger.getLogger(LoginController.class.getName());

    @FXML
    private void registerLinkClicked(ActionEvent event) {
        Stage stage = (Stage) ((javafx.scene.Node) event.getSource()).getScene().getWindow();
        SceneManager.switchScene(stage, "/view/AuthenticationUI/RegisterView/Register.fxml", "Register");
    }
}