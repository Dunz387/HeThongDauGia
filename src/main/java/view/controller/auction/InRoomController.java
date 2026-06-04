package view.controller.auction;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.chart.AreaChart;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import model.auction.Auction;
import network.NotificationManager;
import view.utility.auction.BidderAuctionRoomSupport;

import java.net.URL;
import java.util.ResourceBundle;

public class InRoomController implements Initializable {
    @FXML private AreaChart<Number, Number> priceChart;
    @FXML private TextField bidAmountField;
    @FXML private Label topBidderLabel;
    @FXML private Label totalTimeLabel;
    @FXML private Label roomIdLabel;
    @FXML private Label balanceLabel;
    @FXML private Label bidIncrementLabel;
    @FXML private ToggleButton autoBidToggleButton;
    @FXML private TableView<NotificationManager.NotificationItem> notificationTableView;
    @FXML private TableColumn<NotificationManager.NotificationItem, String> colNotifTime;
    @FXML private TableColumn<NotificationManager.NotificationItem, String> colNotifContent;
    @FXML private TableView<BidderAuctionRoomSupport.HistoryItem> historyTableView;
    @FXML private TableColumn<BidderAuctionRoomSupport.HistoryItem, Integer> colHistoryRound;
    @FXML private TableColumn<BidderAuctionRoomSupport.HistoryItem, Double> colHistoryPrice;

    private BidderAuctionRoomSupport roomSupport;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        roomSupport = new BidderAuctionRoomSupport(
                priceChart,
                bidAmountField,
                topBidderLabel,
                totalTimeLabel,
                roomIdLabel,
                balanceLabel,
                bidIncrementLabel,
                autoBidToggleButton,
                notificationTableView,
                colNotifTime,
                colNotifContent,
                historyTableView,
                colHistoryRound,
                colHistoryPrice
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
    private void handlePlaceBid() {
        roomSupport.handlePlaceBid();
    }

    @FXML
    public void exitRoom(ActionEvent event) {
        roomSupport.exitRoom();
    }

    @FXML
    private void handleAutoBidToggle(ActionEvent event) {
        roomSupport.handleAutoBidToggle(event);
    }
}
