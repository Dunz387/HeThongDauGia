package view.AuctionRoomUI;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
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

import java.net.URL;
import java.util.ResourceBundle;

public class InRoomController implements Initializable {

    @FXML
    private javafx.scene.chart.AreaChart<Number, Number> priceChart;

    @FXML
    private TextField bidAmountField;

    @FXML
    private Label topBidderLabel;
    
    @FXML
    private Label totalTimeLabel;
    
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

    private javafx.collections.ObservableList<network.NotificationManager.NotificationItem> roomNotifications = javafx.collections.FXCollections.observableArrayList();
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

    // ID của phiên đấu giá hiện tại — được truyền vào từ SceneManager.goToInRoom(stage, auctionId)
    private model.auction.Auction auction;
    private String currentAuctionId = null;

    // Quản lý thời gian
    private Timeline totalTimelineTimer;
    private java.time.LocalDateTime auctionEndTime; // Lưu thời gian kết thúc từ server
    private int totalTimeRemaining = 0; // Tính bằng giây
    private String currentTopBidder = "Chưa có";
    private double currentHighestPrice = 0; 

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        priceSeries = new XYChart.Series<>();
        priceSeries.setName("Diễn biến giá ($)");
        priceChart.getData().add(priceSeries);
        priceChart.setAnimated(true); // Cho mượt mà
        
        // CSS Styling cho Chart (màu sắc hiện đại)
        priceChart.lookup(".chart-plot-background").setStyle("-fx-background-color: transparent;");
        
