package view.controller.auction;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import model.auction.Auction;
import view.utility.auction.RoomMenuChoiceSupport;
import view.utility.navigation.SceneManager;
import view.utility.navigation.WindowManager;

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
        new RoomMenuChoiceSupport(
                tableAuctions,
                colId,
                colName,
                colDescription,
                colType,
                colPrice,
                colBidCount,
                colHighestBidder,
                colEndTime,
                colStatus,
                colSeller,
                sellerActionBox
        ).initialize();
    }

    @FXML
    private void backToBaseMenu(ActionEvent event) {
        SceneManager.goToBaseMenu(stageFrom(event));
    }

    @FXML
    private void goToCreateItem(ActionEvent event) {
        WindowManager.openCreateItemWindow(stageFrom(event));
    }

    private Stage stageFrom(ActionEvent event) {
        return (Stage) ((Node) event.getSource()).getScene().getWindow();
    }
}
