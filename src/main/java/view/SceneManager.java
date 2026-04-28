package view;

import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class SceneManager {

    /**
     * @param stage    Cửa sổ hiện tại
     * @param fxmlPath Đường dẫn tuyệt đối đến file FXML mới
     * @param title    Tiêu đề mới cho cửa sổ
     */
    public static void switchScene(Stage stage, String fxmlPath, String title) {
        try {
            FXMLLoader loader = new FXMLLoader(SceneManager.class.getResource(fxmlPath));
            Parent root = loader.load();

            Scene scene = new Scene(root);
            stage.setScene(scene);

            if (title != null && !title.trim().isEmpty()) {
                stage.setTitle(title);
            }

            stage.show();

        } catch (IOException e) {
            System.err.println("Không thể tải màn hình từ đường dẫn: " + fxmlPath);
            e.printStackTrace();
        }
    }

    /**
     * @param event    Sự kiện click để lấy ra cửa sổ hiện tại
     * @param fxmlPath Đường dẫn tuyệt đối đến file FXML mới
     * @param title    Tiêu đề mới cho cửa sổ
     */
    public static void switchScene(ActionEvent event, String fxmlPath, String title) {
        // Lấy Stage từ sự kiện của Nút bấm
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        
        // Gọi lại Phương thức 1 ở trên để thực hiện việc chuyển cảnh
        switchScene(stage, fxmlPath, title);
    }
}