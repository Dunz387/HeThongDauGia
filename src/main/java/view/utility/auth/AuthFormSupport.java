package view.utility.auth;

import javafx.collections.FXCollections;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;

public final class AuthFormSupport {
    private AuthFormSupport() {
    }

    public static void configureLoginFields(TextField txtUsername, TextField txtPassword, javafx.event.EventHandler<javafx.event.ActionEvent> loginAction) {
        txtUsername.setOnAction(event -> txtPassword.requestFocus());
        txtPassword.setOnAction(loginAction);
    }

    public static void configureRegisterFields(TextField txtUsername, TextField txtPassword, ComboBox<String> cbRole) {
        cbRole.setItems(FXCollections.observableArrayList("Người Mua (Bidder)", "Người Bán (Seller)"));
        cbRole.getSelectionModel().selectFirst();
        txtUsername.setOnAction(event -> txtPassword.requestFocus());
        txtPassword.setOnAction(event -> cbRole.requestFocus());
    }

    public static String selectedRole(ComboBox<String> cbRole) {
        return cbRole.getValue() != null && cbRole.getValue().contains("Seller") ? "SELLER" : "BIDDER";
    }
}
