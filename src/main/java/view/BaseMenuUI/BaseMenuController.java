package view.BaseMenuUI;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import network.ClientNetworkManager;
import shared.Protocol;
import view.SceneManager;

import java.net.URL;
import java.util.ResourceBundle;

public class BaseMenuController implements Initializable {
    @FXML
    private HBox menuBar;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Vừa vào trang chủ là tự động xin Server danh sách hàng
        ClientNetworkManager.getInstance().sendData(Protocol.REQ_GET_AUCTIONS);

        // (Sau này sẽ viết luồng hứng gói tin RES_AUCTION_LIST từ luồng ngầm để đổ vào TableView)
    }

    @FXML
    private void backToLoiginButtonClicked(ActionEvent event) {
        // Handle logic to switch back to login screen here
        Stage stage = (Stage) menuBar.getScene().getWindow();
        SceneManager.switchScene(stage, "/view/AuthenticationUI/LoginView/Login.fxml", "Login");
    }

    @FXML
    private void backToRegisterButtonClicked(ActionEvent event) {
        // Handle logic to switch back to register screen here
        Stage stage = (Stage) menuBar.getScene().getWindow();
        SceneManager.switchScene(stage, "/view/AuthenticationUI/RegisterView/Register.fxml", "Register");
    }
}