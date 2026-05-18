package view.controller.auth;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import java.net.URL;
import java.util.ResourceBundle;
import javafx.stage.Stage;
import network.ClientNetworkManager;
import shared.Protocol;
import view.utility.AlertHelper;
import view.utility.SceneManager;

public class RegisterController implements Initializable {

    @FXML private TextField txtUsername;
    @FXML private TextField txtPassword;
    @FXML private ComboBox<String> cbRole;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        cbRole.setItems(FXCollections.observableArrayList("Người Mua (Bidder)", "Người Bán (Seller)"));
        cbRole.getSelectionModel().selectFirst();

        txtUsername.setOnAction(event -> txtPassword.requestFocus());
        txtPassword.setOnAction(event -> cbRole.requestFocus());
    }

    @FXML
    private void registerButtonClicked(ActionEvent event) {
        String username = txtUsername.getText().trim();
        String password = txtPassword.getText().trim();

        // ÁP DỤNG VALIDATION MỚI (Mật khẩu 8 ký tự, Tên đăng nhập hợp lệ)
        if (view.utility.ValidationHelper.isEmpty(username) || view.utility.ValidationHelper.isEmpty(password)) {
            AlertHelper.showWarning("Lỗi", "Vui lòng nhập đủ tài khoản và mật khẩu!");
            return;
        }

        if (!view.utility.ValidationHelper.isValidUsername(username)) {
            AlertHelper.showWarning("Tên đăng nhập không hợp lệ", 
                "Tên đăng nhập phải từ 3-20 ký tự, chỉ chứa chữ cái, số và dấu gạch dưới.");
            return;
        }

        if (!view.utility.ValidationHelper.isStrongPassword(password)) {
            AlertHelper.showWarning("Mật khẩu yếu", 
                "Mật khẩu phải từ 8 ký tự trở lên, bao gồm ít nhất 1 chữ cái và 1 chữ số.");
            return;
        }

        // Lấy Stage an toàn từ một Node có sẵn trong giao diện (bảo đảm không bao giờ Null)
        Stage stage = (Stage) txtUsername.getScene().getWindow();

        String role = cbRole.getValue() != null && cbRole.getValue().contains("Seller") ? "SELLER" : "BIDDER";

        // 1. Chuẩn bị yêu cầu đăng ký
        String request = Protocol.REQ_REGISTER + Protocol.DELIMITER + username.trim() + Protocol.DELIMITER + password.trim() + Protocol.DELIMITER + role;

        // 2. ĐĂNG KÝ CALLBACK
        // T7: Xóa các listener cũ để tránh lỗi callback chồng chéo
        ClientNetworkManager.getInstance().clearListeners(Protocol.REQ_REGISTER);
        ClientNetworkManager.getInstance().registerListener(Protocol.REQ_REGISTER, (response) -> {
            String[] parts = response.split(Protocol.DELIMITER);
            Platform.runLater(() -> {
                if (parts.length > 1 && parts[1].equals(Protocol.RES_SUCCESS)) {
                    AlertHelper.showInfo("Thành công", "Đăng ký thành công! Hãy tiến hành đăng nhập.");
                    // Quay về màn hình Login bằng Stage đã bắt được
                    SceneManager.switchScene(stage, "/view/auth/Login.fxml", "Login");
                } else {
                    AlertHelper.showError("Thất bại", parts.length >= 3 ? parts[2] : "Tài khoản đã tồn tại!");
                }
            });
        });

        // 3. Gửi lệnh đi
        ClientNetworkManager.getInstance().sendData(request);
    }

    // Hàm gắn vào nút/chữ "Quay lại Đăng nhập"
    @FXML
    private void backToLoginLinkClicked(ActionEvent event) {
        Stage stage = (Stage) ((javafx.scene.Node) event.getSource()).getScene().getWindow();
        SceneManager.switchScene(stage, "/view/auth/Login.fxml", "Login");
    }
}