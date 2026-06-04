package view.utility.auth;

import javafx.event.ActionEvent;
import javafx.scene.Node;
import javafx.stage.Stage;
import view.utility.navigation.SceneManager;

public final class AuthNavigationSupport {
    private AuthNavigationSupport() {
    }

    public static Stage stageFrom(ActionEvent event) {
        return (Stage) ((Node) event.getSource()).getScene().getWindow();
    }

    public static void goToLogin(ActionEvent event) {
        SceneManager.switchScene(stageFrom(event), "/view/auth/Login.fxml", "Login");
    }

    public static void goToRegister(ActionEvent event) {
        SceneManager.switchScene(stageFrom(event), "/view/auth/Register.fxml", "Register");
    }
}
