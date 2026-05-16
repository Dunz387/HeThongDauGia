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
        Parent root = FXMLLoader.load(java.util.Objects.requireNonNull(getClass().getResource("/view/AuctionRoomUI/SellerInRoomView.fxml")));
        primaryStage.setTitle("Phòng Đấu Giá - Người Bán");
        primaryStage.setScene(new Scene(root));
        primaryStage.show();
    }

}
