package view.SellerUI;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import network.ClientNetworkManager;
import shared.Protocol;
import view.utility.AlertHelper;
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
        choiceType.getItems().addAll("Đồ điện", "Xe cộ", "Nghệ thuật");
        choiceType.setValue(null);
    }

    @FXML
    private void createItemButtonClicked(ActionEvent event) {
        String itemName = txtItemName.getText().trim();
        String startPrice = txtStartPrice.getText().trim();
        String duration = txtDuration.getText().trim();
        String itemTypeDisplay = choiceType.getValue();

        if (itemName.isEmpty() || startPrice.isEmpty() || duration.isEmpty() || itemTypeDisplay == null) {
            AlertHelper.showWarning("Lỗi", "Vui lòng nhập đầy đủ thông tin!");
            return;
        }

        String itemTypeCode = switch (itemTypeDisplay) {
            case "Xe cộ" -> "VEHICLE";
            case "Nghệ thuật" -> "ART";
            default -> "ELECTRONICS";
        };

        String request = Protocol.REQ_CREATE_ITEM + Protocol.DELIMITER + itemName + Protocol.DELIMITER + startPrice + Protocol.DELIMITER + duration + Protocol.DELIMITER + itemTypeCode;

        ClientNetworkManager.getInstance().registerListener(Protocol.REQ_CREATE_ITEM, (response) -> {
            String[] parts = response.split(Protocol.SEPARATOR);
            Platform.runLater(() -> {
                if (parts.length > 1 && parts[1].equals(Protocol.RES_SUCCESS)) {
                    AlertHelper.showInfo("Thành công", "Đã đăng bán sản phẩm lên sàn!");
                    Stage popupStage = (Stage) txtItemName.getScene().getWindow();
                    Stage mainStage = (Stage) popupStage.getOwner();
                    popupStage.close();
                    view.utility.WindowManager.openSellerAuctionListWindow(mainStage != null ? mainStage : null);
                } else {
                    AlertHelper.showError("Thất bại", parts.length >= 3 ? parts[2] : "Lỗi không xác định");
                }
            });
        });

        ClientNetworkManager.getInstance().sendData(request);
    }

    @FXML
    private void cancelButtonClicked(ActionEvent event) {
        Stage stage = (Stage) txtItemName.getScene().getWindow();
        stage.close();
    }
}