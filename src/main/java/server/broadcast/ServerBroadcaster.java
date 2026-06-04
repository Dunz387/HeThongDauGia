package server.broadcast;

import model.auction.Auction;
import model.user.User;
import server.ClientHandler;
import service.auction.AuctionManager;
import service.user.UserService;
import shared.Protocol;

import java.util.List;
import java.util.concurrent.ExecutorService;

public final class ServerBroadcaster {
    private final List<ClientHandler> clients;
    private final AuctionManager manager;
    private final ExecutorService executor;

    public ServerBroadcaster(List<ClientHandler> clients, AuctionManager manager, ExecutorService executor) {
        this.clients = clients;
        this.manager = manager;
        this.executor = executor;
    }

    public void broadcast(String message) {
        executor.submit(() -> {
            for (ClientHandler client : clients) {
                client.sendData(message);
            }
        });
    }

    public void broadcastAuctionList() {
        executor.submit(() -> {
            List<Auction> auctions = manager.getAllAuctions();
            for (ClientHandler client : clients) {
                client.sendData(Protocol.RES_AUCTION_LIST);
                client.sendData(auctions);
            }
        });
    }

    public void broadcastUserList() {
        executor.submit(() -> {
            List<User> users = UserService.getInstance().getAllUsers();
            for (ClientHandler client : clients) {
                client.sendData(Protocol.RES_USER_LIST);
                client.sendData(users);
            }
        });
    }
}
