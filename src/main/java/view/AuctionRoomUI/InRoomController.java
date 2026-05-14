package view.AuctionRoomUI;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.animation.Timeline;
import javafx.animation.KeyFrame;
import javafx.stage.Stage;
import javafx.util.Duration;
import network.ClientNetworkManager;
import network.NotificationManager;
import shared.Protocol;
import view.utility.AlertHelper;
import view.utility.SceneManager;
import network.SessionManager;

import java.net.URL;
import java.util.ResourceBundle;

public class InRoomController implements Initializable {

    @FXML
    private LineChart<String, Number> priceChart;

    @FXML
    private TextField bidAmountField;

    @FXML
    private Label topBidderLabel;
    
    @FXML
    private Label totalTimeLabel;
    
    @FXML
    private Label roundTimeLabel;

    @FXML
    private Label roomIdLabel;
    
    @FXML
    private Label balanceLabel;
    
    @FXML
    private Label bidIncrementLabel;
    
    // Notifications & History Table
    @FXML private TableView<NotificationManager.NotificationItem> notificationTableView;
    @FXML private TableColumn<NotificationManager.NotificationItem, String> colNotifTime;
    @FXML private TableColumn<network.NotificationManager.NotificationItem, String> colNotifContent;

    @FXML private TableView<HistoryItem> historyTableView;
    @FXML private TableColumn<HistoryItem, Integer> colHistoryRound;
    @FXML private TableColumn<HistoryItem, Double> colHistoryPrice;

    private javafx.collections.ObservableList<HistoryItem> historyData = javafx.collections.FXCollections.observableArrayList();

    public static class HistoryItem {
        private int round;
        private double price;
        public HistoryItem(int round, double price) { this.round = round; this.price = price; }
        public int getRound() { return round; }
        public double getPrice() { return price; }
    }

    private XYChart.Series<String, Number> priceSeries;
    private int bidCount = 0;

    // ID của phiên đấu giá hiện tại — được truyền vào từ SceneManager.goToInRoom(stage, auctionId)
    private String currentAuctionId = null;

    // Quản lý thời gian
    private Timeline totalTimelineTimer;
    private Timeline roundTimelineTimer;
    private java.time.LocalDateTime auctionEndTime; // Lưu thời gian kết thúc từ server
    private int totalTimeRemaining = 0; // Tính bằng giây
    private static final int ROUND_DURATION = 30; // Thời gian mỗi vòng: 30 giây
    private int roundTimeRemaining = ROUND_DURATION; // Tính bằng giây
    private String currentTopBidder = "Chưa có";
    private double currentRoundHighestPrice = 0; // Giá cao nhất trong vòng
    private String roundWinner = ""; // Người chiến thắng vòng

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        priceSeries = new XYChart.Series<>();
        priceSeries.setName("Lịch sử biến động giá");
        priceChart.getData().add(priceSeries);
        priceChart.setAnimated(false);
        
