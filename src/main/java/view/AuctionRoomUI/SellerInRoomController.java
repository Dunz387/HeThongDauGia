package view.AuctionRoomUI;

import javafx.application.Platform;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.stage.Stage;
import javafx.util.Duration;
import network.ClientNetworkManager;
import shared.Protocol;
import view.utility.AlertHelper;
import view.utility.SceneManager;

import java.net.URL;
import java.util.ResourceBundle;

/**
 * Controller cho màn hình Seller theo dõi phiên đấu giá của mình.
 * Chỉ xem, không đặt giá.
 */
public class SellerInRoomController implements Initializable {

    // Header
    @FXML private Label lblRoomId;
    @FXML private Label totalTimeLabel;

    // Sidebar trái
    @FXML private Label lblCurrentPrice;
    @FXML private Label lblMinStep;

    // Biểu đồ
    @FXML private javafx.scene.chart.AreaChart<Number, Number> priceChart;

    // Bảng lịch sử giá
    @FXML private TableView<double[]> tableBidHistory;
    @FXML private TableColumn<double[], Integer> colRound;
    @FXML private TableColumn<double[], Double> colBidPrice;

    // Bottom
    @FXML private Label lblEarnings;
    @FXML private Label lblParticipants;
    @FXML private Label lblRounds;
    @FXML private Label topBidderLabel;

    // Bảng thông báo phòng
    @FXML private TableView<network.NotificationManager.NotificationItem> notificationTableView;
    @FXML private TableColumn<network.NotificationManager.NotificationItem, String> colNotifTime;
    @FXML private TableColumn<network.NotificationManager.NotificationItem, String> colNotifContent;

    private XYChart.Series<Number, Number> priceSeries;
    private ObservableList<double[]> bidHistory = FXCollections.observableArrayList();
    private ObservableList<network.NotificationManager.NotificationItem> roomNotifications = FXCollections.observableArrayList();
    private int bidCount = 0;
    private model.auction.Auction auction;
    private String currentAuctionId = null;
    private double currentHighestPrice = 0;

    // Quản lý thời gian
    private Timeline totalTimelineTimer;
    private java.time.LocalDateTime auctionEndTime;
    private int totalTimeRemaining = 0;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // === KHỞI TẠO BIỂU ĐỒ ===
        priceSeries = new XYChart.Series<>();
        priceSeries.setName("Biến động giá ($)");
        priceChart.getData().add(priceSeries);
        priceChart.setAnimated(true);

        // === KHỞI TẠO BẢNG LỊCH SỬ GIÁ ===
        colRound.setCellValueFactory(cellData ->
                new SimpleIntegerProperty((int) cellData.getValue()[0]).asObject());
        colBidPrice.setCellValueFactory(cellData ->
                new SimpleDoubleProperty(cellData.getValue()[1]).asObject());
        tableBidHistory.setItems(bidHistory);

