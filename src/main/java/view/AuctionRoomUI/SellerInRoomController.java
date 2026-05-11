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
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.stage.Stage;
import network.ClientNetworkManager;
import shared.Protocol;
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
    private String currentAuctionId = "AUC-123";
    private double currentHighestPrice = 0;

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
                if (auctionId.equals(this.currentAuctionId)) {
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

                if (auctionId.equals(this.currentAuctionId)) {
                    Platform.runLater(() -> {
                        showAlert("Phiên đấu giá kết thúc!",
                                "Người chiến thắng: " + winner + "\nGiá cuối cùng: " + finalPrice + " $",
                                Alert.AlertType.INFORMATION);
                    });
                }
            }
        });
    }

    /**
     * Thiết lập ID phiên đấu giá mà Seller đang theo dõi.
     * Gọi trước khi chuyển sang màn hình này.
     */
    public void setAuctionId(String auctionId) {
        this.currentAuctionId = auctionId;
        if (lblRoomId != null) {
            lblRoomId.setText("ID phòng: " + auctionId);
        }
    }

    @FXML
    private void exitRoom(ActionEvent event) {
        Stage stage = (Stage) priceChart.getScene().getWindow();
        SceneManager.goToBaseMenu(stage);
    }

    private void showAlert(String title, String content, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
