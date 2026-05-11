package view.SellerUI;

import javafx.application.Platform;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.stage.Stage;
import model.auction.Auction;
import network.ClientNetworkManager;
import shared.Protocol;
import view.utility.SceneManager;

import java.net.URL;
import java.util.ResourceBundle;

/**
 * Controller cho màn hình hiển thị phiên đấu giá vừa tạo của Seller.
 * Hiển thị 1 bảng với phiên đấu giá mới nhất và nút "Theo dõi".
 */
public class SellerAuctionListController implements Initializable {

    @FXML private TableView<Auction> tableMyAuction;
    @FXML private TableColumn<Auction, String> colId;
    @FXML private TableColumn<Auction, String> colName;
    @FXML private TableColumn<Auction, Double> colPrice;
    @FXML private TableColumn<Auction, String> colStatus;
    @FXML private Button btnMonitor;

    private Auction latestAuction = null;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // === CẤU HÌNH CỘT BẢNG ===
        colId.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getId()));
        colName.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getItem().getName()));
        colPrice.setCellValueFactory(cellData -> new SimpleDoubleProperty(cellData.getValue().getCurrentPrice()).asObject());
        colStatus.setCellValueFactory(cellData -> {
            String status = cellData.getValue().getStatus().name();
            String displayStatus = switch (status) {
                case "OPEN" -> "⏳ Chưa bắt đầu";
                case "RUNNING" -> "🔥 Đang diễn ra";
                case "FINISHED" -> "✅ Đã kết thúc";
                case "PAID" -> "💰 Đã thanh toán";
                case "CANCELED" -> "❌ Đã hủy";
                default -> status;
            };
            return new SimpleStringProperty(displayStatus);
        });

        // === LẮNG NGHE DANH SÁCH ĐẤU GIÁ TỪ SERVER ===
        // Khi Server broadcast danh sách mới, lấy phiên mới nhất (cuối danh sách) để hiển thị
        ClientNetworkManager.getInstance().setAuctionListListener((listFromServer) -> {
            if (listFromServer != null && !listFromServer.isEmpty()) {
                // Phiên mới nhất luôn ở cuối danh sách
                latestAuction = listFromServer.get(listFromServer.size() - 1);

                Platform.runLater(() -> {
                    ObservableList<Auction> data = FXCollections.observableArrayList(latestAuction);
                    tableMyAuction.setItems(data);
                    btnMonitor.setDisable(false);
                });
            }
        });

        // === GỬI YÊU CẦU LẤY DANH SÁCH ĐỂ LẤY PHIÊN MỚI NHẤT ===
        ClientNetworkManager.getInstance().sendData(Protocol.REQ_GET_AUCTIONS);
    }

    @FXML
    private void monitorButtonClicked(ActionEvent event) {
        if (latestAuction == null) {
            showAlert("Lỗi", "Không tìm thấy phiên đấu giá!", Alert.AlertType.WARNING);
            return;
        }

        Stage stage = (Stage) tableMyAuction.getScene().getWindow();
        Stage mainStage = (Stage) stage.getOwner();

        // Đóng popup
        stage.close();

        // Chuyển cửa sổ chính sang SellerInRoomView
        if (mainStage != null) {
            SceneManager.goToSellerInRoom(mainStage);
        }
    }

    @FXML
    private void backToMenuClicked(ActionEvent event) {
        Stage stage = (Stage) tableMyAuction.getScene().getWindow();
        Stage mainStage = (Stage) stage.getOwner();

        stage.close();

        if (mainStage != null) {
            SceneManager.goToBaseMenu(mainStage);
        }
    }

    private void showAlert(String title, String content, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
