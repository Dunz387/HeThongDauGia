package view.AuthenticationUI.RegisterView;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import view.SceneManager;

public class RegisterController {
    @FXML
    private void registerButtonClicked(ActionEvent event) {
        // Handle register logic here
    }

    @FXML
    private void backToLoginLinkClicked(ActionEvent event) {
        // Handle logic to switch back to login screen here
        SceneManager.switchScene(event, "/view/AuthenticationUI/LoginView/Login.fxml", "Login");
    }

}
