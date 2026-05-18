package view.controller.seller;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.stage.Stage;
import model.auction.Auction;
import network.ClientNetworkManager;
import shared.Protocol;
import view.utility.AlertHelper;
import view.utility.SceneManager;
import view.utility.AuctionTableConfigurator;

import java.net.URL;
import java.util.ResourceBundle;

/**
 * Controller cho màn hình hiển thị phiên đấu giá vừa tạo của Seller.
 */
public class SellerAuctionListController implements Initializable {

    @FXML
    private TableView<Auction> tableMyAuction;
    @FXML
    private TableColumn<Auction, String> colId;
    @FXML
    private TableColumn<Auction, String> colName;
    @FXML
    private TableColumn<Auction, String> colDescription;
    @FXML
    private TableColumn<Auction, String> colType;
    @FXML
    private TableColumn<Auction, Double> colPrice;
    @FXML
    private TableColumn<Auction, Integer> colBidCount;
    @FXML
    private TableColumn<Auction, String> colHighestBidder;
    @FXML
    private TableColumn<Auction, String> colEndTime;
    @FXML
    private TableColumn<Auction, String> colStatus;
    @FXML
    private TableColumn<Auction, String> colSeller;
    @FXML
    private Button btnMonitor;

    private Auction latestAuction = null;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // === CẤU HÌNH CỘT BẢNG ===
        AuctionTableConfigurator.configure(colId, colName, colDescription, colType, colPrice, colBidCount,
                colHighestBidder, colEndTime, colStatus, colSeller);

        // === LẮNG NGHE DANH SÁCH ĐẤU GIÁ TỪ SERVER ===
        ClientNetworkManager.getInstance().clearAuctionListListeners();
        ClientNetworkManager.getInstance().addAuctionListListener((listFromServer) -> {
            if (listFromServer != null && !listFromServer.isEmpty()) {
                latestAuction = listFromServer.get(listFromServer.size() - 1);
                Platform.runLater(() -> {
                    ObservableList<Auction> data = FXCollections.observableArrayList(latestAuction);
                    tableMyAuction.setItems(data);
                    btnMonitor.setDisable(false);
                });
            }
        });

        ClientNetworkManager.getInstance().sendData(Protocol.REQ_GET_AUCTIONS);
    }

    @FXML
    private void monitorButtonClicked(ActionEvent event) {
        if (latestAuction == null) {
            AlertHelper.showWarning("Lỗi", "Không tìm thấy phiên đấu giá!");
            return;
        }
        Stage stage = (Stage) tableMyAuction.getScene().getWindow();
        Stage mainStage = (Stage) stage.getOwner();
        stage.close();
        if (mainStage != null) {
            // Truyền auctionId thực tế vào phòng theo dõi
            SceneManager.goToSellerInRoom(mainStage, latestAuction);
        }
    }

    @FXML
    private void backToMenuClicked(ActionEvent event) {
        Stage stage = (Stage) tableMyAuction.getScene().getWindow();
        Stage mainStage = (Stage) stage.getOwner();
        stage.close();
        if (mainStage != null) {
            SceneManager.goToBaseMenu(mainStage);
        }
    }
}
