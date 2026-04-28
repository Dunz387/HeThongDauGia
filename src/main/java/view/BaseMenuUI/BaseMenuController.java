package view.BaseMenuUI;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import view.SceneManager;

public class BaseMenuController {
    @FXML
    private HBox menuBar;

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
