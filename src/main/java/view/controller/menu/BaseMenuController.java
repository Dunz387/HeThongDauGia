package view.controller.menu;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;
import model.auction.Auction;
import view.utility.AuctionTableConfigurator;
import view.utility.MenuHelper;
import view.utility.NotificationMenuHandler;
import view.utility.SceneManager;
import view.utility.WindowManager;
import view.utility.AlertHelper;
import view.utility.WrappingTextCellFactory;

import java.net.URL;
import java.util.ResourceBundle;

public class BaseMenuController implements Initializable {
    @FXML
    private javafx.scene.control.Label txtBalance;
    @FXML
    private javafx.scene.control.Button btnTransaction;
    @FXML
    private Pane darkOverlay;
    @FXML
    private ScrollPane notificationMenu;
    private NotificationMenuHandler notificationMenuHandler;

    @FXML
    private HBox menuBar;

    @FXML
    private TableView<Auction> tableAuctions;
    @FXML
    private TableColumn<Auction, String> colId;
    @FXML
    private TableColumn<Auction, String> colName;
    @FXML
    private TableColumn<Auction, String> colDescription;
    @FXML
    private TableColumn<Auction, String> colType;
    @FXML
    private TableColumn<Auction, Double> colPrice;
    @FXML
    private TableColumn<Auction, Integer> colBidCount;
    @FXML
    private TableColumn<Auction, String> colHighestBidder;
    @FXML
    private TableColumn<Auction, String> colEndTime;
    @FXML
    private TableColumn<Auction, String> colStatus;
    @FXML
    private TableColumn<Auction, String> colSeller;

    // Notifications
    @FXML
    private TableView<network.NotificationManager.NotificationItem> tableNotifications;
    @FXML
    private TableColumn<network.NotificationManager.NotificationItem, String> colNotifContent;
    @FXML
    private TableColumn<network.NotificationManager.NotificationItem, String> colNotifTime;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        MenuHelper.setupBalanceLabel(txtBalance);
        MenuHelper.setupTransactionButton(btnTransaction);

        // Cấu hình 10 cột bảng thống nhất (SRP: delegate sang AuctionTableConfigurator)
        AuctionTableConfigurator.configure(colId, colName, colDescription, colType, colPrice, colBidCount,
                colHighestBidder, colEndTime, colStatus, colSeller);

