package view.AuctionRoomUI;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.animation.Timeline;
import javafx.animation.KeyFrame;
import javafx.stage.Stage;
import javafx.util.Duration;
import network.ClientNetworkManager;
import shared.Protocol;
import view.utility.AlertHelper;
import view.utility.SceneManager;

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

    private XYChart.Series<String, Number> priceSeries;
    private int bidCount = 0;

    // ID của phiên đấu giá hiện tại (Nên được truyền vào khi chuyển màn hình)
    private String currentAuctionId = "AUC-123";

    // Quản lý thời gian
    private Timeline totalTimelineTimer;
    private Timeline roundTimelineTimer;
    private int totalTimeRemaining = 0; // Tính bằng giây (lấy từ duration)
    private static final int ROUND_DURATION = 90; // Thời gian mỗi vòng: 1m30s = 90 giây
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
        
        // Khởi tạo giá trị hiển thị
        if (topBidderLabel != null) {
            topBidderLabel.setText(currentTopBidder);
        }

        // === SỬ DỤNG CLIENTNETWORKMANAGER ĐỂ LẮNG NGHE (thay vì tạo socket riêng) ===
        registerNetworkListeners();
    }

    private void registerNetworkListeners() {
        // Lắng nghe lệnh BROADCAST_AUCTION_START
        ClientNetworkManager.getInstance().registerListener(Protocol.BROADCAST_AUCTION_START, (message) -> {
            String[] parts = message.split(Protocol.SEPARATOR);
            if (parts.length >= 3) {
                String auctionId = parts[1];
                int durationMinutes = Integer.parseInt(parts[2]);

                if (auctionId.equals(this.currentAuctionId)) {
                    totalTimeRemaining = durationMinutes * 60;
                    roundTimeRemaining = ROUND_DURATION;
                    currentRoundHighestPrice = 0;
                    roundWinner = "";

                    Platform.runLater(() -> {
                        startTotalTimer();
                        startRoundTimer();
                    });

                    System.out.println("[Auction] Phiên đấu giá bắt đầu. Tổng thời gian: " + durationMinutes + " phút");
                }
            }
        });

        // Lắng nghe lệnh BROADCAST_NEW_BID (Cập nhật giá mới từ người khác)
        ClientNetworkManager.getInstance().registerListener(Protocol.BROADCAST_NEW_BID, (message) -> {
            String[] parts = message.split(Protocol.SEPARATOR);
            if (parts.length >= 4) {
                String auctionId = parts[1];

                if (auctionId.equals(this.currentAuctionId)) {
                    double newPrice = Double.parseDouble(parts[2]);
                    String topBidder = parts[3];

                    if (newPrice > currentRoundHighestPrice) {
                        currentRoundHighestPrice = newPrice;
                        roundWinner = topBidder;
                        updateTopBidder(topBidder);
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

                if (auctionId.equals(this.currentAuctionId)) {
                    Platform.runLater(() -> {
                        System.out.println("[Round] Vòng kết thúc! Người chiến thắng: " + roundWinner + " với giá: " + currentRoundHighestPrice);
                        if (roundWinner.isEmpty()) {
                            roundWinner = "Không có ai";
                        }
                        priceSeries.getData().add(new XYChart.Data<>("Vòng " + bidCount, currentRoundHighestPrice));
                    });

                    resetRoundForNewBidding();
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

                if (auctionId.equals(this.currentAuctionId)) {
                    Platform.runLater(() -> {
                        stopAllTimers();
                        System.out.println("[Auction] Phiên đấu giá kết thúc!");
                        System.out.println("[Result] Người chiến thắng: " + finalWinner + " với giá: " + finalPrice);
                        AlertHelper.showInfo("Kết quả đấu giá",
                                "Người chiến thắng: " + finalWinner + "\nGiá cuối: $" + finalPrice);
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
    }

    // === CÁC HÀM QUẢN LÝ TIMER ===

    private void startTotalTimer() {
        if (totalTimelineTimer != null) {
            totalTimelineTimer.stop();
        }

        totalTimelineTimer = new Timeline(new KeyFrame(
                Duration.seconds(1),
                event -> {
                    if (totalTimeRemaining > 0) {
                        totalTimeRemaining--;
                        updateTotalTimeDisplay();
                    } else {
                        System.out.println("[Timer] Phiên đấu giá hết thời gian!");
                        stopAllTimers();
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
        if (currentRoundHighestPrice > 0) {
            bidCount++;
            Platform.runLater(() -> {
                priceSeries.getData().add(new XYChart.Data<>(String.valueOf(bidCount), currentRoundHighestPrice));
                if (priceSeries.getData().size() > 20) {
                    priceSeries.getData().remove(0);
                }
            });
        }

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

    // === XỬ LÝ SỰ KIỆN TỪ GIAO DIỆN ===

    @FXML
    private void handlePlaceBid() {
        try {
            if (bidAmountField.getText().isEmpty()) return;
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
}