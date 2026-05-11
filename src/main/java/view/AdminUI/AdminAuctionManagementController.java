package view.AdminUI;

import javafx.application.Platform;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import model.auction.Auction;
import network.ClientNetworkManager;
import shared.Protocol;
import view.utility.SceneManager;

import java.net.URL;
import java.util.Optional;
import java.util.ResourceBundle;

public class AdminAuctionManagementController implements Initializable {

    @FXML private HBox menuBar;
    @FXML private TableView<Auction> tableAuctions;
    @FXML private TableColumn<Auction, Integer> colSTT;
    @FXML private TableColumn<Auction, String> colId;
    @FXML private TableColumn<Auction, String> colName;
    @FXML private TableColumn<Auction, Double> colPrice;
    @FXML private TableColumn<Auction, String> colStatus;
    @FXML private TableColumn<Auction, String> colBidder;
    @FXML private TableColumn<Auction, Void> colAction;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // === CẤU HÌNH CÁC CỘT BẢNG ===
        colSTT.setCellValueFactory(cellData ->
                new SimpleIntegerProperty(tableAuctions.getItems().indexOf(cellData.getValue()) + 1).asObject());
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
        colBidder.setCellValueFactory(cellData -> {
            var bidder = cellData.getValue().getHighestBidder();
            return new SimpleStringProperty(bidder != null ? bidder.getUsername() : "Chưa có");
        });

        // === CỘT HÀNH ĐỘNG: NÚT XÓA PHIÊN ĐẤU GIÁ ===
        colAction.setCellFactory(col -> new TableCell<>() {
            private final Button btnDelete = new Button("🗑️ Xóa");

            {
                btnDelete.getStyleClass().add("btn-danger");
                btnDelete.setOnAction(event -> {
                    Auction auction = getTableView().getItems().get(getIndex());

                    // Xác nhận trước khi xóa
                    Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
                    confirmAlert.setTitle("Xác nhận xóa");
                    confirmAlert.setHeaderText("Bạn có chắc muốn xóa phiên đấu giá này?");
                    confirmAlert.setContentText("Mã phiên: " + auction.getId() + "\nSản phẩm: " + auction.getItem().getName());

                    Optional<ButtonType> result = confirmAlert.showAndWait();
                    if (result.isPresent() && result.get() == ButtonType.OK) {
                        String request = Protocol.REQ_DELETE_AUCTION + Protocol.DELIMITER + auction.getId();

                        // ĐĂNG KÝ CALLBACK
                        ClientNetworkManager.getInstance().registerListener(Protocol.REQ_DELETE_AUCTION, (response) -> {
                            String[] parts = response.split(Protocol.SEPARATOR);
                            Platform.runLater(() -> {
                                if (parts.length > 1 && parts[1].equals(Protocol.RES_SUCCESS)) {
                                    showAlert("Thành công", "Đã xóa phiên đấu giá: " + auction.getId(), Alert.AlertType.INFORMATION);
                                } else {
                                    showAlert("Thất bại",
                                            parts.length >= 3 ? parts[2] : "Lỗi không xác định",
                                            Alert.AlertType.ERROR);
                                }
                            });
                        });

                        ClientNetworkManager.getInstance().sendData(request);
                    }
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getIndex() >= getTableView().getItems().size()) {
                    setGraphic(null);
                } else {
                    setGraphic(btnDelete);
                }
            }
        });

        // === LẮNG NGHE DANH SÁCH ĐẤU GIÁ TỪ SERVER (REAL-TIME) ===
        ClientNetworkManager.getInstance().setAuctionListListener((listFromServer) -> {
            if (listFromServer != null) {
                Platform.runLater(() -> {
                    ObservableList<Auction> data = FXCollections.observableArrayList(listFromServer);
                    tableAuctions.setItems(data);
                    System.out.println("✅ [Admin] Đã cập nhật danh sách " + listFromServer.size() + " phiên đấu giá.");
                });
            }
        });

        // === GỬI YÊU CẦU LẤY DANH SÁCH KHI VÀO TRANG ===
        ClientNetworkManager.getInstance().sendData(Protocol.REQ_GET_AUCTIONS);
    }

    // === ĐIỀU HƯỚNG ===

    @FXML
    private void goToDashboard(ActionEvent event) {
        Stage stage = (Stage) menuBar.getScene().getWindow();
        SceneManager.goToAdminDashboard(stage);
    }

    @FXML
    private void goToUserManagement(ActionEvent event) {
        Stage stage = (Stage) menuBar.getScene().getWindow();
        SceneManager.goToAdminUserManagement(stage);
    }

    @FXML
    private void logoutClicked(ActionEvent event) {
        Stage stage = (Stage) menuBar.getScene().getWindow();
        SceneManager.goToLogin(stage);
    }

    @FXML
    private void refreshData(ActionEvent event) {
        ClientNetworkManager.getInstance().sendData(Protocol.REQ_GET_AUCTIONS);
    }

    private void showAlert(String title, String content, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
