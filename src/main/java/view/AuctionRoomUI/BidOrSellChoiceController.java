package view.AuctionRoomUI;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.stage.Stage;
import view.utility.SceneManager;

public class BidOrSellChoiceController {
    @FXML
    private void SellChoiceClicked(ActionEvent event) {
        Stage currentStage = (Stage) ((javafx.scene.Node) event.getSource()).getScene().getWindow();
        SceneManager.switchScene(currentStage, "/view/SellerUI/CreateItem.fxml", "Đăng bán sản phẩm");
    }

    @FXML
    private void BidChoiceClicked(ActionEvent event) {
        Stage currentStage = (Stage) ((javafx.scene.Node) event.getSource()).getScene().getWindow();
        SceneManager.switchScene(currentStage, "/view/BidderUI/BidItem.fxml", "Đặt giá sản phẩm");
    }

    @FXML
    private void backToMenu(ActionEvent event) {
        Stage currentStage = (Stage) ((javafx.scene.Node) event.getSource()).getScene().getWindow();
        SceneManager.switchScene(currentStage, "/view/BaseMenuUI/BaseMenu.fxml", "Trang chủ");
    }
}
