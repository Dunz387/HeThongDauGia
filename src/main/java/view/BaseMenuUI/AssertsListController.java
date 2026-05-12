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

import java.net.URL;
import java.util.ResourceBundle;

public class AssertsListController implements Initializable {
    @FXML private Pane darkOverlay;
    @FXML private ScrollPane notificationMenu;
    private NotificationMenuHandler notificationMenuHandler;

    @FXML private HBox menuBar;
    @FXML private TableView<Auction> tableAsserts;
    @FXML private TableColumn<Auction, String> colId;
    @FXML private TableColumn<Auction, String> colItemName;
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
        // Lưu ý: colItemName đóng vai trò colName trong bảng thống nhất
        AuctionTableConfigurator.configure(colId, colItemName, colType, colPrice,
                colBidCount, colHighestBidder, colEndTime, colStatus, colSeller);

        notificationMenuHandler = new NotificationMenuHandler(darkOverlay, notificationMenu, 266);

        // Đăng ký lắng nghe danh sách đấu giá từ Server (SRP: delegate sang AuctionNetworkHelper)
        AuctionNetworkHelper.registerAuctionListListener(tableAsserts);
    }

    @FXML
    private void joinNewBidding(ActionEvent event) {
        Stage currentStage = (Stage) menuBar.getScene().getWindow();
        WindowManager.openBidOrSellChoiceWindow(currentStage);
    }

    @FXML
    private void toggleNotificationMenu(ActionEvent event) {
        notificationMenuHandler.toggleMenu();
    }

    @FXML
    private void backToBaseMenuButtonClicked(ActionEvent event) {
        Stage stage = (Stage) menuBar.getScene().getWindow();
        SceneManager.switchScene(stage, "/view/BaseMenuUI/BaseMenu.fxml", "Base Menu");
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
    private void goToAssertsListButtonClicked(ActionEvent event) {
        // Gửi yêu cầu làm mới danh sách tài sản
        System.out.println("🔄 Đang làm mới danh sách tài sản...");
        AuctionNetworkHelper.requestAuctionList();
    }
}