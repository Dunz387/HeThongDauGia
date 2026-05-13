package view.AuctionRoomUI;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.Button;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import model.auction.Auction;
import view.utility.AuctionNetworkHelper;
import view.utility.AuctionTableConfigurator;
import view.utility.SceneManager;

import java.net.URL;
import java.util.ResourceBundle;

public class RoomMenuChoiceController implements Initializable {

    @FXML private TableView<Auction> tableAuctions;
    @FXML private TableColumn<Auction, String> colId;
    @FXML private TableColumn<Auction, String> colName;
    @FXML private TableColumn<Auction, String> colDescription;
    @FXML private TableColumn<Auction, String> colType;
    @FXML private TableColumn<Auction, Double> colPrice;
    @FXML private TableColumn<Auction, Integer> colBidCount;
    @FXML private TableColumn<Auction, String> colHighestBidder;
    @FXML private TableColumn<Auction, String> colEndTime;
    @FXML private TableColumn<Auction, String> colStatus;
    @FXML private TableColumn<Auction, String> colSeller;

    @FXML private HBox sellerActionBox;
    @FXML private Button btnCreateAuction;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        if (!network.SessionManager.getInstance().isSeller()) {
            if (sellerActionBox != null) {
                sellerActionBox.setVisible(false);
                sellerActionBox.setManaged(false);
            }
        }
        // Cấu hình bảng thống nhất (SRP: delegate sang AuctionTableConfigurator)
        AuctionTableConfigurator.configure(colId, colName, colDescription, colType, colPrice,
                colBidCount, colHighestBidder, colEndTime, colStatus, colSeller);