        // Nhấp đúp chuột vào dòng để mở phòng đấu giá dựa trên Role
        tableAuctions.setRowFactory(tv -> {
            TableRow<Auction> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && (!row.isEmpty())) {
                    Auction selectedAuction = row.getItem();
                    Stage currentStage = (Stage) tableAuctions.getScene().getWindow();

                    if (network.SessionManager.getInstance().isBidder()) {
                        SceneManager.goToInRoom(currentStage, selectedAuction);
                    } else if (network.SessionManager.getInstance().isSeller()) {
                        if (selectedAuction.getItem().getOwner() != null && selectedAuction.getItem().getOwner().getId()
                                .equals(network.SessionManager.getInstance().getUserId())) {
                            SceneManager.goToSellerInRoom(currentStage, selectedAuction);
                        } else {
                            AlertHelper.showWarning("Cảnh báo", "Bạn chỉ có thể xem phòng đấu giá của chính mình!");
                        }
                    } else if (network.SessionManager.getInstance().isAdmin()) {
                        SceneManager.goToInRoom(currentStage, selectedAuction);
                    } else {
                        AlertHelper.showWarning("Quyền truy cập", "Bạn không có quyền tham gia!");
                    }
                }
            });
            return row;
        });

        // Đăng ký lắng nghe danh sách đấu giá từ Server
        view.utility.AuctionNetworkHelper.registerAuctionListListener(tableAuctions);

        // Cấu hình bảng thông báo
        if (tableNotifications != null) {
            colNotifContent.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("content"));
            colNotifTime.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("time"));

            colNotifContent.setCellFactory(new WrappingTextCellFactory());

            tableNotifications.setItems(network.NotificationManager.getInstance().getNotifications());
            tableNotifications.setFixedCellSize(-1);
        }

        // Lắng nghe các sự kiện quan trọng để đẩy thông báo ra ngoài màn hình chính
        network.ClientNetworkManager.getInstance().clearListeners(shared.Protocol.BROADCAST_AUCTION_START);
        network.ClientNetworkManager.getInstance().registerListener(shared.Protocol.BROADCAST_AUCTION_START,
                (message) -> {
                    String[] parts = message.split(shared.Protocol.DELIMITER);
                    if (parts.length >= 2) {
                        network.NotificationManager.getInstance()
                                .addNotification("🚀 Một phiên đấu giá mới (" + parts[1] + ") đã bắt đầu!");
                    }
                });

        network.ClientNetworkManager.getInstance().clearListeners(shared.Protocol.BROADCAST_AUCTION_FINISHED);
        network.ClientNetworkManager.getInstance().registerListener(shared.Protocol.BROADCAST_AUCTION_FINISHED,
                (message) -> {
                    String[] parts = message.split(shared.Protocol.DELIMITER);
                    if (parts.length >= 2) {
                        String auctionId = parts[1];
                        String winner = parts.length > 2 ? parts[2] : "Không có";

                        // Tìm tên sản phẩm từ bảng
                        String itemName = auctionId;
                        for (Auction a : tableAuctions.getItems()) {
                            if (a.getId().equals(auctionId)) {
                                itemName = a.getItem().getName();
                                break;
                            }
                        }
                        network.NotificationManager.getInstance()
                                .addNotification("🏆 [" + itemName + "] kết thúc. Người thắng: " + winner);
                    }
                });

        // Cập nhật số dư realtime — đã đăng ký bởi MenuHelper.setupBalanceLabel()

        // Đăng ký Listener phản hồi giao dịch
        network.ClientNetworkManager.getInstance().clearListeners(shared.Protocol.REQ_DEPOSIT);
        network.ClientNetworkManager.getInstance().registerListener(shared.Protocol.REQ_DEPOSIT, (message) -> {
            String[] parts = message.split(shared.Protocol.DELIMITER);
            if (parts.length >= 2 && parts[1].equals(shared.Protocol.RES_SUCCESS)) {
                javafx.application.Platform.runLater(() -> AlertHelper.showInfo("Thành công", "Nạp tiền thành công!"));
            } else {
                String reason = parts.length > 2 ? parts[2] : "Lỗi hệ thống";
                javafx.application.Platform
                        .runLater(() -> AlertHelper.showError("Thất bại", "Nạp tiền thất bại: " + reason));
            }
        });

        network.ClientNetworkManager.getInstance().clearListeners(shared.Protocol.REQ_WITHDRAW);
        network.ClientNetworkManager.getInstance().registerListener(shared.Protocol.REQ_WITHDRAW, (message) -> {
            String[] parts = message.split(shared.Protocol.DELIMITER);
            if (parts.length >= 2 && parts[1].equals(shared.Protocol.RES_SUCCESS)) {
                javafx.application.Platform.runLater(() -> AlertHelper.showInfo("Thành công", "Rút tiền thành công!"));
            } else {
                String reason = parts.length > 2 ? parts[2] : "Lỗi hệ thống";
                javafx.application.Platform
                        .runLater(() -> AlertHelper.showError("Thất bại", "Rút tiền thất bại: " + reason));
            }
        });

        notificationMenuHandler = new NotificationMenuHandler(darkOverlay, notificationMenu, 266);
    }

    // --- CÁC HÀM XỬ LÝ SỰ KIỆN CLICK TỪ GIAO DIỆN (FXML) ---

    @FXML
    private void joinBidding(ActionEvent event) {
        Stage currentStage = (Stage) menuBar.getScene().getWindow();
        SceneManager.goToRoomMenu(currentStage);
    }

    @FXML
    private void openChoiceMenu(ActionEvent event) {
        if (!network.SessionManager.getInstance().isSeller()) {
            AlertHelper.showWarning("Quyền truy cập", "Chỉ người bán (Seller) mới có thể tạo phiên đấu giá!");
            return;
        }
        Stage currentStage = (Stage) menuBar.getScene().getWindow();
        WindowManager.openCreateItemWindow(currentStage);
    }

    @FXML
    private void backToLoiginButtonClicked(ActionEvent event) {
        network.ClientNetworkManager.getInstance().logout();
        Stage stage = (Stage) menuBar.getScene().getWindow();
        SceneManager.switchScene(stage, "/view/auth/Login.fxml", "Login");
    }

    @FXML
    private void backToRegisterButtonClicked(ActionEvent event) {
        Stage stage = (Stage) menuBar.getScene().getWindow();
        SceneManager.switchScene(stage, "/view/auth/Register.fxml", "Register");
    }

    @FXML
    private void goToAssetsListButtonClicked(ActionEvent event) {
        Stage stage = (Stage) menuBar.getScene().getWindow();
        SceneManager.switchScene(stage, "/view/menu/AssetsList.fxml", "Danh Sách Tài Sản");
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
    private void handleTransaction(ActionEvent event) {
        MenuHelper.handleTransaction();
    }
}