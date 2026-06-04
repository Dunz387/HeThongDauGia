package view.utility.auction;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.scene.chart.AreaChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;
import model.auction.Auction;
import model.auction.BidTransaction;
import network.ClientNetworkManager;
import network.NotificationManager;
import network.SessionManager;
import shared.Protocol;
import view.utility.display.AlertHelper;
import view.utility.display.ChartHelper;
import view.utility.table.WrappingTextCellFactory;
import view.utility.validation.ValidationHelper;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Logger;

public class BidderAuctionRoomSupport {
    private static final Logger LOGGER = Logger.getLogger(BidderAuctionRoomSupport.class.getName());
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final AreaChart<Number, Number> priceChart;
    private final TextField bidAmountField;
    private final Label topBidderLabel;
    private final Label totalTimeLabel;
    private final Label roomIdLabel;
    private final Label balanceLabel;
    private final Label bidIncrementLabel;
    private final ToggleButton autoBidToggleButton;
    private final TableView<NotificationManager.NotificationItem> notificationTableView;
    private final TableColumn<NotificationManager.NotificationItem, String> colNotifTime;
    private final TableColumn<NotificationManager.NotificationItem, String> colNotifContent;
    private final TableView<HistoryItem> historyTableView;
    private final TableColumn<HistoryItem, Integer> colHistoryRound;
    private final TableColumn<HistoryItem, Double> colHistoryPrice;
    private final AuctionRoomCommandService commandService = new AuctionRoomCommandService();

    private final ObservableList<NotificationManager.NotificationItem> roomNotifications = FXCollections.observableArrayList();
    private final ObservableList<HistoryItem> historyData = FXCollections.observableArrayList();

    private XYChart.Series<Number, Number> priceSeries;
    private int bidCount = 0;
    private Auction auction;
    private String currentAuctionId;
    private String currentTopBidder = "Chưa có";
    private double currentHighestPrice = 0;
    private AuctionRoomHelper roomHelper;
    private boolean isExiting = false;

    public BidderAuctionRoomSupport(
            AreaChart<Number, Number> priceChart,
            TextField bidAmountField,
            Label topBidderLabel,
            Label totalTimeLabel,
            Label roomIdLabel,
            Label balanceLabel,
            Label bidIncrementLabel,
            ToggleButton autoBidToggleButton,
            TableView<NotificationManager.NotificationItem> notificationTableView,
            TableColumn<NotificationManager.NotificationItem, String> colNotifTime,
            TableColumn<NotificationManager.NotificationItem, String> colNotifContent,
            TableView<HistoryItem> historyTableView,
            TableColumn<HistoryItem, Integer> colHistoryRound,
            TableColumn<HistoryItem, Double> colHistoryPrice
    ) {
        this.priceChart = priceChart;
        this.bidAmountField = bidAmountField;
        this.topBidderLabel = topBidderLabel;
        this.totalTimeLabel = totalTimeLabel;
        this.roomIdLabel = roomIdLabel;
        this.balanceLabel = balanceLabel;
        this.bidIncrementLabel = bidIncrementLabel;
        this.autoBidToggleButton = autoBidToggleButton;
        this.notificationTableView = notificationTableView;
        this.colNotifTime = colNotifTime;
        this.colNotifContent = colNotifContent;
        this.historyTableView = historyTableView;
        this.colHistoryRound = colHistoryRound;
        this.colHistoryPrice = colHistoryPrice;
    }

    public void initialize() {
        configureChart();
        configureHistoryTable();
        configureNotificationTable();
        updateTopBidder(currentTopBidder);
        if (bidAmountField != null) {
            bidAmountField.setOnAction(event -> handlePlaceBid());
        }
    }

    public void setAuction(Auction auction) {
        this.auction = auction;
        this.currentAuctionId = auction.getId();
        if (roomIdLabel != null) {
            roomIdLabel.setText("ID phòng: " + currentAuctionId);
        }

        resetRoomHelper();
        registerNetworkListeners();
        restoreAuctionState();
        updateBalanceDisplay(SessionManager.getInstance().getBalance());

        if (!commandService.joinRoom(currentAuctionId)) {
            LOGGER.warning("Cannot join room: network error.");
        }
    }

