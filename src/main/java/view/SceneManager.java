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
     * Phương thức dùng chung để chuyển đổi màn hình (Scene)
     *
     * @param event    Sự kiện click (từ Button, Hyperlink,...) để lấy ra cửa sổ hiện tại
     * @param fxmlPath Đường dẫn tuyệt đối đến file FXML mới
     * @param title    Tiêu đề mới cho cửa sổ (có thể truyền null nếu không muốn đổi)
     */
    public static void switchScene(ActionEvent event, String fxmlPath, String title) {
        try {
            FXMLLoader loader = new FXMLLoader(SceneManager.class.getResource(fxmlPath));
            Parent root = loader.load();

            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();

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
}