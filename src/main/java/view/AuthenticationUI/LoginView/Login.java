package view.AuthenticationUI.LoginView;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Login extends Application {
    @Override
    public void start(Stage LoginStage) throws Exception {
        Parent root = FXMLLoader.load(getClass().getResource("/view/AuthenticationUI/LoginView/Login.fxml"));
        Scene scene = new Scene(root);

        LoginStage.setTitle("Login");
        LoginStage.setScene(scene);
        LoginStage.show();

    }
}