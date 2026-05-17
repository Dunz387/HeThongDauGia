package view.BaseMenuUI;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;
import model.auction.Auction;
import view.utility.AuctionNetworkHelper;
import view.utility.AuctionTableConfigurator;
import view.utility.NotificationMenuHandler;
import view.utility.SceneManager;
import view.utility.WindowManager;
import javafx.scene.control.TextInputDialog;
import view.utility.AlertHelper;
import java.util.Optional;

import java.net.URL;
import java.util.ResourceBundle;

public class AssetsListController implements Initializable {
    @FXML private Pane darkOverlay;
    @FXML private ScrollPane notificationMenu;
    private NotificationMenuHandler notificationMenuHandler;

    @FXML private HBox menuBar;
    @FXML private javafx.scene.control.Label txtBalance;
    @FXML private javafx.scene.control.Button btnTransaction;

    @FXML private TableView<Auction> tableAssets;
    @FXML private TableColumn<Auction, String> colId;
    @FXML private TableColumn<Auction, String> colItemName;
    @FXML private TableColumn<Auction, String> colDescription;
    @FXML private TableColumn<Auction, String> colType;
    @FXML private TableColumn<Auction, Double> colPrice;
    @FXML private TableColumn<Auction, Integer> colBidCount;
    @FXML private TableColumn<Auction, String> colHighestBidder;
    @FXML private TableColumn<Auction, String> colEndTime;
    @FXML private TableColumn<Auction, String> colStatus;
    @FXML private TableColumn<Auction, String> colSeller;

    // Notifications
    @FXML private TableView<network.NotificationManager.NotificationItem> tableNotifications;
    @FXML private TableColumn<network.NotificationManager.NotificationItem, String> colNotifContent;
    @FXML private TableColumn<network.NotificationManager.NotificationItem, String> colNotifTime;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Cập nhật số dư realtime bằng cách lắng nghe SessionManager
        if (txtBalance != null) {
            network.SessionManager.getInstance().balanceProperty().addListener((obs, oldVal, newVal) -> {
                javafx.application.Platform.runLater(() -> {
                    txtBalance.setText(String.format("💰 Số dư: $%,.0f", newVal.doubleValue()));
                });
            });
            // Initial value
            txtBalance.setText(String.format("💰 Số dư: $%,.0f", network.SessionManager.getInstance().getBalance()));
        }

        if (btnTransaction != null) {
            if (network.SessionManager.getInstance().isBidder()) {
                btnTransaction.setText("Nạp tiền");
            } else if (network.SessionManager.getInstance().isSeller()) {
                btnTransaction.setText("Rút tiền");
            } else {
                btnTransaction.setVisible(false);
                btnTransaction.setManaged(false);
            }
        }

        // Cấu hình bảng thống nhất (SRP: delegate sang AuctionTableConfigurator)
        AuctionTableConfigurator.configure(colId, colItemName, colDescription, colType, colPrice,
                colBidCount, colHighestBidder, colEndTime, colStatus, colSeller);

        notificationMenuHandler = new NotificationMenuHandler(darkOverlay, notificationMenu, 266);

        // Đăng ký lắng nghe danh sách đấu giá từ Server với bộ lọc theo Role
        String currentUserId = network.SessionManager.getInstance().getUserId();
        boolean isAdmin = network.SessionManager.getInstance().isAdmin();
        boolean isSeller = network.SessionManager.getInstance().isSeller();
        boolean isBidder = network.SessionManager.getInstance().isBidder();

        AuctionNetworkHelper.registerAuctionListListener(tableAssets, a -> {
            if (isAdmin) return true; // Admin thấy tất cả
            if (isSeller) {
                // Seller thấy những đồ của mình đăng bán (đang bán/đã bán)
                return a.getItem().getOwner() != null && a.getItem().getOwner().getId().equals(currentUserId);
            }
            if (isBidder) {
                // Bidder thấy những room đã/đang đặt giá vào
                return a.getBidHistory().stream().anyMatch(t -> t.getBidder().getId().equals(currentUserId));
            }
            return false;
        });

