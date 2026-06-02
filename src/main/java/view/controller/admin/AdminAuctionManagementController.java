package view.controller.admin;

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
import view.utility.display.AlertHelper;
import view.utility.navigation.SceneManager;
import view.utility.display.StatusDisplayHelper;

import java.net.URL;
import java.util.Optional;
import java.util.ResourceBundle;

public class AdminAuctionManagementController implements Initializable {

    @FXML
    private HBox menuBar;
    @FXML
    private TableView<Auction> tableAuctions;
    @FXML
    private TableColumn<Auction, Integer> colSTT;
    @FXML
    private TableColumn<Auction, String> colId;
    @FXML
    private TableColumn<Auction, String> colName;
    @FXML
    private TableColumn<Auction, Double> colPrice;
    @FXML
    private TableColumn<Auction, String> colStatus;
    @FXML
    private TableColumn<Auction, String> colBidder;
    @FXML
    private TableColumn<Auction, Void> colView;
    @FXML
    private TableColumn<Auction, Void> colAction;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        configureColumns(); // cấu hình các cột dữ liệu
        configureViewColumn(); // cấu hình cột nút "Chi tiết"
        configureActionColumn(); // cấu hình cột nút "Xóa"
        registerDeleteListener(); // đăng ký lắng nghe phản hồi xóa
        registerAuctionListListener(); // đăng ký lắng nghe danh sách phiên đấu giá
        loadAuctions(); // tải danh sách phiên đấu giá từ server khi khởi tạo giao diện
    }

    private void loadAuctions() {
        ClientNetworkManager.getInstance().sendData(Protocol.REQ_GET_AUCTIONS);
    }

    private void configureColumns() {
        // === CẤU HÌNH CÁC CỘT BẢNG ===
        // Cột STT: hiển thị số thứ tự dựa trên vị trí của item trong TableView
        colSTT.setCellValueFactory(cellData -> new SimpleIntegerProperty(tableAuctions.getItems().indexOf(cellData.getValue()) + 1).asObject());
        // Cột ID: hiển thị ID phiên đấu giá
        colId.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getId()));
        // Cột Tên Sản Phẩm: hiển thị tên sản phẩm
        colName.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getItem().getName()));
        // Cột Giá Hiện Tại: hiển thị giá hiện tại của phiên đấu giá
        colPrice.setCellValueFactory(cellData -> new SimpleDoubleProperty(cellData.getValue().getCurrentPrice()).asObject());
        // Cột Trạng Thái: hiển thị trạng thái của phiên đấu giá
        colStatus.setCellValueFactory(cellData -> new SimpleStringProperty(StatusDisplayHelper.formatAuctionStatus(cellData.getValue().getStatus().name())));
        // Cột Người Đấu Giá Cao Nhất: hiển thị tên người đấu giá cao nhất hoặc "Chưa có" nếu chưa có ai đặt giá
        colBidder.setCellValueFactory(cellData -> {
            var bidder = cellData.getValue().getHighestBidder();
            return new SimpleStringProperty(bidder != null ? bidder.getUsername() : "Chưa có");
        });
    }

    private void configureViewColumn() {
        colView.setCellFactory(col -> new TableCell<>() {
            private final Button btnView = new Button("👁️ Chi tiết");
            {
                btnView.getStyleClass().add("btn-info");
                btnView.setOnAction(event -> {
                    Auction auction = getTableView().getItems().get(getIndex());
                    Stage stage = (Stage) btnView.getScene().getWindow();
                    view.utility.navigation.WindowManager.openSellerInRoomWindow(auction, stage);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty)
                    setGraphic(null);
                else
                    setGraphic(btnView);
            }
        });
    }

    private void configureActionColumn() {
        colAction.setCellFactory(col -> new TableCell<>() {
            private final Button btnDelete = new Button("🗑️ Xóa");

            {
                btnDelete.getStyleClass().add("btn-danger");
                btnDelete.setOnAction(event -> {
                    Auction auction = getTableView().getItems().get(getIndex());

                    // Xác nhận trước khi xóa (SRP: delegate sang AlertHelper)
                    Optional<ButtonType> result = AlertHelper.showConfirmation("Xác nhận xóa",
                            "Bạn có chắc muốn xóa phiên đấu giá này?\n" + "Mã phiên: " + auction.getId()
                                    + "\nSản phẩm: " + auction.getItem().getName());

                    if (result.isPresent() && result.get() == ButtonType.OK) {
                        String request = Protocol.REQ_DELETE_AUCTION + Protocol.DELIMITER + auction.getId();

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
    }

    private void registerDeleteListener() {
        ClientNetworkManager.getInstance().registerListener(Protocol.REQ_DELETE_AUCTION, (response) -> {
            String[] parts = response.split(Protocol.DELIMITER);
            Platform.runLater(() -> {
                if (parts.length > 1 && parts[1].equals(Protocol.RES_SUCCESS)) {
                    ClientNetworkManager.getInstance().sendData(Protocol.REQ_GET_AUCTIONS);
                    AlertHelper.showInfo("Hệ thống", "Đã xóa phiên đấu giá thành công!");
                } else {
                    AlertHelper.showError("Lỗi", parts.length >= 3 ? parts[2] : "Không thể xóa phiên đấu giá");
                }
            });
        });
    }

    private void registerAuctionListListener() {
        ClientNetworkManager.getInstance().clearAuctionListListeners();
        ClientNetworkManager.getInstance().addAuctionListListener((listFromServer) -> {
            if (listFromServer != null) {
                Platform.runLater(() -> {
                    ObservableList<Auction> data = FXCollections.observableArrayList(listFromServer);
                    tableAuctions.setItems(data);
                    System.out.println("✅ [Admin] Đã cập nhật danh sách " + listFromServer.size() + " phiên đấu giá.");
                });
            }
        });
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
        ClientNetworkManager.getInstance().logout();
        Stage stage = (Stage) menuBar.getScene().getWindow();
        SceneManager.goToLogin(stage);
    }

    @FXML
    private void refreshData(ActionEvent event) {
        ClientNetworkManager.getInstance().sendData(Protocol.REQ_GET_AUCTIONS);
    }
}
