package view.SellerUI;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import network.ClientNetworkManager;
import shared.Protocol;

public class CreateItemController {

    @FXML private TextField txtItemName;
    @FXML private TextField txtStartPrice;
    @FXML private TextField txtDuration;

    @FXML
    private void createItemButtonClicked(ActionEvent event) {
        String itemName = txtItemName.getText().trim();
        String startPrice = txtStartPrice.getText().trim();
        String duration = txtDuration.getText().trim();

        if (itemName.isEmpty() || startPrice.isEmpty() || duration.isEmpty()) {
            showAlert("Lỗi", "Vui lòng nhập đầy đủ thông tin!", Alert.AlertType.WARNING);
            return;
        }

        // Đóng gói gửi lên Server: CREATE_ITEM | Tên_Hàng | Giá | Thời_Gian
        String request = Protocol.REQ_CREATE_ITEM + Protocol.DELIMITER + itemName + Protocol.DELIMITER + startPrice + Protocol.DELIMITER + duration;
        ClientNetworkManager.getInstance().sendData(request);

        // Mở luồng ngầm chờ Server trả lời
        new Thread(() -> {
            try {
                String response = null;
                int timeout = 50;
                while (response == null && timeout > 0) {
                    response = ClientNetworkManager.getInstance().getLastResponse();
                    Thread.sleep(100);
                    timeout--;
                }

                if (response != null) {
                    String[] parts = response.split(Protocol.SEPARATOR);
                    Platform.runLater(() -> {
                        if (parts.length > 1 && parts[1].equals(Protocol.RES_SUCCESS)) {
                            showAlert("Thành công", "Đã đăng bán sản phẩm lên sàn!", Alert.AlertType.INFORMATION);
                            closeWindow(); // Đóng popup khi thành công
                        } else {
                            showAlert("Thất bại", parts.length >= 3 ? parts[2] : "Lỗi không xác định", Alert.AlertType.ERROR);
                        }
                    });
                } else {
                    Platform.runLater(() -> showAlert("Lỗi Mạng", "Server không phản hồi.", Alert.AlertType.ERROR));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    @FXML
    private void cancelButtonClicked(ActionEvent event) {
        closeWindow();
    }

    private void closeWindow() {
        Stage stage = (Stage) txtItemName.getScene().getWindow();
        stage.close();
    }

    private void showAlert(String title, String content, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}