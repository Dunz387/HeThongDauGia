package view.controller.auction;

import javafx.application.Platform;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.stage.Stage;
import network.ClientNetworkManager;
import network.NotificationManager;
import shared.Protocol;
import view.utility.AlertHelper;
import view.utility.AuctionRoomHelper;
import view.utility.ChartHelper;
import view.utility.WrappingTextCellFactory;

import java.net.URL;
import java.util.ResourceBundle;

/**
 * Controller cho màn hình Seller theo dõi phiên đấu giá của mình.
 * Chỉ xem, không đặt giá.
 */
public class SellerInRoomController implements Initializable {
    @FXML private Label lblRoomId;
    @FXML private Label totalTimeLabel;
    @FXML private Label lblCurrentPrice;
    @FXML private Label lblMinStep;
    @FXML private javafx.scene.chart.AreaChart<Number, Number> priceChart;
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

    private XYChart.Series<Number, Number> priceSeries;
    private ObservableList<double[]> bidHistory = FXCollections.observableArrayList();
    private ObservableList<NotificationManager.NotificationItem> roomNotifications = FXCollections.observableArrayList();
    private int bidCount = 0;
    private model.auction.Auction auction;
    private String currentAuctionId = null;
    private double currentHighestPrice = 0;
    private AuctionRoomHelper roomHelper;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        priceSeries = new XYChart.Series<>();
        priceSeries.setName("Biến động giá ($)");
        priceChart.getData().add(priceSeries);
        ChartHelper.configureAreaChart(priceChart);

        // Bảng lịch sử giá
        colRound.setCellValueFactory(cd -> new SimpleIntegerProperty((int) cd.getValue()[0]).asObject());
        colBidPrice.setCellValueFactory(cd -> new SimpleDoubleProperty(cd.getValue()[1]).asObject());
        tableBidHistory.setItems(bidHistory);