        // === KHỞI TẠO BẢNG THÔNG BÁO PHÒNG ===
        colNotifTime.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("time"));
        colNotifContent.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("content"));
        notificationTableView.setItems(roomNotifications);
        
        // Tự động xuống dòng cho nội dung thông báo
        colNotifContent.setCellFactory(tc -> {
            javafx.scene.control.TableCell<network.NotificationManager.NotificationItem, String> cell = new javafx.scene.control.TableCell<>() {
                private final javafx.scene.text.Text textNode = new javafx.scene.text.Text();
                {
                    textNode.wrappingWidthProperty().bind(tc.widthProperty().subtract(10));
                    textNode.setStyle("-fx-fill: #333333; -fx-font-family: 'Segoe UI'; -fx-font-size: 13px;");
                    setGraphic(textNode);
                }
                @Override protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) { textNode.setText(null); setGraphic(null); }
                    else { textNode.setText(item); setGraphic(textNode); }
                }
            };
            return cell;
        });

        // Cấu hình Trục X
        if (priceChart.getXAxis() instanceof NumberAxis) {
            NumberAxis xAxis = (NumberAxis) priceChart.getXAxis();
            xAxis.setMinorTickVisible(false);
            xAxis.setMinorTickCount(0);
            xAxis.setTickUnit(1.0);
            xAxis.setAutoRanging(false); 
            xAxis.setLowerBound(0);
            xAxis.setUpperBound(10);
            
            xAxis.setTickLabelFormatter(new javafx.util.StringConverter<Number>() {
                @Override public String toString(Number object) {
                    double val = object.doubleValue();
                    // Chỉ hiện nhãn nếu giá trị là số nguyên (hoặc cực gần số nguyên)
                    if (Math.abs(val - Math.round(val)) < 0.0001) {
                        int intVal = (int) Math.round(val);
                        if (intVal < 0) return "";
                        if (intVal == 0) return "BĐ";
                        return String.valueOf(intVal);
                    }
                    return "";
                }
                @Override public Number fromString(String string) { return 0; }
            });
        }

        // === ĐĂNG KÝ LẮNG NGHE TỪ SERVER ===
        registerNetworkListeners();
    }

    private void registerNetworkListeners() {
        // Lắng nghe giá mới (REAL-TIME)
        ClientNetworkManager.getInstance().registerListener(Protocol.BROADCAST_NEW_BID, (message) -> {
            String[] parts = message.split(Protocol.SEPARATOR);
            if (parts.length >= 4) {
                String auctionId = parts[1];
                if (currentAuctionId != null && auctionId.equals(this.currentAuctionId)) {
                    double newPrice = Double.parseDouble(parts[2]);
                    String topBidder = parts[3];
                    currentHighestPrice = newPrice;

                    Platform.runLater(() -> {
                        lblCurrentPrice.setText(String.format("%.0f $", newPrice));
                        lblEarnings.setText(String.format("%.0f $", newPrice));
                        topBidderLabel.setText(topBidder);
                        bidCount++;
                        if (priceChart.getXAxis() instanceof NumberAxis) {
                            NumberAxis xAxis = (NumberAxis) priceChart.getXAxis();
                            xAxis.setUpperBound(Math.max(10, bidCount + 1));
                            xAxis.setLowerBound(Math.max(0, bidCount - 15));
                        }
                        priceChart.setAnimated(true);
                        priceSeries.getData().add(new XYChart.Data<>(bidCount, newPrice));
                        bidHistory.add(new double[]{bidCount, newPrice});
                        lblRounds.setText(String.valueOf(bidCount));
                        updateIncrementDisplay(newPrice);
                        if (auction != null) {
                            String msg = "📢 [" + auction.getItem().getName() + "] - Lượt #" + bidCount + ": " + topBidder + " vừa đặt $" + newPrice;
                            String timeStr = java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss"));
                            
                            // Cập nhật bảng thông báo tại phòng
                            roomNotifications.add(0, new network.NotificationManager.NotificationItem(msg, timeStr));
                            
                            // Global notification
                            network.NotificationManager.getInstance().addNotification(msg);
                        }
                    });
                }
            }
        });

        // Lắng nghe lệnh BROADCAST_TIME_EXTENDED
        ClientNetworkManager.getInstance().registerListener(Protocol.BROADCAST_TIME_EXTENDED, (message) -> {
            String[] parts = message.split(Protocol.SEPARATOR);
            if (parts.length >= 3) {
                String auctionId = parts[1];
                int addedSeconds = Integer.parseInt(parts[2]);
                if (currentAuctionId != null && auctionId.equals(this.currentAuctionId)) {
                    Platform.runLater(() -> {
                        if (auctionEndTime != null) auctionEndTime = auctionEndTime.plusSeconds(addedSeconds);
                        totalTimeRemaining += addedSeconds;
                        updateTotalTimeDisplay();
                        
                        String timeStr = java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss"));
                        roomNotifications.add(0, new network.NotificationManager.NotificationItem("⏳ Thời gian được gia hạn thêm " + addedSeconds + "s", timeStr));
                    });
                }
            }
        });

        // Lắng nghe kết thúc phiên
        ClientNetworkManager.getInstance().registerListener(Protocol.BROADCAST_AUCTION_FINISHED, (message) -> {
            String[] parts = message.split(Protocol.SEPARATOR);
            if (parts.length >= 2) {
                String auctionId = parts[1];
                String winner = parts.length > 2 ? parts[2] : "Không có";
                double finalPrice = parts.length > 3 ? Double.parseDouble(parts[3]) : 0;
                if (currentAuctionId != null && auctionId.equals(this.currentAuctionId)) {
                    Platform.runLater(() -> {
                        stopAllTimers();
                        AlertHelper.showInfo("Phiên đấu giá kết thúc!",
                                "Người chiến thắng: " + winner + "\nGiá cuối cùng: " + finalPrice + " $");
                    });
                }
            }
        });

        // Lắng nghe số người tham gia phòng
        ClientNetworkManager.getInstance().registerListener(Protocol.BROADCAST_PARTICIPANTS, (message) -> {
            String[] parts = message.split(Protocol.SEPARATOR);
            if (parts.length >= 3) {
                String auctionId = parts[1];
                String count = parts[2];
                if (currentAuctionId != null && auctionId.equals(this.currentAuctionId)) {
                    Platform.runLater(() -> {
                        if (lblParticipants != null) lblParticipants.setText(count + " người");
                    });
                }
            }
        });
    }

    /**
     * Thiết lập phiên đấu giá mà Seller đang theo dõi.
     */
    public void setAuction(model.auction.Auction auction) {
        this.auction = auction;
        this.currentAuctionId = auction.getId();
        if (lblRoomId != null) {
            lblRoomId.setText("ID phòng: " + auction.getId());
        }
        if (auction.getEndTime() != null) {
            this.auctionEndTime = auction.getEndTime();
            long seconds = java.time.Duration.between(java.time.LocalDateTime.now(), auctionEndTime).getSeconds();
            this.totalTimeRemaining = (int) Math.max(0, seconds);
            
            Platform.runLater(() -> {
                startTotalTimer();
                updateTotalTimeDisplay();
            });
        }
        
        // Gửi yêu cầu tham gia phòng để Server tính số người tham gia
        ClientNetworkManager.getInstance().sendData(Protocol.REQ_JOIN_ROOM + Protocol.DELIMITER + currentAuctionId);

        // === KHÔI PHỤC DỮ LIỆU ĐẤU GIÁ ===
        Platform.runLater(() -> {
            priceSeries.getData().clear();
            bidHistory.clear();
            
            // Điểm bắt đầu
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
                 if (lblCurrentPrice != null) lblCurrentPrice.setText(String.format("%.0f $", currentHighestPrice));
                 if (lblEarnings != null) lblEarnings.setText(String.format("%.0f $", currentHighestPrice));
                 if (topBidderLabel != null) topBidderLabel.setText(auction.getHighestBidder().getUsername());
                 if (lblRounds != null) lblRounds.setText(String.valueOf(bidCount));
            } else {
                 currentHighestPrice = auction.getStartingPrice();
            }
            
            // Phục hồi thông báo từ lịch sử (Format đồng bộ)
            roomNotifications.clear();
            if (history != null) {
                int tempCount = 0;
                for (model.auction.BidTransaction tx : history) {
                    tempCount++;
                    String timeStr = tx.getTimestamp().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss"));
                    String msg = "📢 [" + auction.getItem().getName() + "] - Lượt #" + tempCount + ": " + tx.getBidder().getUsername() + " đã đặt $" + tx.getBidAmount();
                    roomNotifications.add(0, new network.NotificationManager.NotificationItem(msg, timeStr));
                }
            }

            // Cập nhật mốc đồ thị
            if (priceChart.getXAxis() instanceof NumberAxis) {
                NumberAxis xAxis = (NumberAxis) priceChart.getXAxis();
                xAxis.setUpperBound(Math.max(10, bidCount + 2));
                xAxis.setLowerBound(Math.max(0, bidCount - 15));
            }

            updateIncrementDisplay(currentHighestPrice);
        });
    }

    @FXML
    private void exitRoom(ActionEvent event) {
        stopAllTimers();
        
        ClientNetworkManager.getInstance().clearListeners(Protocol.BROADCAST_NEW_BID);
        ClientNetworkManager.getInstance().clearListeners(Protocol.BROADCAST_TIME_EXTENDED);
        ClientNetworkManager.getInstance().clearListeners(Protocol.BROADCAST_AUCTION_FINISHED);
        ClientNetworkManager.getInstance().clearListeners(Protocol.BROADCAST_PARTICIPANTS);

        // Gửi thông báo rời phòng
        if (currentAuctionId != null) {
            ClientNetworkManager.getInstance().sendData(Protocol.REQ_LEAVE_ROOM + Protocol.DELIMITER + currentAuctionId);
        }

        Stage stage = (Stage) priceChart.getScene().getWindow();
        
        if (network.SessionManager.getInstance().isAdmin()) {
            SceneManager.goToAdminAuctionManagement(stage);
        } else {
            SceneManager.goToBaseMenu(stage);
        }
    }

    private void startTotalTimer() {
        if (totalTimelineTimer != null) totalTimelineTimer.stop();
        totalTimelineTimer = new Timeline(new KeyFrame(
                Duration.seconds(1),
                event -> {
                    if (auctionEndTime != null) {
                        long seconds = java.time.Duration.between(java.time.LocalDateTime.now(), auctionEndTime).getSeconds();
                        totalTimeRemaining = (int) Math.max(0, seconds);
                        updateTotalTimeDisplay();
                        if (totalTimeRemaining <= 0) stopAllTimers();
                    }
                }
        ));
        totalTimelineTimer.setCycleCount(Timeline.INDEFINITE);
        totalTimelineTimer.play();
    }

    private void stopAllTimers() {
        if (totalTimelineTimer != null) { totalTimelineTimer.stop(); totalTimelineTimer = null; }
    }

    private void updateTotalTimeDisplay() {
        if (totalTimeLabel != null) {
            int m = totalTimeRemaining / 60;
            int s = totalTimeRemaining % 60;
            totalTimeLabel.setText(String.format("%02d:%02d", m, s));
        }
    }

    private void updateIncrementDisplay(double currentPrice) {
        double increment = currentPrice * 0.1;
        double roundedIncrement;
        if (increment < 1) roundedIncrement = 1.0;
        else if (increment < 10) roundedIncrement = Math.floor(increment);
        else if (increment < 100) roundedIncrement = Math.floor(increment / 5) * 5;
        else roundedIncrement = Math.floor(increment / 10) * 10;

        Platform.runLater(() -> {
            if (lblMinStep != null) {
                lblMinStep.setText(String.format("%,.0f $", roundedIncrement));
            }
        });
    }
}

