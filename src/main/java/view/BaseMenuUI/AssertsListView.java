package view.BaseMenuUI;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;


public class AssertsListView extends Application {
    @Override
    public void start(Stage AssertsListViewStage) throws Exception {
        Parent root = FXMLLoader.load(getClass().getResource("/view/BaseMenuUI/AssertsList.fxml"));
        Scene scene = new Scene(root);

        AssertsListViewStage.setTitle("Asserts List View");
        AssertsListViewStage.setScene(scene);
        AssertsListViewStage.show();

    }
    public static void main(String[] args) {
        launch(args);
    }

}
