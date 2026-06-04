package view.controller.menu;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import model.auction.Auction;
import network.notification.NotificationManager;
import view.utility.auction.AuctionNetworkHelper;
import view.utility.auction.RoleBasedFilterHelper;
import view.utility.menu.MenuNavigationSupport;
import view.utility.menu.MenuNetworkSupport;
import view.utility.menu.MenuTableSupport;
import view.utility.navigation.MenuHelper;
import view.utility.navigation.WindowManager;
import view.utility.notification.NotificationFilterHelper;
import view.utility.notification.NotificationMenuHandler;

import java.net.URL;
import java.util.ResourceBundle;

public class BaseMenuController implements Initializable {
    @FXML private Label txtBalance;
    @FXML private Button btnTransaction;
    @FXML private Pane darkOverlay;
    @FXML private ScrollPane notificationMenu;
    @FXML private HBox menuBar;
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
    @FXML private TableView<NotificationManager.NotificationItem> tableNotifications;
    @FXML private TableColumn<NotificationManager.NotificationItem, String> colNotifContent;
    @FXML private TableColumn<NotificationManager.NotificationItem, String> colNotifTime;

    private NotificationMenuHandler notificationMenuHandler;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        MenuHelper.setupBalanceLabel(txtBalance);
        MenuHelper.setupTransactionButton(btnTransaction);
        MenuTableSupport.configureAuctionTable(colId, colName, colDescription, colType, colPrice, colBidCount,
                colHighestBidder, colEndTime, colStatus, colSeller);
        MenuTableSupport.configureRoomOpenOnDoubleClick(tableAuctions);
        MenuTableSupport.configureNotificationTable(tableNotifications, colNotifContent, colNotifTime);
        AuctionNetworkHelper.registerAuctionListListener(tableAuctions, RoleBasedFilterHelper.getRoomFilter());
        NotificationFilterHelper.registerNotificationListeners(tableAuctions);
        MenuNetworkSupport.registerTransactionResponses(true);
        notificationMenuHandler = new NotificationMenuHandler(darkOverlay, notificationMenu, 266);
    }

    @FXML
    private void joinBidding(ActionEvent event) {
        MenuNavigationSupport.openRoomMenu(txtBalance);
    }

    @FXML
    private void openChoiceMenu(ActionEvent event) {
        MenuNavigationSupport.openCreateAuction(txtBalance);
    }

    @FXML
    private void backToLoiginButtonClicked(ActionEvent event) {
        MenuNavigationSupport.logoutToLogin(txtBalance);
    }

    @FXML
    private void backToRegisterButtonClicked(ActionEvent event) {
        MenuNavigationSupport.goToRegister(txtBalance);
    }

    @FXML
    private void goToAssetsListButtonClicked(ActionEvent event) {
        MenuNavigationSupport.goToAssetsList(txtBalance);
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
