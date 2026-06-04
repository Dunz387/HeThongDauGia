package view.controller.admin;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.HBox;
import model.auction.Auction;
import view.utility.admin.AdminAuctionManagementSupport;
import view.utility.admin.AdminNavigation;
import view.utility.admin.AdminTableColumns;

import java.net.URL;
import java.util.ResourceBundle;

public class AdminAuctionManagementController implements Initializable {

    @FXML private HBox menuBar;
    @FXML private TableView<Auction> tableAuctions;
    @FXML private TableColumn<Auction, Integer> colSTT;
    @FXML private TableColumn<Auction, String> colId;
    @FXML private TableColumn<Auction, String> colName;
    @FXML private TableColumn<Auction, Double> colPrice;
    @FXML private TableColumn<Auction, String> colStatus;
    @FXML private TableColumn<Auction, String> colBidder;
    @FXML private TableColumn<Auction, Void> colView;
    @FXML private TableColumn<Auction, Void> colAction;

    private AdminAuctionManagementSupport support;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        AdminTableColumns.configureAuctionManagementColumns(
                tableAuctions,
                colSTT,
                colId,
                colName,
                colPrice,
                colStatus,
                colBidder
        );

        support = new AdminAuctionManagementSupport(tableAuctions, colView, colAction);
        support.configureActionColumns();
        support.registerListeners();
        support.load();
    }

    @FXML
    private void goToDashboard(ActionEvent event) {
        AdminNavigation.goToDashboard(menuBar);
    }

    @FXML
    private void goToUserManagement(ActionEvent event) {
        AdminNavigation.goToUserManagement(menuBar);
    }

    @FXML
    private void logoutClicked(ActionEvent event) {
        AdminNavigation.logout(menuBar);
    }

    @FXML
    private void refreshData(ActionEvent event) {
        support.load();
    }
}
