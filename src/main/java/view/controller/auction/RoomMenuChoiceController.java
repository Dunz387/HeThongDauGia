package view.controller.auction;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.Button;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import model.auction.Auction;
import view.utility.auction.AuctionNetworkHelper;
import view.utility.auction.AuctionTableConfigurator;
import view.utility.navigation.SceneManager;

import java.net.URL;
import java.util.ResourceBundle;

public class RoomMenuChoiceController implements Initializable {

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

    @FXML private HBox sellerActionBox;
    @FXML private Button btnCreateAuction;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        configureSellerActionsVisibility();
        configureAuctionTable();
        configureOpenRoomAction();
        registerAuctionListListener();
        setupContextMenu();
    }

    private void configureSellerActionsVisibility() {
        if (!network.SessionManager.getInstance().isSeller()) {
            if (sellerActionBox != null) {
                sellerActionBox.setVisible(false);
                sellerActionBox.setManaged(false);
            }
        }
        // Cấu hình bảng thống nhất (SRP: delegate sang AuctionTableConfigurator)
    }

    private void configureAuctionTable() {
        AuctionTableConfigurator.configure(colId, colName, colDescription, colType, colPrice,
                colBidCount, colHighestBidder, colEndTime, colStatus, colSeller);
    }

        // Nhấp đúp để vào phòng đấu giá
    private void configureOpenRoomAction() {
        tableAuctions.setRowFactory(tv -> {
            TableRow<Auction> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && (!row.isEmpty())) {
                    Auction selectedAuction = row.getItem();
                    System.out.println("🏛️ Đang vào phòng đấu giá: " + selectedAuction.getId());
                    Stage currentStage = (Stage) tableAuctions.getScene().getWindow();
                    
                    if (network.SessionManager.getInstance().isBidder() || network.SessionManager.getInstance().isAdmin()) {
                        view.utility.navigation.WindowManager.openInRoomWindow(selectedAuction, currentStage);
                    } else if (network.SessionManager.getInstance().isSeller()) {
                        if (selectedAuction.getSeller() != null && selectedAuction.getSeller().getId().equals(network.SessionManager.getInstance().getUserId())) {
                            view.utility.navigation.WindowManager.openSellerInRoomWindow(selectedAuction, currentStage);
                        } else {
                            view.utility.display.AlertHelper.showWarning("Cảnh báo", "Bạn chỉ có thể xem phòng đấu giá của chính mình!");
                        }
                    }
                }
            });
            return row;
        });

        // Đăng ký lắng nghe với bộ lọc theo Role (SRP: delegate sang RoleBasedFilterHelper)
    }

    private void registerAuctionListListener() {
        AuctionNetworkHelper.registerAuctionListListener(tableAuctions, view.utility.auction.RoleBasedFilterHelper.getRoomFilter());

        // Thêm Context Menu cho tính năng Sửa/Xóa của Seller/Admin
    }

    private void setupContextMenu() {
        view.utility.auction.AuctionContextMenuHelper.setupContextMenu(tableAuctions);
    }

    @FXML
    private void backToBaseMenu(ActionEvent event) {
        Stage stage = (Stage) ((javafx.scene.Node) event.getSource()).getScene().getWindow();
        SceneManager.goToBaseMenu(stage);
    }

    @FXML
    private void goToCreateItem(ActionEvent event) {
        Stage stage = (Stage) ((javafx.scene.Node) event.getSource()).getScene().getWindow();
        view.utility.navigation.WindowManager.openCreateItemWindow(stage);
    }
}
