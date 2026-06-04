package view.controller.auth;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import view.utility.auth.AuthFormSupport;
import view.utility.auth.AuthNavigationSupport;
import view.utility.auth.RegisterSupport;

import java.net.URL;
import java.util.ResourceBundle;

public class RegisterController implements Initializable {
    @FXML private TextField txtUsername;
    @FXML private TextField txtPassword;
    @FXML private ComboBox<String> cbRole;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        AuthFormSupport.configureRegisterFields(txtUsername, txtPassword, cbRole);
    }

    @FXML
    private void registerButtonClicked(ActionEvent event) {
        Stage stage = (Stage) txtUsername.getScene().getWindow();
        RegisterSupport.register(
                txtUsername.getText().trim(),
                txtPassword.getText().trim(),
                AuthFormSupport.selectedRole(cbRole),
                stage);
    }

    @FXML
    private void backToLoginLinkClicked(ActionEvent event) {
        AuthNavigationSupport.goToLogin(event);
    }
}