        // Cấu hình bảng lịch sử giá
        colHistoryRound.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("round"));
        colHistoryPrice.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("price"));
        historyTableView.setItems(historyData);

        // Cấu hình bảng thông báo (CHỈ CỦA PHÒNG NÀY)
        colNotifTime.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("time"));
        colNotifContent.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("content"));
        
        // T10: Cho phép tự động xuống dòng và tự động giãn độ cao dòng trong phòng
        colNotifContent.setCellFactory(tc -> {
            javafx.scene.control.TableCell<network.NotificationManager.NotificationItem, String> cell = new javafx.scene.control.TableCell<>() {
                private final javafx.scene.control.Label label = new javafx.scene.control.Label();
                {
                    label.setWrapText(true);
                    label.prefWidthProperty().bind(tc.widthProperty().subtract(10));
                    setGraphic(label);
                }
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        label.setText(null);
                    } else {
                        label.setText(item);
                    }
                }
            };
            return cell;
        });
        
        notificationTableView.setItems(roomNotifications);
        notificationTableView.setFixedCellSize(-1); // Cho phép hàng tự co giãn theo nội dung

        // Khởi tạo giá trị hiển thị
        if (topBidderLabel != null) {
            topBidderLabel.setText(currentTopBidder);
        }

        // Cấu hình Trục X chỉ hiển thị số nguyên (Lượt đặt giá)
        if (priceChart.getXAxis() instanceof javafx.scene.chart.NumberAxis) {
            javafx.scene.chart.NumberAxis xAxis = (javafx.scene.chart.NumberAxis) priceChart.getXAxis();
            xAxis.setMinorTickCount(0);
            xAxis.setTickUnit(1);
            xAxis.setAutoRanging(true); // Để tự động giãn trục X theo lượt đặt giá
            xAxis.setTickLabelFormatter(new javafx.util.StringConverter<Number>() {
                @Override public String toString(Number object) {
                    if (object.doubleValue() == 0) return "S"; // S đại diện cho Starting
                    return String.format("%.0f", object.doubleValue());
                }
                @Override public Number fromString(String string) { return 0; }
            });
        }

        registerNetworkListeners();
    }

    /**
     * Thiết lập ID phiên đấu giá hiện tại.
     */
    public void setAuction(model.auction.Auction auction) {
        this.auction = auction;
        this.currentAuctionId = auction.getId();
        if (roomIdLabel != null) {
            roomIdLabel.setText("ID phòng: " + currentAuctionId);
        }

        // Tính tổng thời gian còn lại
        if (auction.getEndTime() != null) {
            this.auctionEndTime = auction.getEndTime();
            long seconds = java.time.Duration.between(java.time.LocalDateTime.now(), auctionEndTime).getSeconds();
            this.totalTimeRemaining = (int) Math.max(0, seconds);
            
            javafx.application.Platform.runLater(this::startTotalTimer);
            updateTotalTimeDisplay();
        }

        // === KHÔI PHỤC LỊCH SỬ GIÁ, BIỂU ĐỒ VÀ THÔNG BÁO ===
        Platform.runLater(() -> {
            boolean wasAnimated = priceChart.getAnimated();
            priceChart.setAnimated(false); // Tắt animation để load nhanh và không bị hiệu ứng lạ
            
            priceSeries.getData().clear();
            historyData.clear();
            roomNotifications.clear();
            
            // Lấy lịch sử từ Auction object (đã được load từ DB ở Server)
            java.util.List<model.auction.BidTransaction> history = auction.getBidHistory();
            this.bidCount = 0;

            // 1. Điểm xuất phát (Starting Price - Turn 0)
            priceSeries.getData().add(new XYChart.Data<>(0, auction.getStartingPrice()));
            
            if (history != null && !history.isEmpty()) {
                for (model.auction.BidTransaction tx : history) {
                    this.bidCount++;
                    priceSeries.getData().add(new XYChart.Data<>(bidCount, tx.getBidAmount()));
                    
                    // Cập nhật bảng lịch sử
                    historyData.add(0, new HistoryItem(bidCount, tx.getBidAmount()));
                    
                    // Phục hồi thông báo từ lịch sử phòng
                    String timeStr = tx.getTimestamp().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss"));
                    roomNotifications.add(0, new network.NotificationManager.NotificationItem(
                        "📌 " + tx.getBidder().getUsername() + " đã đặt $" + tx.getBidAmount(), timeStr));
                }
            }

            // Cập nhật thông tin giá cao nhất
            if (auction.getHighestBidder() != null) {
                updateTopBidder(auction.getHighestBidder().getUsername());
                currentHighestPrice = auction.getCurrentPrice();
            } else {
                currentHighestPrice = auction.getStartingPrice();
                updateTopBidder("Chưa có");
            }
            
            updateIncrementDisplay(currentHighestPrice);
            
            // Bật lại animation cho các lượt đặt giá tiếp theo
            javafx.animation.PauseTransition pause = new javafx.animation.PauseTransition(Duration.millis(500));
            pause.setOnFinished(e -> priceChart.setAnimated(wasAnimated));
            pause.play();
        });
        
        updateBalanceDisplay(network.SessionManager.getInstance().getBalance());
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
                    currentHighestPrice = 0;

                    Platform.runLater(this::startTotalTimer);
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

                    if (newPrice > currentHighestPrice) {
                        currentHighestPrice = newPrice;
                        updateTopBidder(topBidder);
                        updateIncrementDisplay(newPrice);
                        
                        // Cập nhật biểu đồ theo lượt đấu (Trục X)
                        bidCount++;
                        Platform.runLater(() -> {
                            // Tắt tạm autoRanging để tránh trục bị nhảy giật cục làm mất hiệu ứng chéo
                            if (priceChart.getXAxis() instanceof NumberAxis) {
                                NumberAxis xAxis = (NumberAxis) priceChart.getXAxis();
                                if (bidCount > 5) { // Chỉ can thiệp khi đã có một số điểm nhất định
                                    xAxis.setAutoRanging(false);
                                    xAxis.setLowerBound(Math.max(0, bidCount - 10)); // Giữ khung nhìn 10 lượt gần nhất
                                    xAxis.setUpperBound(bidCount + 1);
                                }
                            }

                            // Tạo điểm mới. LineChart sẽ nối chéo từ (bidCount-1, oldPrice) sang (bidCount, newPrice)
                            priceSeries.getData().add(new XYChart.Data<>(bidCount, newPrice));
                            
                            // Thêm vào bảng lịch sử giá
                            historyData.add(0, new HistoryItem(bidCount, newPrice));

                            // Thêm vào thông báo phòng (Room-specific)
                            String timeStr = java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss"));
                            roomNotifications.add(0, new network.NotificationManager.NotificationItem(
                                "💰 Lượt #" + bidCount + ": " + topBidder + " đặt $" + newPrice, timeStr));
                        });

                        // Global notification (Account wide)
                        network.NotificationManager.getInstance().addNotification("📢 [" + auction.getItem().getName() + "] - " + topBidder + " vừa đặt $" + newPrice);
                    }
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
                            stopAllTimers();
                        }
                    }
                }
        ));

        totalTimelineTimer.setCycleCount(Timeline.INDEFINITE);
        totalTimelineTimer.play();
    }

    private void stopAllTimers() {
        if (totalTimelineTimer != null) {
            totalTimelineTimer.stop();
            totalTimelineTimer = null;
        }
    }

    private void updateTotalTimeDisplay() {
        Platform.runLater(() -> {
            if (totalTimeLabel != null) {
                totalTimeLabel.setText(formatTime(totalTimeRemaining));
            }
        });
    }

    private void updateTopBidder(String bidderName) {
        currentTopBidder = bidderName;
        Platform.runLater(() -> {
            if (topBidderLabel != null) {
                topBidderLabel.setText(bidderName);
            }
        });
    }

    private String formatTime(int seconds) {
        int minutes = seconds / 60;
        int secs = seconds % 60;
        return String.format("%02d:%02d", minutes, secs);
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

    @FXML
    private void handlePlaceBid() {
        try {
            String bidText = bidAmountField.getText().trim();
            if (view.utility.ValidationHelper.isEmpty(bidText)) return;

            if (currentAuctionId == null) {
                AlertHelper.showWarning("Lỗi", "Chưa xác định được phòng đấu giá!");
                return;
            }

            if (!view.utility.ValidationHelper.isValidStartPrice(bidText)) {
                AlertHelper.showWarning("Lỗi dữ liệu", "Vui lòng nhập số tiền hợp lệ và lớn hơn 0!");
                return;
            }

            double amount = Double.parseDouble(bidText);

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
        
        // GIẢI PHÓNG BỘ NHỚ: Xóa các listener để không bị rò rỉ khi ra/vào phòng nhiều lần
        ClientNetworkManager.getInstance().clearListeners(Protocol.BROADCAST_NEW_BID);
        ClientNetworkManager.getInstance().clearListeners(Protocol.BROADCAST_ROUND_FINISHED);
        ClientNetworkManager.getInstance().clearListeners(Protocol.BROADCAST_TIME_EXTENDED);
        ClientNetworkManager.getInstance().clearListeners(Protocol.BROADCAST_AUCTION_FINISHED);
        ClientNetworkManager.getInstance().clearListeners(Protocol.REQ_BID);
        ClientNetworkManager.getInstance().clearListeners(Protocol.RES_UPDATE_BALANCE);

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
