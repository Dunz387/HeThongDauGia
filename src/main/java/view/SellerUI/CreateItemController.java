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
    @FXML private javafx.scene.control.TextArea txtDescription;
    @FXML private ChoiceBox<String> choiceType;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        choiceType.getItems().addAll("Đồ điện", "Xe cộ", "Nghệ thuật");
        choiceType.setValue(null);

        txtItemName.setOnAction(event -> txtStartPrice.requestFocus());
        txtStartPrice.setOnAction(event -> txtDuration.requestFocus());
        txtDuration.setOnAction(event -> txtDescription.requestFocus());
    }

    @FXML
    private void createItemButtonClicked(ActionEvent event) {
        String itemName = txtItemName.getText().trim();
        String startPrice = txtStartPrice.getText().trim();
        String duration = txtDuration.getText().trim();
        String itemTypeDisplay = choiceType.getValue();

        if (view.utility.ValidationHelper.isEmpty(itemName) || view.utility.ValidationHelper.isEmpty(startPrice) || 
            view.utility.ValidationHelper.isEmpty(duration) || itemTypeDisplay == null) {
            AlertHelper.showWarning("Lỗi", "Vui lòng nhập đầy đủ thông tin!");
            return;
        }

        // Kiểm tra tính hợp lệ của số liệu
        if (!view.utility.ValidationHelper.isValidStartPrice(startPrice)) {
            AlertHelper.showWarning("Lỗi dữ liệu", "Giá khởi điểm phải là một số thực lớn hơn 0.");
            return;
        }

        if (!view.utility.ValidationHelper.isValidDuration(duration)) {
            AlertHelper.showWarning("Lỗi dữ liệu", "Thời lượng phiên phải là số nguyên (phút) và lớn hơn 0.");
            return;
        }

        // T7: Lấy Stage ngay tại đây để dùng an toàn trong callback
        Stage currentStage = (Stage) ((javafx.scene.Node) event.getSource()).getScene().getWindow();

        String itemTypeCode = switch (itemTypeDisplay) {
            case "Xe cộ" -> "VEHICLE";
            case "Nghệ thuật" -> "ART";
            default -> "ELECTRONICS";
        };

        String description = txtDescription.getText().trim();
        if (description.isEmpty()) description = "Chưa có mô tả";

        String request = Protocol.REQ_CREATE_ITEM + Protocol.DELIMITER + 
                         itemName + Protocol.DELIMITER + 
                         startPrice + Protocol.DELIMITER + 
                         duration + Protocol.DELIMITER + 
                         itemTypeCode + Protocol.DELIMITER + 
                         description.replace("\n", " ");

        // T7: Clear listener cũ trước khi đăng ký mới
        ClientNetworkManager.getInstance().clearListeners(Protocol.REQ_CREATE_ITEM);
        ClientNetworkManager.getInstance().registerListener(Protocol.REQ_CREATE_ITEM, (response) -> {
            String[] parts = response.split(Protocol.SEPARATOR);
            Platform.runLater(() -> {
                if (parts.length > 1 && parts[1].equals(Protocol.RES_SUCCESS)) {
                    AlertHelper.showInfo("Thành công", "Đã đăng bán sản phẩm lên sàn!");
                    Stage mainStage = (Stage) currentStage.getOwner();
                    currentStage.close();
                    view.utility.WindowManager.openSellerAuctionListWindow(mainStage);
                } else {
                    AlertHelper.showError("Thất bại", parts.length >= 3 ? parts[2] : "Lỗi không xác định");
                }
            });
        });

        if (!ClientNetworkManager.getInstance().sendData(request)) {
            AlertHelper.showError("Lỗi kết nối", "Không thể gửi yêu cầu tới Server. Vui lòng kiểm tra kết nối mạng!");
        }
    }

    @FXML
    private void cancelButtonClicked(ActionEvent event) {
        Stage stage = (Stage) txtItemName.getScene().getWindow();
        stage.close();
    }
}