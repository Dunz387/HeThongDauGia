package view.utility.menu;

import javafx.application.Platform;
import javafx.scene.control.Label;
import model.auction.Auction;
import network.ClientNetworkManager;
import network.SessionManager;
import shared.Protocol;
import view.utility.auction.RoleBasedFilterHelper;

import java.util.List;

public final class UserInfoSupport {
    private UserInfoSupport() {
    }

    public static void loadUserData(
            Label lblUsername,
            Label lblId,
            Label lblBalance,
            Label lblAssetsCount,
            Label lblRole) {
        SessionManager session = SessionManager.getInstance();
        if (!session.isLoggedIn()) {
            return;
        }

        lblUsername.setText(session.getUsername());
        lblId.setText("ID: " + session.getUserId());
        lblBalance.setText("$" + String.format("%.2f", session.getBalance()));
        lblRole.setText(session.getRole().toString());
        lblAssetsCount.setText("Đang tải...");
    }

    public static void bindBalance(Label lblBalance) {
        SessionManager.getInstance().balanceProperty().addListener((obs, oldVal, newVal) ->
                Platform.runLater(() -> lblBalance.setText("$" + String.format("%.2f", newVal.doubleValue()))));
    }

    public static void updateAssetsCount(List<Auction> auctionList, Label lblAssetsCount) {
        if (auctionList == null) {
            return;
        }
        long count = RoleBasedFilterHelper.countAssets(auctionList);
        Platform.runLater(() -> lblAssetsCount.setText(count + " sản phẩm"));
    }

    public static void requestLatestAuctions() {
        ClientNetworkManager.getInstance().sendData(Protocol.REQ_GET_AUCTIONS);
    }
}
