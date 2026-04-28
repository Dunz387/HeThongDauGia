package view.BaseMenuUI;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import view.SceneManager;

public class BaseMenuController {
    @FXML
    private void backToLoiginButtonClicked(ActionEvent event) {
        // Handle logic to switch back to login screen here
        SceneManager.switchScene(event, "/view/AuthenticationUI/LoginView/Login.fxml", "Login");
    }

    @FXML
    private void backToRegisterButtonClicked(ActionEvent event) {
        // Handle logic to switch back to register screen here
        SceneManager.switchScene(event, "/view/AuthenticationUI/RegisterView/Register.fxml", "Register");
    }
}
