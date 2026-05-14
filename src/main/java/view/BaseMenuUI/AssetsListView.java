package view.BaseMenuUI;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;


public class AssetsListView extends Application {
    @Override
    public void start(Stage AssetsListViewStage) throws Exception {
        Parent root = FXMLLoader.load(getClass().getResource("/view/BaseMenuUI/AssetsList.fxml"));
        Scene scene = new Scene(root);

        AssetsListViewStage.setTitle("Assets List View");
        AssetsListViewStage.setScene(scene);
        AssetsListViewStage.show();

    }
    public static void main(String[] args) {
        launch(args);
    }

}
