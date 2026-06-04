package view.utility.admin;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import model.auction.Auction;
import model.user.User;
import network.client.ClientNetworkManager;
import shared.Protocol;

import java.util.List;

public class AdminDashboardDataBinder {
    private final Label totalUsersLabel;
    private final Label totalAuctionsLabel;
    private final Label runningAuctionsLabel;
    private final Label finishedAuctionsLabel;
    private final TableView<Auction> recentAuctionsTable;
    private final TableView<User> recentUsersTable;

    public AdminDashboardDataBinder(
            Label totalUsersLabel,
            Label totalAuctionsLabel,
            Label runningAuctionsLabel,
            Label finishedAuctionsLabel,
            TableView<Auction> recentAuctionsTable,
            TableView<User> recentUsersTable
    ) {
        this.totalUsersLabel = totalUsersLabel;
        this.totalAuctionsLabel = totalAuctionsLabel;
        this.runningAuctionsLabel = runningAuctionsLabel;
        this.finishedAuctionsLabel = finishedAuctionsLabel;
        this.recentAuctionsTable = recentAuctionsTable;
        this.recentUsersTable = recentUsersTable;
    }

    public void registerListeners() {
        ClientNetworkManager.getInstance().clearAuctionListListeners();
        ClientNetworkManager.getInstance().addAuctionListListener(this::updateAuctions);
        ClientNetworkManager.getInstance().clearUserListListeners();
        ClientNetworkManager.getInstance().addUserListListener(this::updateUsers);
    }

    public void load() {
        ClientNetworkManager.getInstance().sendData(Protocol.REQ_GET_AUCTIONS);
        ClientNetworkManager.getInstance().sendData(Protocol.REQ_GET_USERS);
    }

    private void updateAuctions(List<Auction> auctions) {
        if (auctions == null) {
            return;
        }

        Platform.runLater(() -> {
            totalAuctionsLabel.setText(String.valueOf(auctions.size()));
            long running = auctions.stream().filter(a -> "RUNNING".equals(a.getStatus().name())).count();
            long finished = auctions.stream().filter(a -> "FINISHED".equals(a.getStatus().name())).count();
            runningAuctionsLabel.setText(String.valueOf(running));
            finishedAuctionsLabel.setText(String.valueOf(finished));
            recentAuctionsTable.setItems(recentItems(auctions));
        });
    }

    private void updateUsers(List<User> users) {
        if (users == null) {
            return;
        }

        Platform.runLater(() -> {
            totalUsersLabel.setText(String.valueOf(users.size()));
            recentUsersTable.setItems(recentItems(users));
        });
    }

    private static <T> ObservableList<T> recentItems(List<T> items) {
        int limit = Math.min(5, items.size());
        return FXCollections.observableArrayList(items.subList(Math.max(0, items.size() - limit), items.size()));
    }
}
