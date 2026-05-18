package view.controller.menu;

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
import view.utility.MenuHelper;
import view.utility.NotificationMenuHandler;
import view.utility.SceneManager;
import view.utility.WindowManager;
import view.utility.AlertHelper;

import java.net.URL;
import java.util.ResourceBundle;

public class AssetsListController implements Initializable {
    @FXML
    private Pane darkOverlay;
    @FXML
    private ScrollPane notificationMenu;
    private NotificationMenuHandler notificationMenuHandler;

    @FXML
    private HBox menuBar;
    @FXML
    private javafx.scene.control.Label txtBalance;
    @FXML
    private javafx.scene.control.Button btnTransaction;

    @FXML
    private TableView<Auction> tableAssets;
    @FXML
    private TableColumn<Auction, String> colId;
    @FXML
    private TableColumn<Auction, String> colItemName;
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

        // Cấu hình bảng thống nhất (SRP: delegate sang AuctionTableConfigurator)
        AuctionTableConfigurator.configure(colId, colItemName, colDescription, colType, colPrice, colBidCount,
                colHighestBidder, colEndTime, colStatus, colSeller);

        notificationMenuHandler = new NotificationMenuHandler(darkOverlay, notificationMenu, 266);

        // Đăng ký lắng nghe danh sách đấu giá từ Server với bộ lọc theo Role (SRP: delegate sang RoleBasedFilterHelper)
        AuctionNetworkHelper.registerAuctionListListener(tableAssets, view.utility.RoleBasedFilterHelper.getAssetsFilter());

        // Cấu hình bảng thông báo
        if (tableNotifications != null) {
            colNotifContent.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("content"));
            colNotifTime.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("time"));

            colNotifContent.setCellFactory(new view.utility.WrappingTextCellFactory());

            tableNotifications.setItems(network.NotificationManager.getInstance().getNotifications());
            tableNotifications.setFixedCellSize(-1); // Tự động giãn dòng
        }

        setupContextMenu();
        setupNetworkListeners();
        
        // Đăng ký nhận thông báo real-time (tương tự như BaseMenu)
        view.utility.NotificationFilterHelper.registerNotificationListeners(tableAssets);
    }

    private void setupContextMenu() {
        view.utility.AuctionContextMenuHelper.setupContextMenu(tableAssets);
    }

    private void setupNetworkListeners() {
        network.ClientNetworkManager.getInstance().clearListeners(shared.Protocol.REQ_UPDATE_ITEM);
        network.ClientNetworkManager.getInstance().registerListener(shared.Protocol.REQ_UPDATE_ITEM, msg -> {
            String[] parts = msg.split(shared.Protocol.DELIMITER);
            if (parts.length > 1) {
                javafx.application.Platform.runLater(() -> {
                    javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
                            parts[1].equals(shared.Protocol.RES_SUCCESS)
                                    ? javafx.scene.control.Alert.AlertType.INFORMATION
                                    : javafx.scene.control.Alert.AlertType.ERROR);
                    alert.setHeaderText("Thông báo sửa phiên đấu giá");
                    alert.setContentText(parts.length > 2 ? parts[2]
                            : (parts[1].equals(shared.Protocol.RES_SUCCESS) ? "Sửa thành công!" : "Sửa thất bại!"));
                    alert.show();
                });
            }
        });

        network.ClientNetworkManager.getInstance().clearListeners(shared.Protocol.REQ_DELETE_ITEM);
        network.ClientNetworkManager.getInstance().registerListener(shared.Protocol.REQ_DELETE_ITEM, msg -> {
            String[] parts = msg.split(shared.Protocol.DELIMITER);
            if (parts.length > 1) {
                javafx.application.Platform.runLater(() -> {
                    javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
                            parts[1].equals(shared.Protocol.RES_SUCCESS)
                                    ? javafx.scene.control.Alert.AlertType.INFORMATION
                                    : javafx.scene.control.Alert.AlertType.ERROR);
                    alert.setHeaderText("Thông báo xóa phiên đấu giá");
                    alert.setContentText(parts.length > 2 ? parts[2]
                            : (parts[1].equals(shared.Protocol.RES_SUCCESS) ? "Xóa thành công!" : "Xóa thất bại!"));
                    alert.show();
                });
            }
        });

        // Đăng ký lắng nghe số dư phản hồi (nếu cần alert riêng cho trang này)
        network.ClientNetworkManager.getInstance().clearListeners(shared.Protocol.REQ_DEPOSIT);
        network.ClientNetworkManager.getInstance().registerListener(shared.Protocol.REQ_DEPOSIT, (message) -> {
            String[] parts = message.split(shared.Protocol.DELIMITER);
            if (parts.length >= 2 && parts[1].equals(shared.Protocol.RES_SUCCESS)) {
                javafx.application.Platform.runLater(() -> AlertHelper.showInfo("Thành công", "Nạp tiền thành công!"));
            }
        });

        network.ClientNetworkManager.getInstance().clearListeners(shared.Protocol.REQ_WITHDRAW);
        network.ClientNetworkManager.getInstance().registerListener(shared.Protocol.REQ_WITHDRAW, (message) -> {
            String[] parts = message.split(shared.Protocol.DELIMITER);
            if (parts.length >= 2 && parts[1].equals(shared.Protocol.RES_SUCCESS)) {
                javafx.application.Platform.runLater(() -> AlertHelper.showInfo("Thành công", "Rút tiền thành công!"));
            }
        });
    }

    @FXML
    private void openChoiceMenu(ActionEvent event) {
        if (!network.SessionManager.getInstance().isSeller()) {
            view.utility.AlertHelper.showWarning("Quyền truy cập",
                    "Chỉ người bán (Seller) mới có thể tạo phiên đấu giá!");
            return;
        }
        Stage currentStage = (Stage) txtBalance.getScene().getWindow();
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
        Stage stage = (Stage) txtBalance.getScene().getWindow();
        SceneManager.switchScene(stage, "/view/menu/BaseMenu.fxml", "Base Menu");
    }

    @FXML
    private void backToLoiginButtonClicked(ActionEvent event) {
        network.ClientNetworkManager.getInstance().logout();
        Stage stage = (Stage) txtBalance.getScene().getWindow();
        SceneManager.switchScene(stage, "/view/auth/Login.fxml", "Login");
    }

    @FXML
    private void backToRegisterButtonClicked(ActionEvent event) {
        Stage stage = (Stage) txtBalance.getScene().getWindow();
        SceneManager.switchScene(stage, "/view/auth/Register.fxml", "Register");
    }

    @FXML
    private void goToAssetsListButtonClicked(ActionEvent event) {
        // Gửi yêu cầu làm mới danh sách tài sản
        System.out.println("🔄 Đang làm mới danh sách tài sản...");
        AuctionNetworkHelper.requestAuctionList();
    }

    @FXML
    private void handleTransaction(ActionEvent event) {
        MenuHelper.handleTransaction();
    }
}
