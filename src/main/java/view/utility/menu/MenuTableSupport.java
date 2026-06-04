package view.utility.menu;

import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.stage.Stage;
import model.auction.Auction;
import network.NotificationManager;
import network.SessionManager;
import view.utility.auction.AuctionTableConfigurator;
import view.utility.display.AlertHelper;
import view.utility.navigation.WindowManager;
import view.utility.table.WrappingTextCellFactory;

public final class MenuTableSupport {
    private MenuTableSupport() {
    }

    public static void configureAuctionTable(
            TableColumn<Auction, String> colId,
            TableColumn<Auction, String> colName,
            TableColumn<Auction, String> colDescription,
            TableColumn<Auction, String> colType,
            TableColumn<Auction, Double> colPrice,
            TableColumn<Auction, Integer> colBidCount,
            TableColumn<Auction, String> colHighestBidder,
            TableColumn<Auction, String> colEndTime,
            TableColumn<Auction, String> colStatus,
            TableColumn<Auction, String> colSeller) {
        AuctionTableConfigurator.configure(colId, colName, colDescription, colType, colPrice, colBidCount,
                colHighestBidder, colEndTime, colStatus, colSeller);
    }

    public static void configureRoomOpenOnDoubleClick(TableView<Auction> tableAuctions) {
        tableAuctions.setRowFactory(tv -> {
            TableRow<Auction> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    openAuctionRoom(row.getItem(), (Stage) tableAuctions.getScene().getWindow());
                }
            });
            return row;
        });
    }

    public static void configureNotificationTable(
            TableView<NotificationManager.NotificationItem> tableNotifications,
            TableColumn<NotificationManager.NotificationItem, String> colNotifContent,
            TableColumn<NotificationManager.NotificationItem, String> colNotifTime) {
        if (tableNotifications == null) {
            return;
        }

        colNotifContent.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("content"));
        colNotifTime.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("time"));
        colNotifContent.setCellFactory(new WrappingTextCellFactory());
        tableNotifications.setItems(NotificationManager.getInstance().getNotifications());
        tableNotifications.setFixedCellSize(-1);
    }

    private static void openAuctionRoom(Auction selectedAuction, Stage currentStage) {
        SessionManager session = SessionManager.getInstance();
        if (session.isBidder() || session.isAdmin()) {
            WindowManager.openInRoomWindow(selectedAuction, currentStage);
            return;
        }

        if (session.isSeller()) {
            if (selectedAuction.getSeller() != null && selectedAuction.getSeller().getId().equals(session.getUserId())) {
                WindowManager.openSellerInRoomWindow(selectedAuction, currentStage);
            } else {
                AlertHelper.showWarning("Cảnh báo", "Bạn chỉ có thể xem phòng đấu giá của chính mình!");
            }
            return;
        }

        AlertHelper.showWarning("Quyền truy cập", "Bạn không có quyền tham gia!");
    }
}
