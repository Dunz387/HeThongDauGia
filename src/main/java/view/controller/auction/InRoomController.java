package view.controller.auction;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
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
import java.util.logging.Logger;


public class InRoomController implements Initializable {
    private static final Logger LOGGER = Logger.getLogger(InRoomController.class.getName());

    @FXML private javafx.scene.chart.AreaChart<Number, Number> priceChart;
    @FXML private TextField bidAmountField;
    @FXML private Label topBidderLabel;
    @FXML private Label totalTimeLabel;
    @FXML private Label roomIdLabel;
    @FXML private Label balanceLabel;
    @FXML private Label bidIncrementLabel;
    @FXML private javafx.scene.control.ToggleButton autoBidToggleButton;

    // Notifications & History Table
    @FXML private TableView<NotificationManager.NotificationItem> notificationTableView;
    @FXML private TableColumn<NotificationManager.NotificationItem, String> colNotifTime;
    @FXML private TableColumn<NotificationManager.NotificationItem, String> colNotifContent;
    @FXML private TableView<HistoryItem> historyTableView;
    @FXML private TableColumn<HistoryItem, Integer> colHistoryRound;
    @FXML private TableColumn<HistoryItem, Double> colHistoryPrice;

    private javafx.collections.ObservableList<NotificationManager.NotificationItem> roomNotifications = javafx.collections.FXCollections.observableArrayList();
    private javafx.collections.ObservableList<HistoryItem> historyData = javafx.collections.FXCollections.observableArrayList();

    public static class HistoryItem {
        private int round;
        private double price;
        public HistoryItem(int round, double price) { this.round = round; this.price = price; }
        public int getRound() { return round; }
        public double getPrice() { return price; }
    }

    private XYChart.Series<Number, Number> priceSeries;
    private int bidCount = 0;
    private model.auction.Auction auction;
    private String currentAuctionId = null;
    private String currentTopBidder = "Chưa có";
    private double currentHighestPrice = 0;
    private AuctionRoomHelper roomHelper;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        priceSeries = new XYChart.Series<>();
        priceSeries.setName("Diễn biến giá ($)");
        priceChart.getData().add(priceSeries);
        ChartHelper.configureAreaChart(priceChart);

