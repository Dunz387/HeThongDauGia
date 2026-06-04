package view.controller.menu;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.stage.Stage;
import network.client.ClientNetworkManager;
import view.utility.menu.UserInfoSupport;

import java.net.URL;
import java.util.ResourceBundle;

public class UserInfoController implements Initializable {
    @FXML private Label lblUsername;
    @FXML private Label lblId;
    @FXML private Label lblBalance;
    @FXML private Label lblAssetsCount;
    @FXML private Label lblRole;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        UserInfoSupport.loadUserData(lblUsername, lblId, lblBalance, lblAssetsCount, lblRole);
        ClientNetworkManager.getInstance().addAuctionListListener(this::updateAssetsCount);
        UserInfoSupport.bindBalance(lblBalance);
        UserInfoSupport.requestLatestAuctions();
    }

    private void updateAssetsCount(java.util.List<model.auction.Auction> auctionList) {
        UserInfoSupport.updateAssetsCount(auctionList, lblAssetsCount);
    }

    @FXML
    private void closeWindow(ActionEvent event) {
        ClientNetworkManager.getInstance().removeAuctionListListener(this::updateAssetsCount);
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.close();
    }
}
