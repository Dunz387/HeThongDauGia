package view.utility.admin;

import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import model.auction.Auction;
import model.user.Bidder;
import model.user.Seller;
import model.user.User;
import view.utility.display.StatusDisplayHelper;

public final class AdminTableColumns {
    private AdminTableColumns() {
    }

    public static void configureAuctionSummaryColumns(
            TableColumn<Auction, String> idColumn,
            TableColumn<Auction, String> nameColumn,
            TableColumn<Auction, Double> priceColumn,
            TableColumn<Auction, String> statusColumn
    ) {
        idColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getId()));
        nameColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getItem().getName()));
        priceColumn.setCellValueFactory(
                cellData -> new SimpleDoubleProperty(cellData.getValue().getCurrentPrice()).asObject());
        statusColumn.setCellValueFactory(cellData -> new SimpleStringProperty(
                StatusDisplayHelper.formatAuctionStatus(cellData.getValue().getStatus().name())));
    }

    public static void configureAuctionManagementColumns(
            TableView<Auction> table,
            TableColumn<Auction, Integer> indexColumn,
            TableColumn<Auction, String> idColumn,
            TableColumn<Auction, String> nameColumn,
            TableColumn<Auction, Double> priceColumn,
            TableColumn<Auction, String> statusColumn,
            TableColumn<Auction, String> bidderColumn
    ) {
        indexColumn.setCellValueFactory(
                cellData -> new SimpleIntegerProperty(table.getItems().indexOf(cellData.getValue()) + 1).asObject());
        configureAuctionSummaryColumns(idColumn, nameColumn, priceColumn, statusColumn);
        bidderColumn.setCellValueFactory(cellData -> {
            var bidder = cellData.getValue().getHighestBidder();
            return new SimpleStringProperty(bidder != null ? bidder.getUsername() : "Chưa có");
        });
    }

    public static void configureUserSummaryColumns(
            TableColumn<User, String> idColumn,
            TableColumn<User, String> usernameColumn,
            TableColumn<User, String> roleColumn,
            TableColumn<User, String> statusColumn
    ) {
        idColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getId()));
        usernameColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getUsername()));
        roleColumn.setCellValueFactory(cellData -> new SimpleStringProperty(
                StatusDisplayHelper.formatUserRole(cellData.getValue().getRole().name())));
        statusColumn.setCellValueFactory(cellData -> new SimpleStringProperty(
                StatusDisplayHelper.formatUserStatus(cellData.getValue().isActive())));
    }

    public static void configureUserManagementColumns(
            TableView<User> table,
            TableColumn<User, Integer> indexColumn,
            TableColumn<User, String> idColumn,
            TableColumn<User, String> usernameColumn,
            TableColumn<User, String> roleColumn,
            TableColumn<User, Double> balanceColumn,
            TableColumn<User, String> statusColumn
    ) {
        indexColumn.setCellValueFactory(
                cellData -> new SimpleIntegerProperty(table.getItems().indexOf(cellData.getValue()) + 1).asObject());
        configureUserSummaryColumns(idColumn, usernameColumn, roleColumn, statusColumn);
        balanceColumn.setCellValueFactory(
                cellData -> new SimpleDoubleProperty(balanceOf(cellData.getValue())).asObject());
    }

    public static double balanceOf(User user) {
        if (user instanceof Bidder) {
            return ((Bidder) user).getBalance();
        }
        if (user instanceof Seller) {
            return ((Seller) user).getBalance();
        }
        return 0.0;
    }
}
