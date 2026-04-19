package view;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;

public class LoginGUI extends Application {

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("JavaFX Login");

        // Tạo lưới layout
        GridPane grid = new GridPane();
        grid.setAlignment(Pos.CENTER);
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(25, 25, 25, 25));

        // Các thành phần giao diện
        Label lblUser = new Label("Username:");
        grid.add(lblUser, 0, 1);

        TextField txtUser = new TextField();
        grid.add(txtUser, 1, 1);

        Label lblPass = new Label("Password:");
        grid.add(lblPass, 0, 2);

        PasswordField pwBox = new PasswordField();
        grid.add(pwBox, 1, 2);

        Button btnLogin = new Button("Log in");
        grid.add(btnLogin, 1, 3);
//        btnLogin.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14px; -fx-background-radius: 5px;");
//        btnLogin.setPrefWidth(Double.MAX_VALUE);
//        btnLogin.setPrefHeight(35);

        // Xử lý sự kiện
        btnLogin.setOnAction(e -> {
            if ("admin".equals(txtUser.getText()) && "12345".equals(pwBox.getText())) {
                System.out.println("Đăng nhập thành công!");
            } else {
                System.out.println("Sai tài khoản!");
            }
        });

        Scene scene = new Scene(grid, 800, 500);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
