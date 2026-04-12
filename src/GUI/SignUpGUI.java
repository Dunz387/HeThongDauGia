package GUI;

//import javafx.application.Application;
//import javafx.scene.control.Button;
//import javafx.stage.Stage;
//
//public class SignUpGUI extends Application {
//
//    @Override
//    public void start(Stage stage) throws Exception {
//        Button sign_up_button = new Button("Login");
//        Button cancel_button = new Button("Cancel");
//        sign_up_button.setOnAction(event -> {
//
//        })
//    }
//}
import javafx.application.Application;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.stage.Stage;

public class SignUpGUI extends Application {

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("User Registration");

        // 1. Tạo Layout chính (GridPane) để căn chỉnh form
        GridPane grid = new GridPane();
        grid.setAlignment(Pos.CENTER);
        grid.setHgap(10);
        grid.setVgap(15);
        grid.setPadding(new Insets(40, 40, 40, 40));
        grid.setStyle("-fx-background-color: #f4f7f6;"); // Màu nền sáng hiện đại

        // 2. Tiêu đề form
        Text sceneTitle = new Text("Create an Account");
        sceneTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 24));
        sceneTitle.setStyle("-fx-fill: #2c3e50;");
        grid.add(sceneTitle, 0, 0, 2, 1);
        GridPane.setMargin(sceneTitle, new Insets(0, 0, 10, 0));

        // 3. Khởi tạo các trường nhập liệu theo thứ tự yêu cầu
        TextField userTextField = new TextField();
        userTextField.setPromptText("Enter username (request)");
        styleTextField(userTextField);

        TextField nameTextField = new TextField();
        nameTextField.setPromptText("Enter your full name");
        styleTextField(nameTextField);

        TextField emailTextField = new TextField();
        emailTextField.setPromptText("Enter your email");
        styleTextField(emailTextField);

        PasswordField pwBox = new PasswordField();
        pwBox.setPromptText("Enter your password (request)");
        styleTextField(pwBox);

        // Xử lý riêng phần Address (Road - City - Country) trên cùng 1 hàng
        HBox addressBox = new HBox(10);
        TextField roadField = new TextField();
        roadField.setPromptText("Road");
        TextField cityField = new TextField();
        cityField.setPromptText("City");
        TextField countryField = new TextField();
        countryField.setPromptText("Country");

        styleTextField(roadField); styleTextField(cityField); styleTextField(countryField);
        // Chia đều chiều rộng cho 3 trường địa chỉ
        HBox.setHgrow(roadField, Priority.ALWAYS);
        HBox.setHgrow(cityField, Priority.ALWAYS);
        HBox.setHgrow(countryField, Priority.ALWAYS);
        addressBox.getChildren().addAll(roadField, cityField, countryField);

        // Thêm các thành phần vào Grid
        grid.add(new Label("Username:"), 0, 1);
        grid.add(userTextField, 1, 1);

        grid.add(new Label("Full Name:"), 0, 2);
        grid.add(nameTextField, 1, 2);

        grid.add(new Label("Email:"), 0, 3);
        grid.add(emailTextField, 1, 3);

        grid.add(new Label("Password:"), 0, 4);
        grid.add(pwBox, 1, 4);

        grid.add(new Label("Address:"), 0, 5);
        grid.add(addressBox, 1, 5);

        // 4. Nút Sign Up
        Button btnSignUp = new Button("Sign up");
        btnSignUp.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14px; -fx-background-radius: 5px;");
        btnSignUp.setPrefWidth(Double.MAX_VALUE); // Cho nút kéo dài hết chiều rộng
        btnSignUp.setPrefHeight(35);

        // VBox để chứa nút đăng ký và dòng chữ chuyển hướng
        VBox actionBox = new VBox(10);
        actionBox.setAlignment(Pos.CENTER);
        actionBox.setPadding(new Insets(15, 0, 0, 0));

        // 5. Dòng text "Đã có tài khoản?" màu xanh
        Hyperlink loginLink = new Hyperlink("Đã có tài khoản?");
        loginLink.setStyle("-fx-text-fill: #2980b9; -fx-font-size: 12px; -fx-underline: true;");

        actionBox.getChildren().addAll(btnSignUp, loginLink);
        grid.add(actionBox, 0, 6, 2, 1);
        GridPane.setHalignment(actionBox, HPos.CENTER);

        // --- XỬ LÝ SỰ KIỆN (EVENT HANDLING) ---

        // Sự kiện khi nhấn nút Sign up
        btnSignUp.setOnAction(e -> {
            String username = userTextField.getText();
            String password = pwBox.getText();
            String address = roadField.getText() + " - " + cityField.getText() + " - " + countryField.getText();

            // Validate cơ bản (Frontend validation)
            if (username.isEmpty() || password.isEmpty()) {
                showAlert(Alert.AlertType.ERROR, "Lỗi đăng ký", "Vui lòng không để trống Username và Password!");
            } else {
                // TODO: Gọi hàm gọi đến Database hoặc API backend ở đây
                saveUserToDatabase(username, nameTextField.getText(), emailTextField.getText(), password, address);
                showAlert(Alert.AlertType.INFORMATION, "Thành công", "Đăng ký thành công cho tài khoản: " + username);
            }
        });

        // Sự kiện khi nhấn vào link "Đã có tài khoản?"
        loginLink.setOnAction(e -> {
            System.out.println("Chuyển hướng sang giao diện Login...");
            // TODO: Khởi tạo scene Login và setScene(loginScene) tại đây
        });

        // 6. Hiển thị Scene
        Scene scene = new Scene(grid, 800, 500);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    // Hàm tiện ích để làm đẹp các ô nhập liệu
    private void styleTextField(TextField textField) {
        textField.setStyle("-fx-padding: 8px; -fx-background-radius: 5px; -fx-border-color: #bdc3c7; -fx-border-radius: 5px;");
        textField.setPrefHeight(35);
    }

    // Hàm tiện ích để hiển thị thông báo
    private void showAlert(Alert.AlertType alertType, String title, String message) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    // Hàm mô phỏng việc lưu vào Database
    private void saveUserToDatabase(String username, String fullname, String email, String password, String address) {
        // Thực thi mã JDBC (PreparedStatement với câu lệnh INSERT INTO users...)
        // Hoặc gửi dữ liệu tới tầng Controller / Service
        System.out.println("Đang lưu trữ dữ liệu vào Database...");
        System.out.println("Address ghi nhận: " + address);
    }

    public static void main(String[] args) {
        launch(args);
    }
}