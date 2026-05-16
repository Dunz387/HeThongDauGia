package view.BaseMenuUI;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class BaseMenu extends Application {
    @Override
    public void start(Stage BaseMenuStage) throws Exception {
        Parent root = FXMLLoader.load(java.util.Objects.requireNonNull(getClass().getResource("/view/BaseMenuUI/BaseMenu.fxml")));
        Scene scene = new Scene(root, 1050, 680);

        BaseMenuStage.setTitle("Base Menu");
        BaseMenuStage.setMinWidth(800);
        BaseMenuStage.setMinHeight(500);
        BaseMenuStage.setScene(scene);
        BaseMenuStage.show();

    }
    public static void main(String[] args) {
        launch(args);
    }

}
