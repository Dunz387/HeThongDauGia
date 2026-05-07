package view.SellerUI;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import network.ClientNetworkManager;
import shared.Protocol;
import javafx.scene.control.ChoiceBox;
import javafx.fxml.Initializable;

import java.net.URL;
import java.util.ResourceBundle;

public class CreateItemController implements Initializable {

    @FXML private TextField txtItemName;
    @FXML private TextField txtStartPrice;
    @FXML private TextField txtDuration;
    @FXML private ChoiceBox<String> choiceType;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        String[] itemTypes = {"Đồ điện", "Xe cộ", "Nghệ thuật"};
        choiceType.getItems().addAll(itemTypes);
        choiceType.setValue(null);
    }

    @FXML
    private void createItemButtonClicked(ActionEvent event) {
        String itemName = txtItemName.getText().trim();
        String startPrice = txtStartPrice.getText().trim();
        String duration = txtDuration.getText().trim();
        String itemTypeDisplay = choiceType.getValue();

        if (itemName.isEmpty() || startPrice.isEmpty() || duration.isEmpty() || itemTypeDisplay == null) {
            showAlert("Lỗi", "Vui lòng nhập đầy đủ thông tin!", Alert.AlertType.WARNING);
            return;
        }

        // Chuyển đổi (Map) lựa chọn tiếng Việt trên UI sang mã chuẩn của Server
        String itemTypeCode = "";
        switch (itemTypeDisplay) {
            case "Đồ điện":
                itemTypeCode = "ELECTRONICS";
                break;
            case "Xe cộ":
                itemTypeCode = "VEHICLE";
                break;
            case "Nghệ thuật":
                itemTypeCode = "ART";
                break;
            default:
                itemTypeCode = "ELECTRONICS"; // Mặc định an toàn
                break;
        }

        // Tạo lệnh gửi lên Server (Đã bổ sung itemTypeCode vào gói tin gửi đi)
        String request = Protocol.REQ_CREATE_ITEM + Protocol.DELIMITER + itemName + Protocol.DELIMITER + startPrice + Protocol.DELIMITER + duration + Protocol.DELIMITER + itemTypeCode;
        ClientNetworkManager.getInstance().sendData(request);

        // Mở luồng ngầm chờ kết quả
        new Thread(() -> {
            try {
                String response = null;
                int timeout = 50;

                while (timeout > 0) {
                    // Dùng đúng "hòm thư" của Create Item
                    response = ClientNetworkManager.getInstance().getLastCreateItemResponse();

                    if (response != null && response.startsWith(Protocol.REQ_CREATE_ITEM)) {
                        break;
                    }
                    Thread.sleep(100);
                    timeout--;
                }

                if (response != null) {
                    String[] parts = response.split(Protocol.SEPARATOR);
                    Platform.runLater(() -> {
                        if (parts.length > 1 && parts[1].equals(Protocol.RES_SUCCESS)) {
                            showAlert("Thành công", "Đã đăng bán sản phẩm lên sàn!", Alert.AlertType.INFORMATION);
                            closeWindow();
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