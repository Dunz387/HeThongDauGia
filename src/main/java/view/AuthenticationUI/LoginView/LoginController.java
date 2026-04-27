package view.AuthenticationUI.LoginView;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import view.SceneManager;

public class LoginController {
    @FXML
    private void loginButtonClicked(ActionEvent event) {
        // Handle login logic here
    }

    @FXML
    private void registerLinkClicked(ActionEvent event) {
        SceneManager.switchScene(event, "/view/AuthenticationUI/RegisterView/Register.fxml", "Register");
    }
}
