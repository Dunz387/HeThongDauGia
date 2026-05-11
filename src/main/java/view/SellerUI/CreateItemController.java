package view.SellerUI;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import network.ClientNetworkManager;
import shared.Protocol;
import view.utility.SceneManager;
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

        String itemTypeCode = "ELECTRONICS";
        if (itemTypeDisplay.equals("Đồ điện")) itemTypeCode = "ELECTRONICS";
        else if (itemTypeDisplay.equals("Xe cộ")) itemTypeCode = "VEHICLE";
        else if (itemTypeDisplay.equals("Nghệ thuật")) itemTypeCode = "ART";

        String request = Protocol.REQ_CREATE_ITEM + Protocol.DELIMITER + itemName + Protocol.DELIMITER + startPrice + Protocol.DELIMITER + duration + Protocol.DELIMITER + itemTypeCode;

        // ĐĂNG KÝ CALLBACK
        ClientNetworkManager.getInstance().registerListener(Protocol.REQ_CREATE_ITEM, (response) -> {
            String[] parts = response.split(Protocol.SEPARATOR);
            Platform.runLater(() -> {
                if (parts.length > 1 && parts[1].equals(Protocol.RES_SUCCESS)) {
                    showAlert("Thành công", "Đã đăng bán sản phẩm lên sàn!", Alert.AlertType.INFORMATION);
                    
                    // Lấy cửa sổ Popup hiện tại
                    Stage popupStage = (Stage) txtItemName.getScene().getWindow();
                    
                    // Lấy cửa sổ chính (owner)
                    Stage mainStage = (Stage) popupStage.getOwner();
                    
                    // Đóng cửa sổ popup
                    popupStage.close();
                    
                    // Mở popup danh sách phòng đấu giá của Seller
                    if (mainStage != null) {
                        view.utility.WindowManager.openSellerAuctionListWindow(mainStage);
                    } else {
                        view.utility.WindowManager.openSellerAuctionListWindow(null);
                    }
                } else {
                    showAlert("Thất bại", parts.length >= 3 ? parts[2] : "Lỗi không xác định", Alert.AlertType.ERROR);
                }
            });
        });

        // GỬI LỆNH ĐI
        ClientNetworkManager.getInstance().sendData(request);
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