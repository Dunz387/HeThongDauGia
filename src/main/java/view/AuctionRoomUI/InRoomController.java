package view.AuctionRoomUI;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.animation.Timeline;
import javafx.animation.KeyFrame;
import javafx.util.Duration;
import shared.Protocol; // Import class Protocol của bạn

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
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

    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    
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
        
        // Timers sẽ được khởi tạo sau khi nhận thời gian từ server

        connectToServerAndListen();
    }

    private void connectToServerAndListen() {
        Thread listenThread = new Thread(() -> {
            try {
                socket = new Socket("127.0.0.1", 8080);
                out = new ObjectOutputStream(socket.getOutputStream());
                out.flush();
                in = new ObjectInputStream(socket.getInputStream());

                System.out.println("[Client] Đã vào phòng đấu giá, bắt đầu lắng nghe...");

                Object serverData;
                while ((serverData = in.readObject()) != null) {
                    if (serverData instanceof String) {
                        String message = (String) serverData;

                        // Dùng Protocol.SEPARATOR để cắt chuỗi
                        String[] parts = message.split(Protocol.SEPARATOR);
                        String command = parts[0];

                        // Khởi tạo phiên đấu giá với tổng thời gian
                        if (command.equals(Protocol.BROADCAST_AUCTION_START)) {
                            String auctionId = parts[1];
                            int durationMinutes = Integer.parseInt(parts[2]); // Thời gian tổng tính bằng phút
                            
                            if (auctionId.equals(this.currentAuctionId)) {
                                totalTimeRemaining = durationMinutes * 60; // Chuyển sang giây
                                roundTimeRemaining = ROUND_DURATION;
                                currentRoundHighestPrice = 0;
                                roundWinner = "";
                                
                                startTotalTimer();
                                startRoundTimer();
                                
                                System.out.println("[Auction] Phiên đấu giá bắt đầu. Tổng thời gian: " + durationMinutes + " phút");
                            }
                        }
                        // Cập nhật giá mới từ người khác
                        else if (command.equals(Protocol.BROADCAST_NEW_BID)) {
                            String auctionId = parts[1];

                            // Chỉ cập nhật biểu đồ nếu giá mới thuộc về đúng phòng đấu giá này
                            if (auctionId.equals(this.currentAuctionId)) {
                                double newPrice = Double.parseDouble(parts[2]);
                                String topBidder = parts[3];
                                
                                // Cập nhật người đấu giá cao nhất của vòng
                                if (newPrice > currentRoundHighestPrice) {
                                    currentRoundHighestPrice = newPrice;
                                    roundWinner = topBidder;
                                    updateTopBidder(topBidder);
                                }
                                
                                // Reset thời gian lượt khi có lệnh mới
                                resetRoundTimer();

                                updateChartRealTime(newPrice, topBidder);
                            }
                        }
                        // Tất cả người dùng đã chọn xong giá - kết thúc vòng
                        else if (command.equals(Protocol.BROADCAST_ROUND_FINISHED)) {
                            String auctionId = parts[1];
                            
                            if (auctionId.equals(this.currentAuctionId)) {
                                Platform.runLater(() -> {
                                    System.out.println("[Round] Vòng kết thúc! Người chiến thắng: " + roundWinner + " với giá: " + currentRoundHighestPrice);
                                    // Ghi nhận vào biểu đồ cuối vòng
                                    if (roundWinner.isEmpty()) {
                                        roundWinner = "Không có ai";
                                    }
                                    priceSeries.getData().add(new XYChart.Data<>("Vòng " + bidCount, currentRoundHighestPrice));
                                });
                                
                                // Reset cho vòng mới
                                resetRoundForNewBidding();
                            }
                        }
                        // Phiên đấu giá kết thúc
                        else if (command.equals(Protocol.BROADCAST_AUCTION_FINISHED)) {
                            String auctionId = parts[1];
                            String finalWinner = parts.length > 2 ? parts[2] : "Không có";
                            double finalPrice = parts.length > 3 ? Double.parseDouble(parts[3]) : 0;
                            
                            if (auctionId.equals(this.currentAuctionId)) {
                                stopAllTimers();
                                Platform.runLater(() -> {
                                    System.out.println("[Auction] Phiên đấu giá kết thúc!");
                                    System.out.println("[Result] Người chiến thắng: " + finalWinner + " với giá: " + finalPrice);
                                });
                            }
                        }
                    }
                }
            } catch (Exception e) {
                System.out.println("Đã ngắt kết nối với server phòng đấu giá.");
            }
        });

        listenThread.setDaemon(true);
        listenThread.start();
    }

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
        roundTimeRemaining = ROUND_DURATION; // Reset về 1m30s
        if (roundTimelineTimer != null) {
            roundTimelineTimer.stop();
        }
        startRoundTimer();
    }

    private void resetRoundForNewBidding() {
        // Ghi nhận kết quả vòng trước vào biểu đồ
        if (currentRoundHighestPrice > 0) {
            bidCount++;
            Platform.runLater(() -> {
                priceSeries.getData().add(new XYChart.Data<>(String.valueOf(bidCount), currentRoundHighestPrice));
                if (priceSeries.getData().size() > 20) {
                    priceSeries.getData().remove(0);
                }
            });
        }
        
        // Reset các biến cho vòng mới
        currentRoundHighestPrice = 0;
        roundWinner = "";
        roundTimeRemaining = ROUND_DURATION;
        
        // Bắt đầu lại timer vòng mới
        if (totalTimeRemaining > 0 && roundTimelineTimer != null) {
            roundTimelineTimer.stop();
            startRoundTimer();
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
                topBidderLabel.setText("Người cao nhất hiện tại:\n" + bidderName);
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

    // Hàm gọi khi người dùng bấm nút "Xác nhận đấu giá"
    @FXML
    private void handlePlaceBid() {
        try {
            if (bidAmountField.getText().isEmpty()) return;
            double amount = Double.parseDouble(bidAmountField.getText());

            // Dùng Protocol.REQ_BID và Protocol.DELIMITER để đóng gói bản tin gửi đi
            String request = Protocol.REQ_BID + Protocol.DELIMITER +
                             currentAuctionId + Protocol.DELIMITER +
                             amount;

            out.writeObject(request);
            out.flush();

            // Xóa rỗng ô nhập sau khi gửi
            bidAmountField.clear();

        } catch (NumberFormatException e) {
            System.out.println("Lỗi: Vui lòng nhập số tiền hợp lệ!");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}