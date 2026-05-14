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
    @FXML private Label roundTimeLabel;

    // Sidebar trái
    @FXML private Label lblCurrentPrice;
    @FXML private Label lblMinStep;

    // Biểu đồ
    @FXML private LineChart<String, Number> priceChart;

    // Bảng lịch sử giá
    @FXML private TableView<double[]> tableBidHistory;
    @FXML private TableColumn<double[], Integer> colRound;
    @FXML private TableColumn<double[], Double> colBidPrice;

    // Bottom
    @FXML private Label lblEarnings;
    @FXML private Label lblParticipants;
    @FXML private Label lblRounds;
    @FXML private Label topBidderLabel;

    private XYChart.Series<String, Number> priceSeries;
    private ObservableList<double[]> bidHistory = FXCollections.observableArrayList();
    private int bidCount = 0;
    private String currentAuctionId = null;
    private double currentHighestPrice = 0;

    // Quản lý thời gian
    private Timeline totalTimelineTimer;
    private Timeline roundTimelineTimer;
    private java.time.LocalDateTime auctionEndTime;
    private int totalTimeRemaining = 0;
    private static final int ROUND_DURATION = 30;
    private int roundTimeRemaining = ROUND_DURATION;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // === KHỞI TẠO BIỂU ĐỒ ===
        priceSeries = new XYChart.Series<>();
        priceSeries.setName("Biến động giá");
        priceChart.getData().add(priceSeries);
        priceChart.setAnimated(false);

        // === KHỞI TẠO BẢNG LỊCH SỬ GIÁ ===
        colRound.setCellValueFactory(cellData ->
                new SimpleIntegerProperty((int) cellData.getValue()[0]).asObject());
        colBidPrice.setCellValueFactory(cellData ->
                new SimpleDoubleProperty(cellData.getValue()[1]).asObject());
        tableBidHistory.setItems(bidHistory);

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
                    bidCount++;

                    Platform.runLater(() -> {
                        // Cập nhật giá hiện tại
                        lblCurrentPrice.setText(String.format("%.0f $", newPrice));
                        lblEarnings.setText(String.format("%.0f $", newPrice));

                        // Cập nhật người đặt cao nhất
                        topBidderLabel.setText(topBidder);

                        // Cập nhật biểu đồ
                        priceSeries.getData().add(new XYChart.Data<>(String.valueOf(bidCount), newPrice));
                        if (priceSeries.getData().size() > 20) {
                            priceSeries.getData().remove(0);
                        }

                        // Cập nhật bảng lịch sử
                        bidHistory.add(new double[]{bidCount, newPrice});

                        // Cập nhật số vòng
                        lblRounds.setText(String.valueOf(bidCount));
                        
                        // Cập nhật bước giá mới
                        updateIncrementDisplay(newPrice);
                        
                        // Reset round timer
                        resetRoundTimer();
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
                        if (auctionEndTime != null) {
                            auctionEndTime = auctionEndTime.plusSeconds(addedSeconds);
                        }
                        totalTimeRemaining += addedSeconds;
                        updateTotalTimeDisplay();
                        System.out.println("⏳ [Anti-Sniping] Thời gian đã được cộng thêm " + addedSeconds + "s");
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
        
        // Lắng nghe lệnh BROADCAST_ROUND_FINISHED
        ClientNetworkManager.getInstance().registerListener(Protocol.BROADCAST_ROUND_FINISHED, (message) -> {
            String[] parts = message.split(Protocol.SEPARATOR);
            if (parts.length >= 2) {
                String auctionId = parts[1];
                if (currentAuctionId != null && auctionId.equals(this.currentAuctionId)) {
                    Platform.runLater(this::resetRoundTimer);
                }
            }
        });
    }

    /**
     * Thiết lập phiên đấu giá mà Seller đang theo dõi.
     * Gọi trước khi chuyển sang màn hình này.
     */
    public void setAuction(model.auction.Auction auction) {
        this.currentAuctionId = auction.getId();
        if (lblRoomId != null) {
            lblRoomId.setText("ID phòng: " + auction.getId());
        }
        if (auction.getEndTime() != null) {
            this.auctionEndTime = auction.getEndTime();
            long seconds = java.time.Duration.between(java.time.LocalDateTime.now(), auctionEndTime).getSeconds();
            this.totalTimeRemaining = (int) Math.max(0, seconds);
            
            // Tính thời gian lượt còn lại
            long secondsSinceActivity = java.time.Duration.between(auction.getLastActivityTime(), java.time.LocalDateTime.now()).getSeconds();
            this.roundTimeRemaining = Math.max(0, ROUND_DURATION - (int) secondsSinceActivity);
            
            Platform.runLater(() -> {
                startTotalTimer();
                startRoundTimer();
                updateTotalTimeDisplay();
                updateRoundTimeDisplay();
            });
        }
        if (auction.getHighestBidder() != null) {
             currentHighestPrice = auction.getCurrentPrice();
             if (lblCurrentPrice != null) lblCurrentPrice.setText(String.format("%.0f $", currentHighestPrice));
             if (lblEarnings != null) lblEarnings.setText(String.format("%.0f $", currentHighestPrice));
             if (topBidderLabel != null) topBidderLabel.setText(auction.getHighestBidder().getUsername());
             Platform.runLater(() -> {
                 priceSeries.getData().add(new XYChart.Data<>("Current", currentHighestPrice));
             });
        }
        
        // Cập nhật bước giá ban đầu
        updateIncrementDisplay(currentHighestPrice);
    }

    @FXML
    private void exitRoom(ActionEvent event) {
        stopAllTimers();
        Stage stage = (Stage) priceChart.getScene().getWindow();
        SceneManager.goToBaseMenu(stage);
    }

    // === CÁC HÀM QUẢN LÝ TIMER ===

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

    private void startRoundTimer() {
        if (roundTimelineTimer != null) roundTimelineTimer.stop();
        roundTimelineTimer = new Timeline(new KeyFrame(
                Duration.seconds(1),
                event -> {
                    if (roundTimeRemaining > 0) {
                        roundTimeRemaining--;
                        updateRoundTimeDisplay();
                    }
                }
        ));
        roundTimelineTimer.setCycleCount(Timeline.INDEFINITE);
        roundTimelineTimer.play();
    }

    private void resetRoundTimer() {
        roundTimeRemaining = ROUND_DURATION;
        if (roundTimelineTimer != null) roundTimelineTimer.stop();
        startRoundTimer();
    }

    private void stopAllTimers() {
        if (totalTimelineTimer != null) { totalTimelineTimer.stop(); totalTimelineTimer = null; }
        if (roundTimelineTimer != null) { roundTimelineTimer.stop(); roundTimelineTimer = null; }
    }

    private void updateTotalTimeDisplay() {
        if (totalTimeLabel != null) {
            int m = totalTimeRemaining / 60;
            int s = totalTimeRemaining % 60;
            totalTimeLabel.setText(String.format("%02d:%02d", m, s));
        }
    }

    private void updateRoundTimeDisplay() {
        if (roundTimeLabel != null) {
            int m = roundTimeRemaining / 60;
            int s = roundTimeRemaining % 60;
            roundTimeLabel.setText(String.format("%02d:%02d", m, s));
        }
    }

    private void updateIncrementDisplay(double currentPrice) {
        // Tính toán bước giá theo quy tắc 10% đồng bộ với Auction.java
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