        // Bảng thông báo (DRY: WrappingTextCellFactory)
        colNotifTime.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("time"));
        colNotifContent.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("content"));
        colNotifContent.setCellFactory(new WrappingTextCellFactory());
        notificationTableView.setItems(roomNotifications);

        registerNetworkListeners();

        // Nút Kick User cho Admin
        if (network.SessionManager.getInstance().isAdmin()) {
            javafx.scene.control.Button btnKick = new javafx.scene.control.Button("Đuổi người dùng");
            btnKick.getStyleClass().setAll("btn-danger");
            btnKick.setStyle("-fx-font-weight: bold; -fx-padding: 5 15;");
            btnKick.setOnAction(e -> handleKickUser());
            Platform.runLater(() -> {
                if (lblRoomId != null && lblRoomId.getParent() instanceof javafx.scene.layout.VBox) {
                    ((javafx.scene.layout.VBox) lblRoomId.getParent()).getChildren().add(btnKick);
                }
            });
        }
    }

    private void handleKickUser() {
        javafx.scene.control.TextInputDialog dialog = new javafx.scene.control.TextInputDialog();
        dialog.setTitle("Đuổi người dùng");
        dialog.setHeaderText("Đuổi người dùng khỏi phòng đấu giá");
        dialog.setContentText("Nhập username:");
        dialog.showAndWait().ifPresent(username -> {
            if (!username.trim().isEmpty()) {
                ClientNetworkManager.getInstance().sendData(Protocol.REQ_KICK_USER + Protocol.DELIMITER + currentAuctionId + Protocol.DELIMITER + username.trim());
                AlertHelper.showInfo("Hệ thống", "Đã gửi yêu cầu đuổi " + username.trim());
            }
        });
    }

    private void registerNetworkListeners() {
        ClientNetworkManager.getInstance().registerListener(Protocol.BROADCAST_NEW_BID, (message) -> {
            String[] parts = message.split(Protocol.DELIMITER);
            if (parts.length >= 4 && java.util.Objects.equals(parts[1], currentAuctionId)) {
                double newPrice = Double.parseDouble(parts[2]);
                String topBidder = parts[3];
                currentHighestPrice = newPrice;

                Platform.runLater(() -> {
                    lblCurrentPrice.setText(String.format("%.0f $", newPrice));
                    lblEarnings.setText(String.format("%.0f $", newPrice));
                    topBidderLabel.setText(topBidder);
                    bidCount++;
                    ChartHelper.updateXAxisBounds(priceChart, bidCount);
                    priceChart.setAnimated(true);
                    priceSeries.getData().add(new XYChart.Data<>(bidCount, newPrice));
                    bidHistory.add(new double[]{bidCount, newPrice});
                    lblRounds.setText(String.valueOf(bidCount));
                    updateIncrementDisplay(newPrice);
                    if (auction != null) {
                        String msg = "📢 [" + auction.getItem().getName() + "] - Lượt #" + bidCount + ": " + topBidder + " vừa đặt $" + String.format("%,.0f", newPrice);
                        String timeStr = java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss"));
                        roomNotifications.add(0, new NotificationManager.NotificationItem(msg, timeStr));
                        NotificationManager.getInstance().addNotification(msg);
                    }
                });
            }
        });

        ClientNetworkManager.getInstance().registerListener(Protocol.BROADCAST_AUCTION_FINISHED, (message) -> {
            String[] parts = message.split(Protocol.DELIMITER);
            if (parts.length >= 2 && java.util.Objects.equals(parts[1], currentAuctionId)) {
                String winner = parts.length > 2 ? parts[2] : "Không có";
                double finalPrice = parts.length > 3 ? Double.parseDouble(parts[3]) : 0;
                Platform.runLater(() -> {
                    if (roomHelper != null) roomHelper.stopTimer();
                    lblCurrentPrice.setText(String.format("%,.0f $", finalPrice));
                    topBidderLabel.setText(winner);
                    String timeStr = java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss"));
                    roomNotifications.add(0, new NotificationManager.NotificationItem(
                        "🏁 PHIÊN ĐẤU GIÁ KẾT THÚC! Người thắng: " + winner + " ($" + String.format("%,.0f", finalPrice) + ")", timeStr));
                    AlertHelper.showInfo("Phiên đấu giá kết thúc!", "Người chiến thắng: " + winner + "\nGiá cuối cùng: " + String.format("%,.0f $", finalPrice));
                    if (!network.SessionManager.getInstance().isAdmin()) exitRoom(null);
                });
            }
        });
    }

    public void setAuction(model.auction.Auction auction) {
        this.auction = auction;
        this.currentAuctionId = auction.getId();
        if (lblRoomId != null) lblRoomId.setText("ID phòng: " + auction.getId());

        // Khởi tạo helper (DRY)
        roomHelper = new AuctionRoomHelper(currentAuctionId);
        roomHelper.setOnTimeUpdate(() -> {
            if (totalTimeLabel != null) totalTimeLabel.setText(ChartHelper.formatTime(roomHelper.getTotalTimeRemaining()));
        });

        if (auction.getEndTime() != null) {
            roomHelper.initTimer(auction.getEndTime());
            Platform.runLater(() -> {
                roomHelper.startTimer();
                if (totalTimeLabel != null) totalTimeLabel.setText(ChartHelper.formatTime(roomHelper.getTotalTimeRemaining()));
            });
        }

        // Common listeners via helper
        roomHelper.registerTimeExtendedListener(() -> {
            if (totalTimeLabel != null) totalTimeLabel.setText(ChartHelper.formatTime(roomHelper.getTotalTimeRemaining()));
            String timeStr = java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss"));
            roomNotifications.add(0, new NotificationManager.NotificationItem("⏳ Thời gian được gia hạn", timeStr));
        });
        roomHelper.registerParticipantsListener(count -> { if (lblParticipants != null) lblParticipants.setText(count + " người"); });
        roomHelper.registerRoomKickedListener(() -> exitRoom(null));

        ClientNetworkManager.getInstance().sendData(Protocol.REQ_JOIN_ROOM + Protocol.DELIMITER + currentAuctionId);

        // Khôi phục dữ liệu
        Platform.runLater(() -> {
            priceChart.setAnimated(false);
            priceSeries.getData().clear();
            bidHistory.clear();
            priceSeries.getData().add(new XYChart.Data<>(0, auction.getStartingPrice()));

            java.util.List<model.auction.BidTransaction> history = auction.getBidHistory();
            this.bidCount = 0;
            if (history != null) {
                for (model.auction.BidTransaction tx : history) {
                    this.bidCount++;
                    priceSeries.getData().add(new XYChart.Data<>(bidCount, tx.getBidAmount()));
                    bidHistory.add(new double[]{bidCount, tx.getBidAmount()});
                }
            }

            if (auction.getHighestBidder() != null) {
                currentHighestPrice = auction.getCurrentPrice();
                if (lblCurrentPrice != null) lblCurrentPrice.setText(String.format("%,.0f $", currentHighestPrice));
                if (lblEarnings != null) lblEarnings.setText(String.format("%,.0f $", currentHighestPrice));
                if (topBidderLabel != null) topBidderLabel.setText(auction.getHighestBidder().getUsername());
                if (lblRounds != null) lblRounds.setText(String.valueOf(bidCount));
            } else {
                currentHighestPrice = auction.getStartingPrice();
            }

            roomNotifications.clear();
            if (history != null) {
                int tempCount = 0;
                for (model.auction.BidTransaction tx : history) {
                    tempCount++;
                    String timeStr = tx.getTimestamp().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss"));
                    roomNotifications.add(0, new NotificationManager.NotificationItem(
                        "📢 [" + auction.getItem().getName() + "] - Lượt #" + tempCount + ": " + tx.getBidder().getUsername() + " đã đặt $" + String.format("%,.0f", tx.getBidAmount()), timeStr));
                }
            }

            ChartHelper.updateXAxisBounds(priceChart, bidCount);
            updateIncrementDisplay(currentHighestPrice);
        });
    }

    @FXML
    public void exitRoom(ActionEvent event) {
        if (roomHelper != null) {
            Stage stage = (Stage) priceChart.getScene().getWindow();
            roomHelper.exitRoom(stage,
                Protocol.BROADCAST_NEW_BID, Protocol.BROADCAST_TIME_EXTENDED,
                Protocol.BROADCAST_AUCTION_FINISHED, Protocol.BROADCAST_PARTICIPANTS,
                Protocol.BROADCAST_ROOM_KICKED);
        }
    }

    private void updateIncrementDisplay(double currentPrice) {
        double roundedIncrement = ChartHelper.calculateMinIncrement(currentPrice);
        Platform.runLater(() -> { if (lblMinStep != null) lblMinStep.setText(String.format("%,.0f $", roundedIncrement)); });
    }
}
