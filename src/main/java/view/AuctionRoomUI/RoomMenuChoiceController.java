package view.AuctionRoomUI;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.stage.Stage;
import model.auction.Auction;
import view.utility.AuctionNetworkHelper;
import view.utility.AuctionTableConfigurator;
import view.utility.SceneManager;

import java.net.URL;
import java.util.ResourceBundle;

public class RoomMenuChoiceController implements Initializable {

    @FXML private TableView<Auction> tableAuctions;
    @FXML private TableColumn<Auction, String> colId;
    @FXML private TableColumn<Auction, String> colName;
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
        AuctionTableConfigurator.configure(colId, colName, colType, colPrice,
                colBidCount, colHighestBidder, colEndTime, colStatus, colSeller);

        // Nhấp đúp để vào phòng đấu giá
        tableAuctions.setRowFactory(tv -> {
            TableRow<Auction> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && (!row.isEmpty())) {
                    Auction selectedAuction = row.getItem();
                    System.out.println("🏛️ Đang vào phòng đấu giá: " + selectedAuction.getId());
                    Stage currentStage = (Stage) tableAuctions.getScene().getWindow();
                    // Truyền auctionId thực tế vào phòng đấu giá
                    SceneManager.goToInRoom(currentStage, selectedAuction.getId());
                }
            });
            return row;
        });

        // Đăng ký lắng nghe với bộ lọc chỉ hiển thị phiên RUNNING (SRP: delegate sang AuctionNetworkHelper)
        AuctionNetworkHelper.registerAuctionListListener(tableAuctions,
                a -> "RUNNING".equals(a.getStatus().name()));
    }

    @FXML
    private void backToBaseMenu(ActionEvent event) {
        Stage stage = (Stage) ((javafx.scene.Node) event.getSource()).getScene().getWindow();
        SceneManager.goToBaseMenu(stage);
    }
}
