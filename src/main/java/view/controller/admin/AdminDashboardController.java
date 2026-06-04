package view.controller.admin;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.HBox;
import model.auction.Auction;
import model.user.User;
import view.utility.admin.AdminDashboardDataBinder;
import view.utility.admin.AdminNavigation;
import view.utility.admin.AdminTableColumns;

import java.net.URL;
import java.util.ResourceBundle;

public class AdminDashboardController implements Initializable {

    @FXML private HBox menuBar;
    @FXML private Label lblTotalUsers;
    @FXML private Label lblTotalAuctions;
    @FXML private Label lblRunningAuctions;
    @FXML private Label lblFinishedAuctions;
    @FXML private TableView<Auction> tableRecentAuctions;
    @FXML private TableColumn<Auction, String> colAucId;
    @FXML private TableColumn<Auction, String> colAucName;
    @FXML private TableColumn<Auction, Double> colAucPrice;
    @FXML private TableColumn<Auction, String> colAucStatus;
    @FXML private TableView<User> tableRecentUsers;
    @FXML private TableColumn<User, String> colUserId;
    @FXML private TableColumn<User, String> colUserName;
    @FXML private TableColumn<User, String> colUserRole;
    @FXML private TableColumn<User, String> colUserStatus;

    private AdminDashboardDataBinder dataBinder;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        AdminTableColumns.configureAuctionSummaryColumns(colAucId, colAucName, colAucPrice, colAucStatus);
        AdminTableColumns.configureUserSummaryColumns(colUserId, colUserName, colUserRole, colUserStatus);

        dataBinder = new AdminDashboardDataBinder(
                lblTotalUsers,
                lblTotalAuctions,
                lblRunningAuctions,
                lblFinishedAuctions,
                tableRecentAuctions,
                tableRecentUsers
        );
        dataBinder.registerListeners();
        dataBinder.load();
    }

    @FXML
    private void goToUserManagement(ActionEvent event) {
        AdminNavigation.goToUserManagement(menuBar);
    }

    @FXML
    private void goToAuctionManagement(ActionEvent event) {
        AdminNavigation.goToAuctionManagement(menuBar);
    }

    @FXML
    private void logoutClicked(ActionEvent event) {
        AdminNavigation.logout(menuBar);
    }
}
