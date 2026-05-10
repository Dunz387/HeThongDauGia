package view.AuctionRoomUI;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.TextField;
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

    private XYChart.Series<String, Number> priceSeries;
    private int bidCount = 0; 
    
    // ID của phiên đấu giá hiện tại (Nên được truyền vào khi chuyển màn hình)
    private String currentAuctionId = "AUC-123"; 

    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        priceSeries = new XYChart.Series<>();
        priceSeries.setName("Lịch sử biến động giá");
        priceChart.getData().add(priceSeries);
        priceChart.setAnimated(false); 

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

                        // So sánh lệnh với Protocol.BROADCAST_NEW_BID
                        if (command.equals(Protocol.BROADCAST_NEW_BID)) { 
                            String auctionId = parts[1]; 
                            
                            // Chỉ cập nhật biểu đồ nếu giá mới thuộc về đúng phòng đấu giá này
                            if (auctionId.equals(this.currentAuctionId)) {
                                double newPrice = Double.parseDouble(parts[2]);
                                String topBidder = parts[3];

                                updateChartRealTime(newPrice, topBidder);
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

    private void updateChartRealTime(double newPrice, String topBidder) {
        Platform.runLater(() -> {
            bidCount++; 
            priceSeries.getData().add(new XYChart.Data<>(String.valueOf(bidCount), newPrice));

            if (priceSeries.getData().size() > 20) {
                priceSeries.getData().remove(0);
            }
            
            System.out.println("Đã vẽ giá mới lên biểu đồ: " + newPrice + "$ bởi " + topBidder);
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