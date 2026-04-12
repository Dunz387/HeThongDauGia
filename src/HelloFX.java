import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

public class HelloFX extends Application {

    // Phương thức start là điểm bắt đầu của giao diện JavaFX
    @Override
    public void start(Stage primaryStage) {
        
        // 1. Tạo một Node (Nút bấm)
        Button btn = new Button();
        btn.setText("Nhấn vào tôi!");
        
        // Thêm sự kiện khi nhấn nút
        btn.setOnAction(event -> {
            System.out.println("Hello World! Chào mừng đến với JavaFX!");
        });

        // 2. Tạo một Layout (Bố cục) để chứa nút bấm
        // StackPane sẽ canh giữa tất cả các phần tử bên trong nó
        StackPane root = new StackPane();
        root.getChildren().add(btn);

        // 3. Tạo một Scene (Cảnh) chứa Bố cục trên, kèm theo kích thước (rộng x cao)
        Scene scene = new Scene(root, 600, 500);

        // 4. Thiết lập Stage (Cửa sổ chính)
        primaryStage.setTitle("Ứng dụng JavaFX đầu tiên"); // Tiêu đề cửa sổ
        primaryStage.setScene(scene); // Đưa Scene vào Stage
        primaryStage.show(); // Hiển thị cửa sổ
    }

    // Hàm main dùng để khởi chạy ứng dụng
    public static void main(String[] args) {
        launch(args);
    }
}