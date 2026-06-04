package view.controller.auth;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import view.utility.auth.AuthFormSupport;
import view.utility.auth.AuthNavigationSupport;
import view.utility.auth.ForceLogoutSupport;
import view.utility.auth.LoginSupport;

public class LoginController {
    @FXML private TextField txtUsername;
    @FXML private TextField txtPassword;

    @FXML
    public void initialize() {
        AuthFormSupport.configureLoginFields(txtUsername, txtPassword, this::loginButtonClicked);
        ForceLogoutSupport.registerForceLogoutListener();
    }

    @FXML
    private void loginButtonClicked(ActionEvent event) {
        Stage stage = AuthNavigationSupport.stageFrom(event);
        LoginSupport.login(txtUsername.getText().trim(), txtPassword.getText().trim(), stage);
    }

    @FXML
    private void registerLinkClicked(ActionEvent event) {
        AuthNavigationSupport.goToRegister(event);
    }
}