        // Cấu hình bảng lịch sử giá
        colHistoryRound.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("round"));
        colHistoryPrice.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("price"));
        historyTableView.setItems(historyData);

        // Cấu hình bảng thông báo
        colNotifTime.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("time"));
        colNotifContent.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("content"));
        notificationTableView.setItems(network.NotificationManager.getInstance().getNotifications());

        // Khởi tạo giá trị hiển thị
        if (topBidderLabel != null) {
            topBidderLabel.setText(currentTopBidder);
        }

        // === SỬ DỤNG CLIENTNETWORKMANAGER ĐỂ LẮNG NGHE (thay vì tạo socket riêng) ===
        registerNetworkListeners();
    }

    /**
     * Thiết lập ID phiên đấu giá hiện tại.
     * Được gọi từ SceneManager.goToInRoom(stage, auction) SAU khi FXML đã load.
     */
    public void setAuction(model.auction.Auction auction) {
        this.currentAuctionId = auction.getId();
        if (roomIdLabel != null) {
            roomIdLabel.setText("ID phòng: " + currentAuctionId);
        }
        System.out.println("🏛️ [InRoom] Đã thiết lập phòng đấu giá: " + currentAuctionId);

        // Tính tổng thời gian còn lại
        if (auction.getEndTime() != null) {
            this.auctionEndTime = auction.getEndTime();
            long seconds = java.time.Duration.between(java.time.LocalDateTime.now(), auctionEndTime).getSeconds();
            this.totalTimeRemaining = (int) Math.max(0, seconds);
            
            // Tính thời gian lượt còn lại
            long secondsSinceActivity = java.time.Duration.between(auction.getLastActivityTime(), java.time.LocalDateTime.now()).getSeconds();
            this.roundTimeRemaining = Math.max(0, ROUND_DURATION - (int) secondsSinceActivity);
            
            javafx.application.Platform.runLater(() -> {
                startTotalTimer();
                startRoundTimer();
            });
            updateTotalTimeDisplay();
            updateRoundTimeDisplay();
        }
        
        // Cập nhật giá cao nhất và top bidder hiện tại nếu có
        if (auction.getHighestBidder() != null) {
             currentRoundHighestPrice = auction.getCurrentPrice();
             roundWinner = auction.getHighestBidder().getUsername();
             updateTopBidder(roundWinner);
             javafx.application.Platform.runLater(() -> {
                 priceSeries.getData().add(new XYChart.Data<>("Current", currentRoundHighestPrice));
             });
        } else if (auction.getCurrentPrice() > 0) {
             currentRoundHighestPrice = auction.getCurrentPrice();
             javafx.application.Platform.runLater(() -> {
                 priceSeries.getData().add(new XYChart.Data<>("Start", currentRoundHighestPrice));
             });
        }
        
        // Cập nhật Số dư và Bước giá khởi điểm
        updateBalanceDisplay(network.SessionManager.getInstance().getBalance());
        updateIncrementDisplay(currentRoundHighestPrice);
    }

    private void registerNetworkListeners() {
        // Lắng nghe lệnh BROADCAST_AUCTION_START
        ClientNetworkManager.getInstance().registerListener(Protocol.BROADCAST_AUCTION_START, (message) -> {
            String[] parts = message.split(Protocol.SEPARATOR);
            if (parts.length >= 3) {
                String auctionId = parts[1];
                int durationMinutes = Integer.parseInt(parts[2]);

                if (currentAuctionId != null && auctionId.equals(this.currentAuctionId)) {
                    totalTimeRemaining = durationMinutes * 60;
                    roundTimeRemaining = ROUND_DURATION;
                    currentRoundHighestPrice = 0;
                    roundWinner = "";

                    Platform.runLater(() -> {
                        startTotalTimer();
                        startRoundTimer();
                    });

                    System.out.println("[Auction] Phiên đấu giá bắt đầu. Tổng thời gian: " + durationMinutes + " phút");
                    network.NotificationManager.getInstance().addNotification("🚀 Phiên đấu giá " + auctionId + " đã BẮT ĐẦU!");
                }
            }
        });

        // Lắng nghe lệnh BROADCAST_NEW_BID (Cập nhật giá mới từ người khác)
        ClientNetworkManager.getInstance().registerListener(Protocol.BROADCAST_NEW_BID, (message) -> {
            String[] parts = message.split(Protocol.SEPARATOR);
            if (parts.length >= 4) {
                String auctionId = parts[1];

                if (currentAuctionId != null && auctionId.equals(this.currentAuctionId)) {
                    double newPrice = Double.parseDouble(parts[2]);
                    String topBidder = parts[3];

                    if (newPrice > currentRoundHighestPrice) {
                        currentRoundHighestPrice = newPrice;
                        roundWinner = topBidder;
                        updateTopBidder(topBidder);
                        updateIncrementDisplay(newPrice);
                        
                        // Cập nhật biểu đồ NGAY LẬP TỨC khi có giá mới
                        bidCount++;
                        Platform.runLater(() -> {
                            priceSeries.getData().add(new XYChart.Data<>(String.valueOf(bidCount), newPrice));
                            if (priceSeries.getData().size() > 20) {
                                priceSeries.getData().remove(0);
                            }
                        });
                        
                        // Thêm vào bảng lịch sử giá
                        Platform.runLater(() -> {
                            historyData.add(0, new HistoryItem(bidCount, newPrice));
                        });

                        network.NotificationManager.getInstance().addNotification("💰 " + topBidder + " vừa đặt giá: $" + newPrice);
                    }

                    Platform.runLater(this::resetRoundTimer);
                    updateChartRealTime(newPrice, topBidder);
                }
            }
        });

        // Lắng nghe lệnh BROADCAST_ROUND_FINISHED
        ClientNetworkManager.getInstance().registerListener(Protocol.BROADCAST_ROUND_FINISHED, (message) -> {
            String[] parts = message.split(Protocol.SEPARATOR);
            if (parts.length >= 2) {
                String auctionId = parts[1];

                if (currentAuctionId != null && auctionId.equals(this.currentAuctionId)) {
                    Platform.runLater(() -> {
                        System.out.println("[Round] Vòng kết thúc! Người chiến thắng: " + roundWinner + " với giá: " + currentRoundHighestPrice);
                        if (roundWinner.isEmpty()) {
                            roundWinner = "Không có ai";
                        }
                    });

                    resetRoundForNewBidding();
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
                        AlertHelper.showInfo("Gia hạn", "Có người đặt giá phút chót! Thời gian cộng thêm " + addedSeconds + "s");
                        network.NotificationManager.getInstance().addNotification("⏳ Thời gian được gia hạn thêm " + addedSeconds + "s");
                    });
                }
            }
        });

        // Lắng nghe lệnh BROADCAST_AUCTION_FINISHED
        ClientNetworkManager.getInstance().registerListener(Protocol.BROADCAST_AUCTION_FINISHED, (message) -> {
            String[] parts = message.split(Protocol.SEPARATOR);
            if (parts.length >= 2) {
                String auctionId = parts[1];
                String finalWinner = parts.length > 2 ? parts[2] : "Không có";
                double finalPrice = parts.length > 3 ? Double.parseDouble(parts[3]) : 0;

                if (currentAuctionId != null && auctionId.equals(this.currentAuctionId)) {
                    Platform.runLater(() -> {
                        stopAllTimers();
                        System.out.println("[Auction] Phiên đấu giá kết thúc!");
                        System.out.println("[Result] Người chiến thắng: " + finalWinner + " với giá: " + finalPrice);
                        AlertHelper.showInfo("Kết quả đấu giá",
                                "Người chiến thắng: " + finalWinner + "\nGiá cuối: $" + finalPrice);
                        network.NotificationManager.getInstance().addNotification("🏆 Phiên đấu giá KẾT THÚC! Người thắng: " + finalWinner);
                    });
                }
            }
        });

        // Lắng nghe kết quả lệnh BID
        ClientNetworkManager.getInstance().registerListener(Protocol.REQ_BID, (response) -> {
            String[] parts = response.split(Protocol.SEPARATOR);
            Platform.runLater(() -> {
                if (parts.length > 1 && parts[1].equals(Protocol.RES_SUCCESS)) {
                    System.out.println("✅ Đặt giá thành công!");
                } else {
                    AlertHelper.showWarning("Đặt giá thất bại",
                            parts.length >= 3 ? parts[2] : "Lỗi không xác định");
                }
            });
        });

        // Lắng nghe cập nhật số dư (Real-time)
        ClientNetworkManager.getInstance().registerListener(Protocol.RES_UPDATE_BALANCE, (message) -> {
            String[] parts = message.split(Protocol.SEPARATOR);
            if (parts.length >= 2) {
                double newBalance = Double.parseDouble(parts[1]);
                network.SessionManager.getInstance().setBalance(newBalance);
                updateBalanceDisplay(newBalance);
            }
        });
    }

    // === CÁC HÀM QUẢN LÝ TIMER ===

    private void startTotalTimer() {
        if (totalTimelineTimer != null) {
            totalTimelineTimer.stop();
        }

        totalTimelineTimer = new Timeline(new KeyFrame(
                Duration.seconds(1),
                event -> {
                    if (auctionEndTime != null) {
                        long seconds = java.time.Duration.between(java.time.LocalDateTime.now(), auctionEndTime).getSeconds();
                        totalTimeRemaining = (int) Math.max(0, seconds);
                        updateTotalTimeDisplay();
                        
                        if (totalTimeRemaining <= 0) {
                            System.out.println("[Timer] Phiên đấu giá hết thời gian!");
                            stopAllTimers();
                        }
                    }
                }
        ));

        totalTimelineTimer.setCycleCount(Timeline.INDEFINITE);
        totalTimelineTimer.play();
    }

    private void startRoundTimer() {
        if (roundTimelineTimer != null) {
            roundTimelineTimer.stop();
        }

        roundTimelineTimer = new Timeline(new KeyFrame(
                Duration.seconds(1),
                event -> {
                    if (roundTimeRemaining > 0) {
                        roundTimeRemaining--;
                        updateRoundTimeDisplay();
                    } else {
                        System.out.println("[Timer] Hết thời gian lượt, vòng kết thúc!");
                        resetRoundForNewBidding();
                    }
                }
        ));

        roundTimelineTimer.setCycleCount(Timeline.INDEFINITE);
        roundTimelineTimer.play();
    }

    private void resetRoundTimer() {
        roundTimeRemaining = ROUND_DURATION;
        if (roundTimelineTimer != null) {
            roundTimelineTimer.stop();
        }
        startRoundTimer();
    }

    private void resetRoundForNewBidding() {
        // Không reset bidCount ở đây để biểu đồ chạy tiếp tục
        currentRoundHighestPrice = 0;
        roundWinner = "";
        roundTimeRemaining = ROUND_DURATION;

        if (totalTimeRemaining > 0 && roundTimelineTimer != null) {
            roundTimelineTimer.stop();
            Platform.runLater(this::startRoundTimer);
        }
    }

    private void stopAllTimers() {
        if (totalTimelineTimer != null) {
            totalTimelineTimer.stop();
            totalTimelineTimer = null;
        }
        if (roundTimelineTimer != null) {
            roundTimelineTimer.stop();
            roundTimelineTimer = null;
        }
    }

    // === CÁC HÀM CẬP NHẬT GIAO DIỆN ===

    private void updateTotalTimeDisplay() {
        Platform.runLater(() -> {
            if (totalTimeLabel != null) {
                totalTimeLabel.setText(formatTime(totalTimeRemaining));
            }
        });
    }

    private void updateRoundTimeDisplay() {
        Platform.runLater(() -> {
            if (roundTimeLabel != null) {
                roundTimeLabel.setText(formatTime(roundTimeRemaining));
            }
        });
    }

    private void updateTopBidder(String bidderName) {
        currentTopBidder = bidderName;
        Platform.runLater(() -> {
            if (topBidderLabel != null) {
                topBidderLabel.setText(bidderName);
                System.out.println("[Bidder] Cập nhật người đấu giá cao nhất vòng: " + bidderName);
            }
        });
    }

    private String formatTime(int seconds) {
        int minutes = seconds / 60;
        int secs = seconds % 60;
        return String.format("%02d:%02d", minutes, secs);
    }

    private void updateChartRealTime(double newPrice, String topBidder) {
        Platform.runLater(() -> {
            System.out.println("[Update] Giá mới: " + newPrice + "$ bởi " + topBidder + " | Giá cao nhất vòng: " + currentRoundHighestPrice);
        });
    }

    private void updateBalanceDisplay(double balance) {
        Platform.runLater(() -> {
            if (balanceLabel != null) {
                balanceLabel.setText(String.format("%,.0f $", balance));
            }
        });
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
            if (bidIncrementLabel != null) {
                bidIncrementLabel.setText(String.format("%,.0f $", roundedIncrement));
            }
            if (bidAmountField != null) {
                bidAmountField.setPromptText("Tối thiểu: " + String.format("%,.0f", currentPrice + roundedIncrement));
            }
        });
    }

    // === XỬ LÝ SỰ KIỆN TỪ GIAO DIỆN ===

    @FXML
    private void handlePlaceBid() {
        try {
            if (bidAmountField.getText().isEmpty()) return;
            if (currentAuctionId == null) {
                AlertHelper.showWarning("Lỗi", "Chưa xác định được phòng đấu giá!");
                return;
            }
            double amount = Double.parseDouble(bidAmountField.getText());

            // Sử dụng ClientNetworkManager thay vì socket riêng
            String request = Protocol.REQ_BID + Protocol.DELIMITER +
                             currentAuctionId + Protocol.DELIMITER +
                             amount;

            ClientNetworkManager.getInstance().sendData(request);

            // Xóa rỗng ô nhập sau khi gửi
            bidAmountField.clear();

        } catch (NumberFormatException e) {
            AlertHelper.showWarning("Lỗi", "Vui lòng nhập số tiền hợp lệ!");
        }
    }

    @FXML
    private void exitRoom(ActionEvent event) {
        stopAllTimers();
        Stage stage = (Stage) priceChart.getScene().getWindow();
        SceneManager.goToBaseMenu(stage);
    }

    @FXML
    private void handleAutoBidToggle(ActionEvent event) {
        javafx.scene.control.ToggleButton btn = (javafx.scene.control.ToggleButton) event.getSource();
        if (btn.isSelected()) {
            btn.setText("Bật");
            
            // Show dialog to enter maxBid and increment
            javafx.scene.control.TextInputDialog dialog = new javafx.scene.control.TextInputDialog();
            dialog.setTitle("Cấu hình Auto-Bid");
            dialog.setHeaderText("Nhập Max Bid và Increment (cách nhau bởi dấu phẩy)\nVí dụ: 5000,100");
            dialog.setContentText("Cấu hình:");

            java.util.Optional<String> result = dialog.showAndWait();
            if (result.isPresent()) {
                String[] parts = result.get().split(",");
                if (parts.length == 2) {
                    try {
                        double maxBid = Double.parseDouble(parts[0].trim());
                        double increment = Double.parseDouble(parts[1].trim());
                        
                        String request = Protocol.REQ_AUTOBID + Protocol.DELIMITER +
                                         currentAuctionId + Protocol.DELIMITER +
                                         maxBid + Protocol.DELIMITER +
                                         increment;
                        ClientNetworkManager.getInstance().sendData(request);
                        AlertHelper.showInfo("Thành công", "Đã đăng ký auto-bid!");
                        return;
                    } catch (NumberFormatException e) {
                        AlertHelper.showWarning("Lỗi", "Vui lòng nhập số hợp lệ!");
                    }
                } else {
                    AlertHelper.showWarning("Lỗi", "Vui lòng nhập đúng định dạng (VD: 5000,100)!");
                }
            }
            // If failed or cancelled, uncheck
            btn.setSelected(false);
            btn.setText("Tắt");
        } else {
            btn.setText("Tắt");
            // Trong thực tế cần có lệnh hủy auto-bid, ở đây ta có thể đơn giản bỏ qua
        }
    }
}