    public void handlePlaceBid() {
        try {
            String bidText = bidAmountField.getText().trim();
            if (ValidationHelper.isEmpty(bidText)) {
                return;
            }
            if (currentAuctionId == null) {
                AlertHelper.showWarning("Lỗi", "Chưa xác định được phòng đấu giá!");
                return;
            }
            if (!ValidationHelper.isValidStartPrice(bidText)) {
                AlertHelper.showWarning("Lỗi dữ liệu", "Vui lòng nhập số tiền hợp lệ và lớn hơn 0!");
                return;
            }

            double amount = Double.parseDouble(bidText);
            if (!commandService.placeBid(currentAuctionId, amount)) {
                AlertHelper.showError("Lỗi kết nối", "Không thể đặt giá. Vui lòng kiểm tra kết nối mạng!");
            }
            bidAmountField.clear();
        } catch (NumberFormatException e) {
            AlertHelper.showWarning("Lỗi", "Vui lòng nhập số tiền hợp lệ!");
        }
    }

    public void handleAutoBidToggle(ActionEvent event) {
        ToggleButton button = (ToggleButton) event.getSource();
        if (button.isSelected()) {
            enableAutoBid(button);
        } else {
            cancelAutoBid(button);
        }
    }

    public void cleanupRoom() {
        if (roomHelper != null) {
            roomHelper.cleanup();
        }
    }

    public void exitRoom() {
        if (isExiting) {
            return;
        }
        isExiting = true;
        if (roomHelper != null) {
            roomHelper.exitRoom(getCurrentStage());
        }
    }

    private void configureChart() {
        priceSeries = new XYChart.Series<>();
        priceSeries.setName("Diễn biến giá ($)");
        priceChart.getData().add(priceSeries);
        ChartHelper.configureAreaChart(priceChart);
    }

    private void configureHistoryTable() {
        colHistoryRound.setCellValueFactory(new PropertyValueFactory<>("round"));
        colHistoryPrice.setCellValueFactory(new PropertyValueFactory<>("price"));
        historyTableView.setItems(historyData);
    }

    private void configureNotificationTable() {
        colNotifTime.setCellValueFactory(new PropertyValueFactory<>("time"));
        colNotifContent.setCellValueFactory(new PropertyValueFactory<>("content"));
        colNotifContent.setCellFactory(new WrappingTextCellFactory());
        notificationTableView.setItems(roomNotifications);
        notificationTableView.setFixedCellSize(-1);
    }

    private void resetRoomHelper() {
        if (roomHelper != null) {
            roomHelper.cleanup();
        }

        roomHelper = new AuctionRoomHelper(currentAuctionId);
        roomHelper.setOnTimeUpdate(this::updateTimeLabel);
        roomHelper.setOnTimerExpired(() -> {
        });

        if (auction.getEndTimeEpoch() > 0) {
            roomHelper.initTimer(auction.getEndTimeEpoch());
            roomHelper.startTimer();
        } else if (auction.getEndTime() != null) {
            roomHelper.initTimer(auction.getEndTime());
            roomHelper.startTimer();
        }

        roomHelper.registerTimeExtendedListener(() -> {
            updateTimeLabel();
            AlertHelper.showInfo("Gia hạn", "Có người đặt giá phút chót! Thời gian cộng thêm");
        });
        roomHelper.registerParticipantsListener(
                count -> System.out.println("[Phòng " + currentAuctionId + "] Số người đang xem: " + count));
        roomHelper.registerRoomKickedListener(this::exitRoom);
    }

    private void registerNetworkListeners() {
        roomHelper.registerRoomListener(Protocol.BROADCAST_AUCTION_START, this::handleAuctionStarted);
        roomHelper.registerRoomListener(Protocol.BROADCAST_NEW_BID, this::handleNewBid);
        roomHelper.registerRoomListener(Protocol.BROADCAST_AUCTION_FINISHED, this::handleAuctionFinished);
        ClientNetworkManager.getInstance().registerListener(Protocol.REQ_BID, this::handleBidResponse);
        ClientNetworkManager.getInstance().registerListener(Protocol.REQ_AUTOBID, this::handleAutoBidResponse);
        SessionManager.getInstance().balanceProperty()
                .addListener((obs, oldVal, newVal) -> updateBalanceDisplay(newVal.doubleValue()));
    }

