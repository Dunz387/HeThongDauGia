package view.AuctionRoomUI;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class SellerInRoomView extends Application {
    @Override
    public void start(Stage primaryStage) throws IOException {
        Parent root = FXMLLoader.load(getClass().getResource("/AuctionRoomUI/SellerInRoomView.fxml"));
        primaryStage.setTitle("In Room");
        primaryStage.setScene(new Scene(root));
        primaryStage.show();
    }

}