        // Cấu hình bảng thông báo
        if (tableNotifications != null) {
            colNotifContent.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("content"));
            colNotifTime.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("time"));
            
            colNotifContent.setCellFactory(tc -> {
                javafx.scene.control.TableCell<network.NotificationManager.NotificationItem, String> cell = new javafx.scene.control.TableCell<>() {
                    private final javafx.scene.text.Text textNode = new javafx.scene.text.Text();
                    {
                        textNode.wrappingWidthProperty().bind(tc.widthProperty().subtract(10));
                        textNode.setStyle("-fx-fill: #333333; -fx-font-family: 'Segoe UI'; -fx-font-size: 13px;");
                        setGraphic(textNode);
                    }
                    @Override
                    protected void updateItem(String item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty || item == null) {
                            textNode.setText(null);
                        } else {
                            textNode.setText(item);
                        }
                    }
                };
                return cell;
            });
            
            tableNotifications.setItems(network.NotificationManager.getInstance().getNotifications());
            tableNotifications.setFixedCellSize(-1); // Tự động giãn dòng
        }

        setupContextMenu();
        setupNetworkListeners();
    }

    private void setupContextMenu() {
        javafx.scene.control.ContextMenu contextMenu = new javafx.scene.control.ContextMenu();
        javafx.scene.control.MenuItem editItem = new javafx.scene.control.MenuItem("Sửa thông tin phiên đấu giá");
        javafx.scene.control.MenuItem deleteItem = new javafx.scene.control.MenuItem("Xóa phiên đấu giá");

        // Chỉ Admin hoặc Seller mới thấy menu này (Bidder không thấy hoặc không có action)
        if (network.SessionManager.getInstance().isBidder()) {
            return;
        }

        editItem.setOnAction(e -> {
            Auction selected = tableAssets.getSelectionModel().getSelectedItem();
            if (selected != null) {
                boolean isAdmin = network.SessionManager.getInstance().isAdmin();
                boolean isOwner = selected.getItem().getOwner() != null && 
                                 selected.getItem().getOwner().getId().equals(network.SessionManager.getInstance().getUserId());

                if (!isAdmin && !isOwner) {
                    view.utility.AlertHelper.showWarning("Cảnh báo", "Bạn chỉ có thể sửa thông tin của chính mình!");
                    return;
                }

                // Tạo Custom Dialog để sửa nhiều trường
                javafx.scene.control.Dialog<String> dialog = new javafx.scene.control.Dialog<>();
                dialog.setTitle("Sửa phiên đấu giá");
                dialog.setHeaderText("Chỉnh sửa thông tin phiên: " + selected.getId());

                javafx.scene.control.ButtonType updateButtonType = new javafx.scene.control.ButtonType("Cập nhật", javafx.scene.control.ButtonBar.ButtonData.OK_DONE);
                dialog.getDialogPane().getButtonTypes().addAll(updateButtonType, javafx.scene.control.ButtonType.CANCEL);

                javafx.scene.layout.GridPane grid = new javafx.scene.layout.GridPane();
                grid.setHgap(10);
                grid.setVgap(10);
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
                priceField.setDisable(!isAdmin); // Chỉ ADMIN mới được sửa giá

                javafx.scene.control.TextField timeField = new javafx.scene.control.TextField(String.valueOf(java.time.Duration.between(java.time.LocalDateTime.now(), selected.getEndTime()).toMinutes()));
                timeField.setDisable(!isAdmin); // Chỉ ADMIN mới được sửa thời gian

                grid.add(new javafx.scene.control.Label("Tên sản phẩm:"), 0, 0);
                grid.add(nameField, 1, 0);
                grid.add(new javafx.scene.control.Label("Mô tả:"), 0, 1);
                grid.add(descField, 1, 1);
                grid.add(new javafx.scene.control.Label("Loại:"), 0, 2);
                grid.add(typeBox, 1, 2);
                grid.add(new javafx.scene.control.Label("Giá khởi điểm ($):"), 0, 3);
                grid.add(priceField, 1, 3);
                grid.add(new javafx.scene.control.Label("Thời gian còn lại (phút):"), 0, 4);
                grid.add(timeField, 1, 4);

                dialog.getDialogPane().setContent(grid);

                dialog.setResultConverter(dialogButton -> {
                    if (dialogButton == updateButtonType) {
                        String name = nameField.getText().trim();
                        String desc = descField.getText().trim();
                        String price = priceField.getText().trim();
                        String duration = timeField.getText().trim();

                        // VALIDATION TRƯỚC KHI GỬI
                        if (view.utility.ValidationHelper.isEmpty(name)) {
                            view.utility.AlertHelper.showWarning("Lỗi", "Tên sản phẩm không được để trống!");
                            return null;
                        }
                        if (!view.utility.ValidationHelper.isValidStartPrice(price)) {
                            view.utility.AlertHelper.showWarning("Lỗi", "Giá khởi điểm phải là số dương!");
                            return null;
                        }
                        if (!view.utility.ValidationHelper.isValidDuration(duration)) {
                            view.utility.AlertHelper.showWarning("Lỗi", "Thời lượng phải là số nguyên dương!");
                            return null;
                        }

                        return shared.Protocol.REQ_UPDATE_ITEM + shared.Protocol.DELIMITER + 
                               selected.getId() + shared.Protocol.DELIMITER + 
                               name + shared.Protocol.DELIMITER + 
                               desc.replace("\n", " ") + shared.Protocol.DELIMITER + 
                               typeBox.getValue() + shared.Protocol.DELIMITER + 
                               price + shared.Protocol.DELIMITER + 
                               duration;
                    }
                    return null;
                });

                dialog.showAndWait().ifPresent(command -> {
                    network.ClientNetworkManager.getInstance().sendData(command);
                });
            }
        });

        deleteItem.setOnAction(e -> {
            Auction selected = tableAssets.getSelectionModel().getSelectedItem();
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
                confirm.setContentText("Mã: " + selected.getId() + "\nLưu ý: Seller chỉ xóa được nếu chưa có ai đặt giá.");
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
        tableAssets.setContextMenu(contextMenu);
    }

    private void setupNetworkListeners() {
        network.ClientNetworkManager.getInstance().clearListeners(shared.Protocol.REQ_UPDATE_ITEM);
        network.ClientNetworkManager.getInstance().registerListener(shared.Protocol.REQ_UPDATE_ITEM, msg -> {
            String[] parts = msg.split(shared.Protocol.SEPARATOR);
            if (parts.length > 1) {
                javafx.application.Platform.runLater(() -> {
                    javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
                        parts[1].equals(shared.Protocol.RES_SUCCESS) ? javafx.scene.control.Alert.AlertType.INFORMATION : javafx.scene.control.Alert.AlertType.ERROR
                    );
                    alert.setHeaderText("Thông báo sửa phiên đấu giá");
                    alert.setContentText(parts.length > 2 ? parts[2] : (parts[1].equals(shared.Protocol.RES_SUCCESS) ? "Sửa thành công!" : "Sửa thất bại!"));
                    alert.show();
                });
            }
        });

        network.ClientNetworkManager.getInstance().clearListeners(shared.Protocol.REQ_DELETE_ITEM);
        network.ClientNetworkManager.getInstance().registerListener(shared.Protocol.REQ_DELETE_ITEM, msg -> {
            String[] parts = msg.split(shared.Protocol.SEPARATOR);
            if (parts.length > 1) {
                javafx.application.Platform.runLater(() -> {
                    javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
                        parts[1].equals(shared.Protocol.RES_SUCCESS) ? javafx.scene.control.Alert.AlertType.INFORMATION : javafx.scene.control.Alert.AlertType.ERROR
                    );
                    alert.setHeaderText("Thông báo xóa phiên đấu giá");
                    alert.setContentText(parts.length > 2 ? parts[2] : (parts[1].equals(shared.Protocol.RES_SUCCESS) ? "Xóa thành công!" : "Xóa thất bại!"));
                    alert.show();
                });
            }
        });

        // Đăng ký lắng nghe số dư phản hồi (nếu cần alert riêng cho trang này)
        network.ClientNetworkManager.getInstance().clearListeners(shared.Protocol.REQ_DEPOSIT);
        network.ClientNetworkManager.getInstance().registerListener(shared.Protocol.REQ_DEPOSIT, (message) -> {
            String[] parts = message.split(shared.Protocol.SEPARATOR);
            if (parts.length >= 2 && parts[1].equals(shared.Protocol.RES_SUCCESS)) {
                javafx.application.Platform.runLater(() -> AlertHelper.showInfo("Thành công", "Nạp tiền thành công!"));
            }
        });

        network.ClientNetworkManager.getInstance().clearListeners(shared.Protocol.REQ_WITHDRAW);
        network.ClientNetworkManager.getInstance().registerListener(shared.Protocol.REQ_WITHDRAW, (message) -> {
            String[] parts = message.split(shared.Protocol.SEPARATOR);
            if (parts.length >= 2 && parts[1].equals(shared.Protocol.RES_SUCCESS)) {
                javafx.application.Platform.runLater(() -> AlertHelper.showInfo("Thành công", "Rút tiền thành công!"));
            }
        });
    }

    @FXML
    private void openChoiceMenu(ActionEvent event) {
        if (!network.SessionManager.getInstance().isSeller()) {
            view.utility.AlertHelper.showWarning("Quyền truy cập", "Chỉ người bán (Seller) mới có thể tạo phiên đấu giá!");
            return;
        }
        Stage currentStage = (Stage) menuBar.getScene().getWindow();
        WindowManager.openCreateItemWindow(currentStage);
    }

    @FXML
    private void notificationClicked(ActionEvent event) {
        notificationMenuHandler.toggleMenu();
    }

    @FXML
    private void openProfile(ActionEvent event) {
        WindowManager.openUserProfileWindow();
    }

    @FXML
    private void backToBaseMenuButtonClicked(ActionEvent event) {
        Stage stage = (Stage) menuBar.getScene().getWindow();
        SceneManager.switchScene(stage, "/view/BaseMenuUI/BaseMenu.fxml", "Base Menu");
    }

    @FXML
    private void backToLoiginButtonClicked(ActionEvent event) {
        network.ClientNetworkManager.getInstance().logout();
        Stage stage = (Stage) menuBar.getScene().getWindow();
        SceneManager.switchScene(stage, "/view/AuthenticationUI/LoginView/Login.fxml", "Login");
    }

    @FXML
    private void backToRegisterButtonClicked(ActionEvent event) {
        Stage stage = (Stage) menuBar.getScene().getWindow();
        SceneManager.switchScene(stage, "/view/AuthenticationUI/RegisterView/Register.fxml", "Register");
    }

    @FXML
    private void goToAssetsListButtonClicked(ActionEvent event) {
        // Gửi yêu cầu làm mới danh sách tài sản
        System.out.println("🔄 Đang làm mới danh sách tài sản...");
        AuctionNetworkHelper.requestAuctionList();
    }

    @FXML
    private void handleTransaction(ActionEvent event) {
        if (network.SessionManager.getInstance().isBidder()) {
            TextInputDialog dialog = new TextInputDialog();
            dialog.setTitle("Nạp tiền");
            dialog.setHeaderText("Nhập số tiền muốn nạp:");
            dialog.setContentText("Số tiền ($):");
            Optional<String> result = dialog.showAndWait();
            result.ifPresent(amount -> {
                if (!amount.trim().isEmpty()) {
                    network.ClientNetworkManager.getInstance().sendData(shared.Protocol.REQ_DEPOSIT + shared.Protocol.DELIMITER + amount.trim());
                }
            });
        } else if (network.SessionManager.getInstance().isSeller()) {
            TextInputDialog dialog = new TextInputDialog();
            dialog.setTitle("Rút tiền");
            dialog.setHeaderText("Nhập số tiền muốn rút:");
            dialog.setContentText("Số tiền ($):");
            Optional<String> result = dialog.showAndWait();
            result.ifPresent(amount -> {
                if (!amount.trim().isEmpty()) {
                    network.ClientNetworkManager.getInstance().sendData(shared.Protocol.REQ_WITHDRAW + shared.Protocol.DELIMITER + amount.trim());
                }
            });
        }
    }
}
