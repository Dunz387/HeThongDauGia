package view.utility.auction;

import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import model.auction.Auction;
import network.session.SessionManager;
import view.utility.display.AlertHelper;
import view.utility.navigation.WindowManager;

public class RoomMenuChoiceSupport {
    private final TableView<Auction> tableAuctions;
    private final TableColumn<Auction, String> colId;
    private final TableColumn<Auction, String> colName;
    private final TableColumn<Auction, String> colDescription;
    private final TableColumn<Auction, String> colType;
    private final TableColumn<Auction, Double> colPrice;
    private final TableColumn<Auction, Integer> colBidCount;
    private final TableColumn<Auction, String> colHighestBidder;
    private final TableColumn<Auction, String> colEndTime;
    private final TableColumn<Auction, String> colStatus;
    private final TableColumn<Auction, String> colSeller;
    private final HBox sellerActionBox;

    public RoomMenuChoiceSupport(
            TableView<Auction> tableAuctions,
            TableColumn<Auction, String> colId,
            TableColumn<Auction, String> colName,
            TableColumn<Auction, String> colDescription,
            TableColumn<Auction, String> colType,
            TableColumn<Auction, Double> colPrice,
            TableColumn<Auction, Integer> colBidCount,
            TableColumn<Auction, String> colHighestBidder,
            TableColumn<Auction, String> colEndTime,
            TableColumn<Auction, String> colStatus,
            TableColumn<Auction, String> colSeller,
            HBox sellerActionBox
    ) {
        this.tableAuctions = tableAuctions;
        this.colId = colId;
        this.colName = colName;
        this.colDescription = colDescription;
        this.colType = colType;
        this.colPrice = colPrice;
        this.colBidCount = colBidCount;
        this.colHighestBidder = colHighestBidder;
        this.colEndTime = colEndTime;
        this.colStatus = colStatus;
        this.colSeller = colSeller;
        this.sellerActionBox = sellerActionBox;
    }

    public void initialize() {
        configureSellerActionsVisibility();
        AuctionTableConfigurator.configure(colId, colName, colDescription, colType, colPrice,
                colBidCount, colHighestBidder, colEndTime, colStatus, colSeller);
        configureOpenRoomAction();
        AuctionNetworkHelper.registerAuctionListListener(tableAuctions, RoleBasedFilterHelper.getRoomFilter());
        AuctionContextMenuHelper.setupContextMenu(tableAuctions);
    }

    private void configureSellerActionsVisibility() {
        if (!SessionManager.getInstance().isSeller() && sellerActionBox != null) {
            sellerActionBox.setVisible(false);
            sellerActionBox.setManaged(false);
        }
    }

    private void configureOpenRoomAction() {
        tableAuctions.setRowFactory(tv -> {
            TableRow<Auction> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    openRoom(row.getItem());
                }
            });
            return row;
        });
    }

    private void openRoom(Auction selectedAuction) {
        Stage currentStage = (Stage) tableAuctions.getScene().getWindow();
        SessionManager session = SessionManager.getInstance();

        if (session.isBidder() || session.isAdmin()) {
            WindowManager.openInRoomWindow(selectedAuction, currentStage);
            return;
        }

        if (session.isSeller() && isOwnedByCurrentSeller(selectedAuction, session.getUserId())) {
            WindowManager.openSellerInRoomWindow(selectedAuction, currentStage);
        } else {
            AlertHelper.showWarning("Cảnh báo", "Bạn chỉ có thể xem phòng đấu giá của chính mình!");
        }
    }

    private boolean isOwnedByCurrentSeller(Auction auction, String userId) {
        return auction.getSeller() != null && auction.getSeller().getId().equals(userId);
    }
}
