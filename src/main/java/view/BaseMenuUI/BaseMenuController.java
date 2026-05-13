package view.BaseMenuUI;

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
import view.utility.AuctionNetworkHelper;
import view.utility.AuctionTableConfigurator;
import view.utility.NotificationMenuHandler;
import view.utility.SceneManager;
import view.utility.WindowManager;
import view.utility.AlertHelper;

import java.net.URL;
import java.util.ResourceBundle;

public class BaseMenuController implements Initializable {
    @FXML private Pane darkOverlay;
    @FXML private ScrollPane notificationMenu;
    private NotificationMenuHandler notificationMenuHandler;

    @FXML private HBox menuBar;

    @FXML private TableView<Auction> tableAuctions;
    @FXML private TableColumn<Auction, String> colId;
    @FXML private TableColumn<Auction, String> colName;
    @FXML private TableColumn<Auction, String> colType;
    @FXML private TableColumn<Auction, Double> colPrice;
    @FXML private TableColumn<Auction, Integer> colBidCount;
    @FXML private TableColumn<Auction, String> colHighestBidder;
    @FXML private TableColumn<Auction, String> colEndTime;
    @FXML private TableColumn<Auction, String> colStatus;
    @FXML private TableColumn<Auction, String> colSeller;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Cấu hình 9 cột bảng thống nhất (SRP: delegate sang AuctionTableConfigurator)
        AuctionTableConfigurator.configure(colId, colName, colType, colPrice,
                colBidCount, colHighestBidder, colEndTime, colStatus, colSeller);

        // Nhấp đúp chuột vào dòng để mở phòng đấu giá dựa trên Role
        tableAuctions.setRowFactory(tv -> {
            TableRow<Auction> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && (!row.isEmpty())) {
                    Auction selectedAuction = row.getItem();
                    Stage currentStage = (Stage) tableAuctions.getScene().getWindow();
                    
                    if (network.SessionManager.getInstance().isBidder()) {
                        SceneManager.goToInRoom(currentStage, selectedAuction.getId());
                    } else if (network.SessionManager.getInstance().isSeller()) {
                        if (selectedAuction.getItem().getOwner() != null && selectedAuction.getItem().getOwner().getId().equals(network.SessionManager.getInstance().getUserId())) {
                            SceneManager.goToSellerInRoom(currentStage, selectedAuction.getId());
                        } else {
                            AlertHelper.showWarning("Cảnh báo", "Bạn chỉ có thể xem phòng đấu giá của chính mình!");
                        }
                    } else if (network.SessionManager.getInstance().isAdmin()) {
                        SceneManager.goToInRoom(currentStage, selectedAuction.getId());
                    } else {
                        AlertHelper.showWarning("Quyền truy cập", "Bạn không có quyền tham gia!");
                    }
                }
            });
            return row;
        });

        // Đăng ký lắng nghe danh sách đấu giá từ Server (SRP: delegate sang AuctionNetworkHelper)
        AuctionNetworkHelper.registerAuctionListListener(tableAuctions);

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
        Stage stage = (Stage) menuBar.getScene().getWindow();
        SceneManager.switchScene(stage, "/view/BaseMenuUI/AssetsList.fxml", "Danh Sách Tài Sản");
    }

    @FXML
    private void notificationClicked(ActionEvent event){
        notificationMenuHandler.toggleMenu();
    }

    @FXML
    private void openProfile(ActionEvent event) {
        WindowManager.openUserProfileWindow();
    }
}