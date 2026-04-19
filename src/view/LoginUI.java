package view;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.stage.Stage;

import java.util.HashMap;
import java.util.Map;

public class LoginUI extends Application {

    // Khởi tạo một Mock Database đơn giản bằng Map để kiểm tra logic
    private static final Map<String, String> MOCK_DB = new HashMap<>();
    static {
        MOCK_DB.put("admin", "123456");
        MOCK_DB.put("bqdunz", "password123");
    }

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Log in screen");

        Scene loginScene = createLoginScene(primaryStage);

        primaryStage.setScene(loginScene);
        primaryStage.show();
    }

    private Scene createLoginScene(Stage primaryStage) {
        // Sử dụng GridPane để layout form gọn gàng, chia theo dạng lưới
        GridPane grid = new GridPane();
        grid.setAlignment(Pos.CENTER);
        grid.setHgap(10); // Khoảng cách ngang giữa các cột
        grid.setVgap(10); // Khoảng cách dọc giữa các hàng
        grid.setPadding(new Insets(25, 25, 25, 25));

        // Tiêu đề Form
        Text scenetitle = new Text("Log In");
        scenetitle.setFont(Font.font("Tahoma", FontWeight.NORMAL, 20));
        grid.add(scenetitle, 0, 0, 2, 1);

        // Tên người dùng
        Label userName = new Label("Username:");
        grid.add(userName, 0, 1);
        TextField userTextField = new TextField();
        userTextField.setPromptText("Enter username...");
        grid.add(userTextField, 1, 1);

        // Mật khẩu
        Label pw = new Label("Password:");
        grid.add(pw, 0, 2);
        PasswordField pwBox = new PasswordField();
        pwBox.setPromptText("Enter password...");
        grid.add(pwBox, 1, 2);

        // Nút điều hướng
        Button btnLogin = new Button("Login");
        Button btnCancel = new Button("Cancel");

        // Nhóm các nút vào một HBox và căn lề phải
        HBox hbBtn = new HBox(10);
        hbBtn.setAlignment(Pos.BOTTOM_RIGHT);
        hbBtn.getChildren().addAll(btnCancel, btnLogin);
        grid.add(hbBtn, 1, 4);

        // Vùng Text hiển thị thông báo lỗi/thành công
        Text actiontarget = new Text();
        grid.add(actiontarget, 1, 6);

        // ==========================================
        // XỬ LÝ SỰ KIỆN (EVENT HANDLING)
        // ==========================================

        // Xử lý sự kiện nút Login
        btnLogin.setOnAction(e -> {
            String username = userTextField.getText();
            String password = pwBox.getText();

            // Logic xác thực (Authentication)
            if (MOCK_DB.containsKey(username) && MOCK_DB.get(username).equals(password)) {
                actiontarget.setFill(Color.GREEN);
                actiontarget.setText("Đăng nhập thành công!");

                // Xóa text trên form để bảo mật
                userTextField.clear();
                pwBox.clear();

                // Điều hướng sang Giao diện Trung tâm
                primaryStage.setScene(createMainScene(primaryStage));
            } else {
                actiontarget.setFill(Color.FIREBRICK); // Màu đỏ chuyên dụng cho cảnh báo
                actiontarget.setText("Không tìm thấy người dùng");
            }
        });

        // Xử lý sự kiện nút Cancel
        btnCancel.setOnAction(e -> {
            // Reset lại form và chuyển cảnh
            userTextField.clear();
            pwBox.clear();
            actiontarget.setText("");

            // Điều hướng về Giao diện Sign Up
            primaryStage.setScene(createSignUpScene(primaryStage));
        });

        return new Scene(grid, 400, 300);
    }

    // --- CÁC GIAO DIỆN GIẢ LẬP ĐỂ TEST LUỒNG ĐIỀU HƯỚNG ---

    // Giao diện Trung tâm (Đích đến khi Login thành công)
    private Scene createMainScene(Stage primaryStage) {
        VBox layout = new VBox(20);
        layout.setAlignment(Pos.CENTER);
        Text welcomeText = new Text("Chào mừng đến với Giao diện Trung tâm!");
        welcomeText.setFont(Font.font("Tahoma", FontWeight.BOLD, 18));

        Button btnLogout = new Button("Đăng xuất");
        btnLogout.setOnAction(e -> primaryStage.setScene(createLoginScene(primaryStage)));

        layout.getChildren().addAll(welcomeText, btnLogout);
        return new Scene(layout, 500, 400);
    }

    // Giao diện Sign Up (Đích đến khi ấn Cancel)
    private Scene createSignUpScene(Stage primaryStage) {
        VBox layout = new VBox(20);
        layout.setAlignment(Pos.CENTER);
        Text signupText = new Text("Giao diện Đăng Ký (Sign Up)");
        signupText.setFont(Font.font("Tahoma", FontWeight.BOLD, 18));

        Button btnBackToLogin = new Button("Quay lại Login");
        btnBackToLogin.setOnAction(e -> primaryStage.setScene(createLoginScene(primaryStage)));

        layout.getChildren().addAll(signupText, btnBackToLogin);
        return new Scene(layout, 400, 300);
    }

    public static void main(String[] args) {
        launch(args);
    }
}