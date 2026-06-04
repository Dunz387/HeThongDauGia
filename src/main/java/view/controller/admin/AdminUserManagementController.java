package view.controller.admin;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.HBox;
import model.user.User;
import view.utility.admin.AdminNavigation;
import view.utility.admin.AdminTableColumns;
import view.utility.admin.AdminUserManagementSupport;

import java.net.URL;
import java.util.ResourceBundle;

public class AdminUserManagementController implements Initializable {

    @FXML private HBox menuBar;
    @FXML private TableView<User> tableUsers;
    @FXML private TableColumn<User, Integer> colSTT;
    @FXML private TableColumn<User, String> colId;
    @FXML private TableColumn<User, String> colUsername;
    @FXML private TableColumn<User, String> colRole;
    @FXML private TableColumn<User, Double> colBalance;
    @FXML private TableColumn<User, String> colStatus;
    @FXML private TableColumn<User, Void> colAction;

    private AdminUserManagementSupport support;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        AdminTableColumns.configureUserManagementColumns(
                tableUsers,
                colSTT,
                colId,
                colUsername,
                colRole,
                colBalance,
                colStatus
        );

        support = new AdminUserManagementSupport(tableUsers, colAction);
        support.configureActionColumn();
        support.registerListeners();
        support.load();
    }

    @FXML
    private void goToDashboard(ActionEvent event) {
        AdminNavigation.goToDashboard(menuBar);
    }

    @FXML
    private void goToAuctionManagement(ActionEvent event) {
        AdminNavigation.goToAuctionManagement(menuBar);
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