    private void handleAuctionStarted(String message) {
        String[] parts = message.split(Protocol.DELIMITER);
        if (parts.length >= 3 && Objects.equals(parts[1], currentAuctionId)) {
            int durationMinutes = Integer.parseInt(parts[2]);
            Platform.runLater(() -> {
                roomHelper.initTimer(LocalDateTime.now().plusMinutes(durationMinutes));
                roomHelper.startTimer();
            });
            NotificationManager.getInstance().addNotification("Phiên đấu giá " + currentAuctionId + " đã BẮT ĐẦU!");
        }
    }

    private void handleNewBid(String message) {
        String[] parts = message.split(Protocol.DELIMITER);
        if (parts.length < 4 || !Objects.equals(parts[1], currentAuctionId)) {
            return;
        }

        double newPrice = Double.parseDouble(parts[2]);
        String topBidder = parts[3];
        if (newPrice == currentHighestPrice && topBidder.equals(currentTopBidder)) {
            return;
        }

        currentHighestPrice = newPrice;
        updateTopBidder(topBidder);
        updateIncrementDisplay(newPrice);
        bidCount++;
        Platform.runLater(() -> addBidPoint(bidCount, newPrice, "[" + auction.getItem().getName()
                + "] Cập nhật giá: " + topBidder + " - $" + ChartHelper.formatDouble(newPrice)));
    }

    private void handleAuctionFinished(String message) {
        String[] parts = message.split(Protocol.DELIMITER);
        if (parts.length < 2 || !Objects.equals(parts[1], currentAuctionId)) {
            return;
        }

        String finalWinner = parts.length > 2 ? parts[2] : "Không có";
        double finalPrice = parts.length > 3 ? Double.parseDouble(parts[3]) : 0;
        Platform.runLater(() -> {
            if (roomHelper != null) {
                roomHelper.stopTimer();
            }
            updateTopBidder(finalWinner);
            addNotification("PHIÊN ĐẤU GIÁ KẾT THÚC! Người thắng: "
                    + finalWinner + " ($" + ChartHelper.formatDouble(finalPrice) + ")");
            AlertHelper.showInfo("Kết quả đấu giá",
                    "Người chiến thắng: " + finalWinner + "\nGiá cuối: $" + ChartHelper.formatDouble(finalPrice));
            if (!SessionManager.getInstance().isAdmin()) {
                exitRoom();
            }
        });
    }

    private void handleBidResponse(String response) {
        String[] parts = response.split(Protocol.DELIMITER);
        Platform.runLater(() -> {
            if (!(parts.length > 1 && parts[1].equals(Protocol.RES_SUCCESS))) {
                AlertHelper.showWarning("Đặt giá thất bại",
                        parts.length >= 3 ? parts[2] : "Lỗi không xác định");
            }
        });
    }

    private void handleAutoBidResponse(String response) {
        String[] parts = response.split(Protocol.DELIMITER);
        Platform.runLater(() -> {
            if (parts.length > 1 && parts[1].equals(Protocol.RES_SUCCESS)) {
                boolean cancelled = parts.length > 2 && parts[2].equals("CANCEL");
                AlertHelper.showInfo("Auto-bid", cancelled ? "Đã tắt tính năng đặt giá tự động."
                        : "Đã bật tính năng đặt giá tự động thành công!");
                setAutoBidState(!cancelled, cancelled ? "Tắt" : "Bật");
            } else {
                AlertHelper.showWarning("Auto-bid thất bại",
                        parts.length >= 3 ? parts[2] : "Lỗi không xác định");
                setAutoBidState(false, "Tắt");
            }
        });
    }