        // Cấu hình bảng lịch sử giá
        colHistoryRound.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("round"));
        colHistoryPrice.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("price"));
        historyTableView.setItems(historyData);

        // Cấu hình bảng thông báo (DRY: dùng WrappingTextCellFactory)
        colNotifTime.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("time"));
        colNotifContent.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("content"));
        colNotifContent.setCellFactory(new WrappingTextCellFactory());
        notificationTableView.setItems(roomNotifications);
        notificationTableView.setFixedCellSize(-1);

        if (topBidderLabel != null) topBidderLabel.setText(currentTopBidder);
        if (bidAmountField != null) bidAmountField.setOnAction(event -> handlePlaceBid());

        registerNetworkListeners();
    }

    public void setAuction(model.auction.Auction auction) {
        this.auction = auction;
        this.currentAuctionId = auction.getId();
        if (roomIdLabel != null) roomIdLabel.setText("ID phòng: " + currentAuctionId);

        // Khởi tạo helper (DRY: timer, exit, common listeners)
        roomHelper = new AuctionRoomHelper(currentAuctionId);
        roomHelper.setOnTimeUpdate(() -> updateTimeLabel());
        roomHelper.setOnTimerExpired(() -> {});

        if (auction.getEndTime() != null) {
            roomHelper.initTimer(auction.getEndTime());
            Platform.runLater(() -> {
                roomHelper.startTimer();
                updateTimeLabel();
            });
        }

        // Đăng ký common listeners qua helper
        roomHelper.registerTimeExtendedListener(() -> {
            updateTimeLabel();
            AlertHelper.showInfo("Gia hạn", "Có người đặt giá phút chót! Thời gian cộng thêm");
        });
        roomHelper.registerParticipantsListener(count ->
            System.out.println("👥 [Phòng " + currentAuctionId + "] Số người đang xem: " + count));
        roomHelper.registerRoomKickedListener(() -> exitRoom(null));

        // Khôi phục lịch sử
        Platform.runLater(() -> {
            priceChart.setAnimated(false);
            priceSeries.getData().clear();
            historyData.clear();
            roomNotifications.clear();

            java.util.List<model.auction.BidTransaction> history = auction.getBidHistory();
            this.bidCount = 0;
            priceSeries.getData().add(new XYChart.Data<>(0, auction.getStartingPrice()));

            if (history != null && !history.isEmpty()) {
                for (model.auction.BidTransaction tx : history) {
                    this.bidCount++;
                    priceSeries.getData().add(new XYChart.Data<>(bidCount, tx.getBidAmount()));
                    historyData.add(0, new HistoryItem(bidCount, tx.getBidAmount()));
                    String timeStr = tx.getTimestamp().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss"));
                    roomNotifications.add(0, new NotificationManager.NotificationItem(
                        "📢 [" + auction.getItem().getName() + "] - Lượt #" + bidCount + ": " + tx.getBidder().getUsername() + " đã đặt $" + tx.getBidAmount(), timeStr));
                }
            }

            if (auction.getHighestBidder() != null) {
                updateTopBidder(auction.getHighestBidder().getUsername());
                currentHighestPrice = auction.getCurrentPrice();
            } else {
                currentHighestPrice = auction.getStartingPrice();
                updateTopBidder("Chưa có");
            }

            ChartHelper.updateXAxisBounds(priceChart, bidCount);
            updateIncrementDisplay(currentHighestPrice);
        });

        updateBalanceDisplay(network.SessionManager.getInstance().getBalance());

        if (!ClientNetworkManager.getInstance().sendData(Protocol.REQ_JOIN_ROOM + Protocol.DELIMITER + currentAuctionId)) {
            LOGGER.warning("❌ Không thể tham gia phòng: Lỗi kết nối mạng.");
        }
    }

    private void registerNetworkListeners() {
        ClientNetworkManager.getInstance().registerListener(Protocol.BROADCAST_AUCTION_START, (message) -> {
            String[] parts = message.split(Protocol.DELIMITER);
            if (parts.length >= 3 && java.util.Objects.equals(parts[1], currentAuctionId)) {
                int durationMinutes = Integer.parseInt(parts[2]);
                if (roomHelper != null) {
                    roomHelper.initTimer(java.time.LocalDateTime.now().plusMinutes(durationMinutes));
                    Platform.runLater(() -> roomHelper.startTimer());
                }
                NotificationManager.getInstance().addNotification("🚀 Phiên đấu giá " + currentAuctionId + " đã BẮT ĐẦU!");
            }
        });

        ClientNetworkManager.getInstance().registerListener(Protocol.BROADCAST_NEW_BID, (message) -> {
            String[] parts = message.split(Protocol.DELIMITER);
            if (parts.length >= 4 && java.util.Objects.equals(parts[1], currentAuctionId)) {
                double newPrice = Double.parseDouble(parts[2]);
                String topBidder = parts[3];

                if (newPrice > currentHighestPrice) {
                    currentHighestPrice = newPrice;
                    updateTopBidder(topBidder);
                    updateIncrementDisplay(newPrice);
                    bidCount++;
                    Platform.runLater(() -> {
                        ChartHelper.updateXAxisBounds(priceChart, bidCount);
                        priceSeries.getData().add(new XYChart.Data<>(bidCount, newPrice));
                        historyData.add(0, new HistoryItem(bidCount, newPrice));
                        String timeStr = java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss"));
                        roomNotifications.add(0, new NotificationManager.NotificationItem(
                            "📢 [" + auction.getItem().getName() + "] - Lượt #" + bidCount + ": " + topBidder + " đặt $" + String.format("%,.0f", newPrice), timeStr));
                    });
                    NotificationManager.getInstance().addNotification("📢 [" + auction.getItem().getName() + "] - Lượt #" + bidCount + ": " + topBidder + " vừa đặt $" + newPrice);
                }
            }
        });

        ClientNetworkManager.getInstance().registerListener(Protocol.BROADCAST_AUCTION_FINISHED, (message) -> {
            String[] parts = message.split(Protocol.DELIMITER);
            if (parts.length >= 2 && java.util.Objects.equals(parts[1], currentAuctionId)) {
                String finalWinner = parts.length > 2 ? parts[2] : "Không có";
                double finalPrice = parts.length > 3 ? Double.parseDouble(parts[3]) : 0;
                Platform.runLater(() -> {
                    if (roomHelper != null) roomHelper.stopTimer();
                    updateTopBidder(finalWinner);
                    String timeStr = java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss"));
                    roomNotifications.add(0, new NotificationManager.NotificationItem(
                        "🏁 PHIÊN ĐẤU GIÁ KẾT THÚC! Người thắng: " + finalWinner + " ($" + String.format("%,.0f", finalPrice) + ")", timeStr));
                    AlertHelper.showInfo("Kết quả đấu giá", "Người chiến thắng: " + finalWinner + "\nGiá cuối: $" + String.format("%,.0f", finalPrice));
                    NotificationManager.getInstance().addNotification("🏆 Phiên đấu giá KẾT THÚC! Người thắng: " + finalWinner);
                    if (!network.SessionManager.getInstance().isAdmin()) exitRoom(null);
                });
            }
        });

        ClientNetworkManager.getInstance().registerListener(Protocol.REQ_BID, (response) -> {
            String[] parts = response.split(Protocol.DELIMITER);
            Platform.runLater(() -> {
                if (!(parts.length > 1 && parts[1].equals(Protocol.RES_SUCCESS))) {
                    AlertHelper.showWarning("Đặt giá thất bại", parts.length >= 3 ? parts[2] : "Lỗi không xác định");
                }
            });
        });

        ClientNetworkManager.getInstance().registerListener(Protocol.REQ_AUTOBID, (response) -> {
            String[] parts = response.split(Protocol.DELIMITER);
            Platform.runLater(() -> {
                if (parts.length > 1 && parts[1].equals(Protocol.RES_SUCCESS)) {
                    boolean cancelled = parts.length > 2 && parts[2].equals("CANCEL");
                    AlertHelper.showInfo("Auto-bid", cancelled ? "Đã tắt tính năng đặt giá tự động." : "Đã bật tính năng đặt giá tự động thành công!");
                    if (autoBidToggleButton != null) {
                        autoBidToggleButton.setSelected(!cancelled);
                        autoBidToggleButton.setText(cancelled ? "Tắt" : "Bật");
                    }
                } else {
                    AlertHelper.showWarning("Auto-bid thất bại", parts.length >= 3 ? parts[2] : "Lỗi không xác định");
                    if (autoBidToggleButton != null) { autoBidToggleButton.setSelected(false); autoBidToggleButton.setText("Tắt"); }
                }
            });
        });

        network.SessionManager.getInstance().balanceProperty().addListener((obs, oldVal, newVal) ->
            updateBalanceDisplay(newVal.doubleValue()));
    }

    private void updateTimeLabel() {
        Platform.runLater(() -> {
            if (totalTimeLabel != null && roomHelper != null)
                totalTimeLabel.setText(ChartHelper.formatTime(roomHelper.getTotalTimeRemaining()));
        });
    }

    private void updateTopBidder(String bidderName) {
        currentTopBidder = bidderName;
        Platform.runLater(() -> { if (topBidderLabel != null) topBidderLabel.setText(bidderName); });
    }

    private void updateBalanceDisplay(double balance) {
        Platform.runLater(() -> { if (balanceLabel != null) balanceLabel.setText(String.format("%,.0f $", balance)); });
    }

    private void updateIncrementDisplay(double currentPrice) {
        double roundedIncrement = ChartHelper.calculateMinIncrement(currentPrice);
        Platform.runLater(() -> {
            if (bidIncrementLabel != null) bidIncrementLabel.setText(String.format("%,.0f $", roundedIncrement));
            if (bidAmountField != null) bidAmountField.setPromptText("Tối thiểu: " + String.format("%,.0f", currentPrice + roundedIncrement));
        });
    }

    @FXML
    private void handlePlaceBid() {
        try {
            String bidText = bidAmountField.getText().trim();
            if (view.utility.ValidationHelper.isEmpty(bidText)) return;
            if (currentAuctionId == null) { AlertHelper.showWarning("Lỗi", "Chưa xác định được phòng đấu giá!"); return; }
            if (!view.utility.ValidationHelper.isValidStartPrice(bidText)) { AlertHelper.showWarning("Lỗi dữ liệu", "Vui lòng nhập số tiền hợp lệ và lớn hơn 0!"); return; }

            double amount = Double.parseDouble(bidText);
            if (!ClientNetworkManager.getInstance().sendData(Protocol.REQ_BID + Protocol.DELIMITER + currentAuctionId + Protocol.DELIMITER + amount)) {
                AlertHelper.showError("Lỗi kết nối", "Không thể đặt giá. Vui lòng kiểm tra kết nối mạng!");
            }
            bidAmountField.clear();
        } catch (NumberFormatException e) {
            AlertHelper.showWarning("Lỗi", "Vui lòng nhập số tiền hợp lệ!");
        }
    }

    @FXML
    public void exitRoom(ActionEvent event) {
        if (roomHelper != null) {
            Stage stage = (Stage) priceChart.getScene().getWindow();
            roomHelper.exitRoom(stage,
                Protocol.BROADCAST_NEW_BID, Protocol.BROADCAST_ROUND_FINISHED,
                Protocol.BROADCAST_TIME_EXTENDED, Protocol.BROADCAST_AUCTION_FINISHED,
                Protocol.REQ_BID, Protocol.REQ_AUTOBID, Protocol.RES_UPDATE_BALANCE,
                Protocol.BROADCAST_PARTICIPANTS, Protocol.BROADCAST_ROOM_KICKED);
        }
    }

    @FXML
    private void handleAutoBidToggle(ActionEvent event) {
        javafx.scene.control.ToggleButton btn = (javafx.scene.control.ToggleButton) event.getSource();
        if (btn.isSelected()) {
            btn.setText("Bật");
            javafx.scene.control.TextInputDialog dialog = new javafx.scene.control.TextInputDialog();
            dialog.setTitle("Cấu hình Auto-Bid");
            dialog.setHeaderText("Nhập giá tối đa bạn sẵn sàng trả (Max Bid)\nBước nhảy giá sẽ được tự động tính toán tối ưu theo thời gian thực (10%)");
            dialog.setContentText("Giá tối đa ($):");

            java.util.Optional<String> result = dialog.showAndWait();
            if (result.isPresent()) {
                try {
                    double maxBid = Double.parseDouble(result.get().trim());
                    if (maxBid <= 0) { AlertHelper.showWarning("Lỗi", "Vui lòng nhập số lớn hơn 0!"); }
                    else {
                        if (!ClientNetworkManager.getInstance().sendData(Protocol.REQ_AUTOBID + Protocol.DELIMITER + currentAuctionId + Protocol.DELIMITER + maxBid)) {
                            AlertHelper.showError("Lỗi kết nối", "Không thể đăng ký auto-bid!");
                            btn.setSelected(false); btn.setText("Tắt");
                        }
                        return;
                    }
                } catch (NumberFormatException e) { AlertHelper.showWarning("Lỗi", "Vui lòng nhập số hợp lệ!"); }
            }
            btn.setSelected(false); btn.setText("Tắt");
        } else {
            btn.setText("Tắt");
            if (!ClientNetworkManager.getInstance().sendData(Protocol.REQ_AUTOBID + Protocol.DELIMITER + currentAuctionId + Protocol.DELIMITER + "CANCEL")) {
                AlertHelper.showError("Lỗi kết nối", "Không thể hủy đăng ký auto-bid!");
                btn.setSelected(true); btn.setText("Bật");
            }
        }
    }
}
