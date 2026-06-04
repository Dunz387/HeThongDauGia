package view.controller.auction;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.chart.AreaChart;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import model.auction.Auction;
import network.NotificationManager;
import view.utility.auction.SellerAuctionRoomSupport;

import java.net.URL;
import java.util.ResourceBundle;

public class SellerInRoomController implements Initializable {
    @FXML private Label lblRoomId;
    @FXML private Label totalTimeLabel;
    @FXML private Label lblCurrentPrice;
    @FXML private Label lblMinStep;
    @FXML private AreaChart<Number, Number> priceChart;
    @FXML private TableView<double[]> tableBidHistory;
    @FXML private TableColumn<double[], Integer> colRound;
    @FXML private TableColumn<double[], Double> colBidPrice;
    @FXML private Label lblEarnings;
    @FXML private Label lblParticipants;
    @FXML private Label lblRounds;
    @FXML private Label topBidderLabel;
    @FXML private TableView<NotificationManager.NotificationItem> notificationTableView;
    @FXML private TableColumn<NotificationManager.NotificationItem, String> colNotifTime;
    @FXML private TableColumn<NotificationManager.NotificationItem, String> colNotifContent;

    private SellerAuctionRoomSupport roomSupport;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        roomSupport = new SellerAuctionRoomSupport(
                lblRoomId,
                totalTimeLabel,
                lblCurrentPrice,
                lblMinStep,
                priceChart,
                tableBidHistory,
                colRound,
                colBidPrice,
                lblEarnings,
                lblParticipants,
                lblRounds,
                topBidderLabel,
                notificationTableView,
                colNotifTime,
                colNotifContent
        );
        roomSupport.initialize();
    }

    public void setAuction(Auction auction) {
        roomSupport.setAuction(auction);
    }

    public void cleanupRoom() {
        roomSupport.cleanupRoom();
    }

    @FXML
    public void exitRoom(ActionEvent event) {
        roomSupport.exitRoom(event);
    }
}