    private void restoreAuctionState() {
        Platform.runLater(() -> {
            priceChart.setAnimated(false);
            priceSeries.getData().clear();
            historyData.clear();
            roomNotifications.clear();
            bidCount = 0;
            priceSeries.getData().add(new XYChart.Data<>(0, auction.getStartingPrice()));

            if (auction.getBidHistory() != null && !auction.getBidHistory().isEmpty()) {
                for (BidTransaction tx : auction.getBidHistory()) {
                    bidCount++;
                    priceSeries.getData().add(new XYChart.Data<>(bidCount, tx.getBidAmount()));
                    historyData.add(0, new HistoryItem(bidCount, tx.getBidAmount()));
                    addNotification("[" + auction.getItem().getName() + "] - Lượt #" + bidCount + ": "
                            + tx.getBidder().getUsername() + " đã đặt $" + ChartHelper.formatDouble(tx.getBidAmount()),
                            tx.getTimestamp().format(TIME_FORMATTER));
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
    }

    private void addBidPoint(int round, double price, String notification) {
        ChartHelper.updateXAxisBounds(priceChart, round);
        priceSeries.getData().add(new XYChart.Data<>(round, price));
        historyData.add(0, new HistoryItem(round, price));
        addNotification(notification);
    }

    private void updateTimeLabel() {
        if (totalTimeLabel != null && roomHelper != null) {
            totalTimeLabel.setText(ChartHelper.formatTime(roomHelper.getTotalTimeRemaining()));
        }
    }

    private void updateTopBidder(String bidderName) {
        currentTopBidder = bidderName;
        Platform.runLater(() -> {
            if (topBidderLabel != null) {
                topBidderLabel.setText(bidderName);
            }
        });
    }

    private void updateBalanceDisplay(double balance) {
        Platform.runLater(() -> {
            if (balanceLabel != null) {
                balanceLabel.setText(ChartHelper.formatDouble(balance) + " $");
            }
        });
    }

    private void updateIncrementDisplay(double currentPrice) {
        double roundedIncrement = ChartHelper.calculateMinIncrement(currentPrice);
        Platform.runLater(() -> {
            if (bidIncrementLabel != null) {
                bidIncrementLabel.setText(ChartHelper.formatDouble(roundedIncrement) + " $");
            }
            if (bidAmountField != null) {
                bidAmountField.setPromptText("Tối thiểu: " + ChartHelper.formatDouble(currentPrice + roundedIncrement));
            }
        });
    }

    private void enableAutoBid(ToggleButton button) {
        button.setText("Bật");
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Cấu hình Auto-Bid");
        dialog.setHeaderText("Nhập giá tối đa bạn sẵn sàng trả (Max Bid)\n"
                + "Bước nhảy giá sẽ được tự động tính toán tối ưu theo thời gian thực (10%)");
        dialog.setContentText("Giá tối đa ($):");

        Optional<String> result = dialog.showAndWait();
        if (result.isPresent() && tryEnableAutoBid(result.get(), button)) {
            return;
        }
        setAutoBidState(false, "Tắt");
    }

    private boolean tryEnableAutoBid(String input, ToggleButton button) {
        try {
            double maxBid = Double.parseDouble(input.trim());
            if (maxBid <= 0) {
                AlertHelper.showWarning("Lỗi", "Vui lòng nhập số lớn hơn 0!");
                return false;
            }
            if (!commandService.enableAutoBid(currentAuctionId, maxBid)) {
                AlertHelper.showError("Lỗi kết nối", "Không thể đăng ký auto-bid!");
                setAutoBidState(false, "Tắt");
                return false;
            }
            return true;
        } catch (NumberFormatException e) {
            AlertHelper.showWarning("Lỗi", "Vui lòng nhập số hợp lệ!");
            return false;
        }
    }

    private void cancelAutoBid(ToggleButton button) {
        button.setText("Tắt");
        if (!commandService.cancelAutoBid(currentAuctionId)) {
            AlertHelper.showError("Lỗi kết nối", "Không thể hủy đăng ký auto-bid!");
            setAutoBidState(true, "Bật");
        }
    }

    private void setAutoBidState(boolean selected, String text) {
        if (autoBidToggleButton != null) {
            autoBidToggleButton.setSelected(selected);
            autoBidToggleButton.setText(text);
        }
    }

    private void addNotification(String content) {
        addNotification(content, LocalTime.now().format(TIME_FORMATTER));
    }

    private void addNotification(String content, String time) {
        roomNotifications.add(0, new NotificationManager.NotificationItem(content, time));
    }

    private Stage getCurrentStage() {
        if (priceChart != null && priceChart.getScene() != null
                && priceChart.getScene().getWindow() instanceof Stage stage) {
            return stage;
        }
        return null;
    }

    public static class HistoryItem {
        private final int round;
        private final double price;

        public HistoryItem(int round, double price) {
            this.round = round;
            this.price = price;
        }

        public int getRound() {
            return round;
        }

        public double getPrice() {
            return price;
        }
    }
}
