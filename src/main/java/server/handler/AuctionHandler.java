package server.handler;

import model.auction.Auction;
import model.user.User;
import server.AuctionServer;
import server.ClientHandler;
import service.auction.AuctionManager;
import shared.Protocol;

import java.util.List;
import java.util.logging.Logger;

/**
 * Router xử lý request đấu giá và delegate phần nghiệp vụ sang helper chuyên trách.
 */
public class AuctionHandler {
    private static final Logger LOGGER = Logger.getLogger(AuctionHandler.class.getName());
    private final ClientHandler clientHandler;
    private final AuctionServer server;
    private final AuctionManager manager;
    private final AuctionItemRequestHandler itemHandler;
    private final AuctionBidRequestHandler bidHandler;

    public AuctionHandler(ClientHandler clientHandler, AuctionServer server, AuctionManager manager) {
        this.clientHandler = clientHandler;
        this.server = server;
        this.manager = manager;
        this.itemHandler = new AuctionItemRequestHandler(clientHandler, server, manager);
        this.bidHandler = new AuctionBidRequestHandler(clientHandler, manager);
    }

    public void handleGetAuctions() {
        List<Auction> list = manager.getAllAuctions();
        clientHandler.sendData(Protocol.RES_AUCTION_LIST);
        clientHandler.sendData(list);
    }

    public void handleCreateItem(String name, String priceStr, String durStr, String itemType, String itemDesc) {
        itemHandler.handleCreateItem(name, priceStr, durStr, itemType, itemDesc);
    }

    public void handleUpdateItem(String auctionId, String newName, String newDesc, String newType,
            String newPriceStr, String newDurStr) {
        itemHandler.handleUpdateItem(auctionId, newName, newDesc, newType, newPriceStr, newDurStr);
    }

    public void handleDeleteItem(String auctionId) {
        itemHandler.handleDeleteItem(auctionId);
    }

    public void handleBid(String auctionId, String amountStr) {
        bidHandler.handleBid(auctionId, amountStr);
    }

    public void handleAutoBid(String auctionId, String maxBidStr, String incrementStr) {
        bidHandler.handleAutoBid(auctionId, maxBidStr, incrementStr);
    }

    public void handleJoinRoom(String auctionId) {
        if (!clientHandler.getCurrentRoomIds().contains(auctionId)) {
            clientHandler.getCurrentRoomIds().add(auctionId);
            server.joinRoom(auctionId, clientHandler);
            LOGGER.info(String.format("User %s đã vào phòng %s", username(), auctionId));
        }
    }

    public void handleLeaveRoom(String auctionId) {
        if (clientHandler.getCurrentRoomIds().contains(auctionId)) {
            server.leaveRoom(auctionId, clientHandler);
            clientHandler.getCurrentRoomIds().remove(auctionId);
            LOGGER.info(String.format("User %s đã rời phòng %s", username(), auctionId));
        }
    }

    private String username() {
        User user = clientHandler.getLoggedInUser();
        return user != null ? user.getUsername() : "Guest";
    }
}
