package view.AuctionRoomUI;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.stage.Stage;
import view.utility.SceneManager;


public class RoomMenuChoiceController {
    @FXML
    private void backToBaseMenu(ActionEvent event) {
        Stage stage = (Stage) ((javafx.scene.Node) event.getSource()).getScene().getWindow();
        SceneManager.goToBaseMenu(stage);
    }
}
