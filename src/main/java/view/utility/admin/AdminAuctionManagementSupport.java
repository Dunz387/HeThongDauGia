package view.utility.admin;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.stage.Stage;
import model.auction.Auction;
import network.client.ClientNetworkManager;
import shared.Protocol;
import view.utility.display.AlertHelper;
import view.utility.navigation.WindowManager;

import java.util.Optional;

public class AdminAuctionManagementSupport {
    private final TableView<Auction> tableAuctions;
    private final TableColumn<Auction, Void> viewColumn;
    private final TableColumn<Auction, Void> actionColumn;

    public AdminAuctionManagementSupport(
            TableView<Auction> tableAuctions,
            TableColumn<Auction, Void> viewColumn,
            TableColumn<Auction, Void> actionColumn
    ) {
        this.tableAuctions = tableAuctions;
        this.viewColumn = viewColumn;
        this.actionColumn = actionColumn;
    }

    public void configureActionColumns() {
        configureViewColumn();
        configureDeleteColumn();
    }

    public void registerListeners() {
        registerDeleteListener();
        ClientNetworkManager.getInstance().clearAuctionListListeners();
        ClientNetworkManager.getInstance().addAuctionListListener(listFromServer -> {
            if (listFromServer != null) {
                Platform.runLater(() -> tableAuctions.setItems(FXCollections.observableArrayList(listFromServer)));
            }
        });
    }

    public void load() {
        ClientNetworkManager.getInstance().sendData(Protocol.REQ_GET_AUCTIONS);
    }

    private void configureViewColumn() {
        viewColumn.setCellFactory(col -> new TableCell<>() {
            private final Button viewButton = new Button("👁️ Chi tiết");

            {
                viewButton.getStyleClass().add("btn-info");
                viewButton.setOnAction(event -> {
                    Auction auction = getTableView().getItems().get(getIndex());
                    Stage stage = (Stage) viewButton.getScene().getWindow();
                    WindowManager.openSellerInRoomWindow(auction, stage);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : viewButton);
            }
        });
    }

    private void configureDeleteColumn() {
        actionColumn.setCellFactory(col -> new TableCell<>() {
            private final Button deleteButton = new Button("🗑️ Xóa");

            {
                deleteButton.getStyleClass().add("btn-danger");
                deleteButton.setOnAction(event -> confirmAndDelete(getTableView().getItems().get(getIndex())));
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty || getIndex() >= getTableView().getItems().size() ? null : deleteButton);
            }
        });
    }

    private void confirmAndDelete(Auction auction) {
        Optional<ButtonType> result = AlertHelper.showConfirmation(
                "Xác nhận xóa",
                "Bạn có chắc muốn xóa phiên đấu giá này?\n"
                        + "Mã phiên: " + auction.getId()
                        + "\nSản phẩm: " + auction.getItem().getName()
        );

        if (result.isPresent() && result.get() == ButtonType.OK) {
            ClientNetworkManager.getInstance().sendData(
                    Protocol.REQ_DELETE_AUCTION + Protocol.DELIMITER + auction.getId());
        }
    }

    private void registerDeleteListener() {
        ClientNetworkManager.getInstance().registerListener(Protocol.REQ_DELETE_AUCTION, response -> {
            String[] parts = response.split(Protocol.DELIMITER);
            Platform.runLater(() -> {
                if (parts.length > 1 && Protocol.RES_SUCCESS.equals(parts[1])) {
                    load();
                    AlertHelper.showInfo("Hệ thống", "Đã xóa phiên đấu giá thành công!");
                } else {
                    AlertHelper.showError("Lỗi", parts.length >= 3 ? parts[2] : "Không thể xóa phiên đấu giá");
                }
            });
        });
    }
}