        // Nhấp đúp để vào phòng đấu giá
        tableAuctions.setRowFactory(tv -> {
            TableRow<Auction> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && (!row.isEmpty())) {
                    Auction selectedAuction = row.getItem();
                    System.out.println("🏛️ Đang vào phòng đấu giá: " + selectedAuction.getId());
                    Stage currentStage = (Stage) tableAuctions.getScene().getWindow();
                    
                    if (network.SessionManager.getInstance().isBidder() || network.SessionManager.getInstance().isAdmin()) {
                        SceneManager.goToInRoom(currentStage, selectedAuction.getId());
                    } else if (network.SessionManager.getInstance().isSeller()) {
                        if (selectedAuction.getItem().getOwner() != null && selectedAuction.getItem().getOwner().getId().equals(network.SessionManager.getInstance().getUserId())) {
                            SceneManager.goToSellerInRoom(currentStage, selectedAuction.getId());
                        } else {
                            view.utility.AlertHelper.showWarning("Cảnh báo", "Bạn chỉ có thể xem phòng đấu giá của chính mình!");
                        }
                    }
                }
            });
            return row;
        });

        // Đăng ký lắng nghe với bộ lọc theo Role
        String currentUserId = network.SessionManager.getInstance().getUserId();
        boolean isAdmin = network.SessionManager.getInstance().isAdmin();
        boolean isSeller = network.SessionManager.getInstance().isSeller();

        AuctionNetworkHelper.registerAuctionListListener(tableAuctions, a -> {
            if (isAdmin) return true; // Admin thấy tất cả
            if (isSeller) {
                // Seller chỉ hiện phòng đấu giá của mình thôi (đang diễn ra)
                return a.getItem().getOwner() != null && a.getItem().getOwner().getId().equals(currentUserId) 
                       && "RUNNING".equals(a.getStatus().name());
            }
            // Bidder (Everyone else): Nhìn được everyone room (đang diễn ra)
            return "RUNNING".equals(a.getStatus().name());
        });

        // Thêm Context Menu cho tính năng Sửa/Xóa của Seller/Admin
        setupContextMenu();
    }

    private void setupContextMenu() {
        javafx.scene.control.ContextMenu contextMenu = new javafx.scene.control.ContextMenu();
        javafx.scene.control.MenuItem editItem = new javafx.scene.control.MenuItem("Sửa thông tin phiên");
        javafx.scene.control.MenuItem deleteItem = new javafx.scene.control.MenuItem("Xóa phiên đấu giá");

        if (network.SessionManager.getInstance().isBidder()) return;

        editItem.setOnAction(e -> {
            Auction selected = tableAuctions.getSelectionModel().getSelectedItem();
            if (selected != null) {
                boolean isAdmin = network.SessionManager.getInstance().isAdmin();
                boolean isOwner = selected.getItem().getOwner() != null && 
                                 selected.getItem().getOwner().getId().equals(network.SessionManager.getInstance().getUserId());

                if (!isAdmin && !isOwner) {
                    view.utility.AlertHelper.showWarning("Cảnh báo", "Bạn chỉ có thể sửa thông tin của chính mình!");
                    return;
                }

                javafx.scene.control.Dialog<String> dialog = new javafx.scene.control.Dialog<>();
                dialog.setTitle("Sửa phiên đấu giá");
                dialog.setHeaderText("Chỉnh sửa thông tin phiên: " + selected.getId());
                javafx.scene.control.ButtonType updateButtonType = new javafx.scene.control.ButtonType("Cập nhật", javafx.scene.control.ButtonBar.ButtonData.OK_DONE);
                dialog.getDialogPane().getButtonTypes().addAll(updateButtonType, javafx.scene.control.ButtonType.CANCEL);

                javafx.scene.layout.GridPane grid = new javafx.scene.layout.GridPane();
                grid.setHgap(10); grid.setVgap(10);
                grid.setPadding(new javafx.geometry.Insets(20, 150, 10, 10));

                javafx.scene.control.TextField nameField = new javafx.scene.control.TextField(selected.getItem().getName());
                javafx.scene.control.TextArea descField = new javafx.scene.control.TextArea(selected.getItem().getDescription());
                descField.setPrefRowCount(3);
                javafx.scene.control.ComboBox<String> typeBox = new javafx.scene.control.ComboBox<>(javafx.collections.FXCollections.observableArrayList("ELECTRONICS", "ART", "VEHICLE"));
                String currentType = "ELECTRONICS";
                if (selected.getItem() instanceof model.item.Arts) currentType = "ART";
                else if (selected.getItem() instanceof model.item.Vehicle) currentType = "VEHICLE";
                typeBox.setValue(currentType);

                javafx.scene.control.TextField priceField = new javafx.scene.control.TextField(String.valueOf(selected.getStartingPrice()));
                priceField.setDisable(!isAdmin);

                javafx.scene.control.TextField timeField = new javafx.scene.control.TextField(String.valueOf(java.time.Duration.between(java.time.LocalDateTime.now(), selected.getEndTime()).toMinutes()));
                timeField.setDisable(!isAdmin);

                grid.add(new javafx.scene.control.Label("Tên sản phẩm:"), 0, 0); grid.add(nameField, 1, 0);
                grid.add(new javafx.scene.control.Label("Mô tả:"), 0, 1); grid.add(descField, 1, 1);
                grid.add(new javafx.scene.control.Label("Loại:"), 0, 2); grid.add(typeBox, 1, 2);
                grid.add(new javafx.scene.control.Label("Giá khởi điểm ($):"), 0, 3); grid.add(priceField, 1, 3);
                grid.add(new javafx.scene.control.Label("Thời gian còn lại (phút):"), 0, 4); grid.add(timeField, 1, 4);

                dialog.getDialogPane().setContent(grid);
                dialog.setResultConverter(btn -> btn == updateButtonType ? shared.Protocol.REQ_UPDATE_ITEM + shared.Protocol.DELIMITER + 
                        selected.getId() + shared.Protocol.DELIMITER + nameField.getText() + shared.Protocol.DELIMITER + 
                        descField.getText().replace("\n", " ") + shared.Protocol.DELIMITER + typeBox.getValue() + shared.Protocol.DELIMITER + 
                        priceField.getText() + shared.Protocol.DELIMITER + timeField.getText() : null);

                dialog.showAndWait().ifPresent(cmd -> network.ClientNetworkManager.getInstance().sendData(cmd));
            }
        });

        deleteItem.setOnAction(e -> {
            Auction selected = tableAuctions.getSelectionModel().getSelectedItem();
            if (selected != null) {
                boolean isAdmin = network.SessionManager.getInstance().isAdmin();
                boolean isOwner = selected.getItem().getOwner() != null && 
                                 selected.getItem().getOwner().getId().equals(network.SessionManager.getInstance().getUserId());

                if (!isAdmin && !isOwner) {
                    view.utility.AlertHelper.showWarning("Cảnh báo", "Bạn chỉ có thể xóa phiên của chính mình!");
                    return;
                }

                javafx.scene.control.Alert confirm = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.CONFIRMATION);
                confirm.setTitle("Xác nhận Xóa");
                confirm.setHeaderText("Bạn có chắc muốn xóa phiên đấu giá này không?");
                confirm.showAndWait().ifPresent(res -> {
                    if (res == javafx.scene.control.ButtonType.OK) {
                        String deleteCommand = (isAdmin ? shared.Protocol.REQ_DELETE_AUCTION : shared.Protocol.REQ_DELETE_ITEM) 
                                             + shared.Protocol.DELIMITER + selected.getId();
                        network.ClientNetworkManager.getInstance().sendData(deleteCommand);
                    }
                });
            }
        });

        contextMenu.getItems().addAll(editItem, deleteItem);
        tableAuctions.setContextMenu(contextMenu);
    }

    @FXML
    private void backToBaseMenu(ActionEvent event) {
        Stage stage = (Stage) ((javafx.scene.Node) event.getSource()).getScene().getWindow();
        SceneManager.goToBaseMenu(stage);
    }

    @FXML
    private void goToCreateItem(ActionEvent event) {
        Stage stage = (Stage) ((javafx.scene.Node) event.getSource()).getScene().getWindow();
        view.utility.WindowManager.openCreateItemWindow(stage);
    }
}
