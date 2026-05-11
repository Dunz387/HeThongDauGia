package view.AuctionRoomUI;

import javafx.application.Platform;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.stage.Stage;
import model.auction.Auction;
import network.ClientNetworkManager;
import shared.Protocol;
import view.utility.SceneManager;

import java.net.URL;
import java.util.ResourceBundle;

public class RoomMenuChoiceController implements Initializable {

    @FXML private TableView<Auction> tableAuctions;
    @FXML private TableColumn<Auction, String> colId;
    @FXML private TableColumn<Auction, String> colName;
    @FXML private TableColumn<Auction, Double> colPrice;
    @FXML private TableColumn<Auction, String> colStatus;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // === CẤU HÌNH CÁC CỘT BẢNG ===
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

        // === NHẤP ĐÚP ĐỂ VÀO PHÒNG ĐẤU GIÁ ===
        tableAuctions.setRowFactory(tv -> {
            TableRow<Auction> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && (!row.isEmpty())) {
                    Auction selectedAuction = row.getItem();
                    System.out.println("🏛️ Đang vào phòng đấu giá: " + selectedAuction.getId());
                    Stage currentStage = (Stage) tableAuctions.getScene().getWindow();
                    SceneManager.goToInRoom(currentStage);
                }
            });
            return row;
        });

        // === LẮNG NGHE DANH SÁCH ĐẤU GIÁ TỪ SERVER (REAL-TIME) ===
        ClientNetworkManager.getInstance().setAuctionListListener((listFromServer) -> {
            if (listFromServer != null) {
                // Chỉ hiển thị các phiên đang RUNNING
                var runningList = listFromServer.stream()
                        .filter(a -> "RUNNING".equals(a.getStatus().name()))
                        .toList();
                ObservableList<Auction> data = FXCollections.observableArrayList(runningList);
                Platform.runLater(() -> tableAuctions.setItems(data));
            }
        });

        // === GỬI YÊU CẦU LẤY DANH SÁCH KHI VÀO TRANG ===
        ClientNetworkManager.getInstance().sendData(Protocol.REQ_GET_AUCTIONS);
    }

    @FXML
    private void backToBaseMenu(ActionEvent event) {
        Stage stage = (Stage) ((javafx.scene.Node) event.getSource()).getScene().getWindow();
        SceneManager.goToBaseMenu(stage);
    }
}
