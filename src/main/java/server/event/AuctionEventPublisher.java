package server.event;

import model.auction.Auction;
import model.user.Bidder;
import model.user.Seller;
import model.user.User;
import server.broadcast.ServerBroadcaster;
import server.notification.ClientNotifier;
import service.auction.AuctionManager;
import shared.Protocol;

import java.util.logging.Logger;

public final class AuctionEventPublisher {
    private static final Logger LOGGER = Logger.getLogger(AuctionEventPublisher.class.getName());
    private final AuctionManager manager;
    private final ServerBroadcaster broadcaster;
    private final ClientNotifier clientNotifier;

    public AuctionEventPublisher(AuctionManager manager, ServerBroadcaster broadcaster, ClientNotifier clientNotifier) {
        this.manager = manager;
        this.broadcaster = broadcaster;
        this.clientNotifier = clientNotifier;
    }

    public void registerAuctionFinishedCallback() {
        manager.setAuctionFinishedCallback((finishedAuction, originalSeller) -> {
            broadcaster.broadcast(finishedMessage(finishedAuction));
            broadcaster.broadcastAuctionList();
            notifyFinalBalanceChanges(finishedAuction, originalSeller);
        });
    }

    public void publishNewBid(Auction auction, double newPrice, String topBidderName, Bidder previousBidder) {
        String message = Protocol.BROADCAST_NEW_BID + Protocol.DELIMITER
                + auction.getId() + Protocol.DELIMITER
                + newPrice + Protocol.DELIMITER
                + topBidderName;
        LOGGER.info("[BROADCAST] Đã phát sóng giá mới: " + message);
        broadcaster.broadcast(message);

        if (previousBidder != null) {
            clientNotifier.sendBalanceUpdateToUser(previousBidder.getId());
        }
        if (auction.getHighestBidder() != null) {
            clientNotifier.sendBalanceUpdateToUser(auction.getHighestBidder().getId());
        }
    }

    public void publishTimeExtended(Auction auction, int addedSeconds) {
        String message = Protocol.BROADCAST_TIME_EXTENDED + Protocol.DELIMITER
                + auction.getId() + Protocol.DELIMITER + addedSeconds;
        LOGGER.info("[BROADCAST] Gia hạn phiên đấu giá " + auction.getId() + " thêm " + addedSeconds + "s");
        broadcaster.broadcast(message);
    }

    public void publishAutoBidExpired(Bidder bidder) {
        clientNotifier.notifyAutoBidExpired(bidder);
    }

    private String finishedMessage(Auction finishedAuction) {
        String winnerName = finishedAuction.getHighestBidder() != null
                ? finishedAuction.getHighestBidder().getUsername()
                : "Không có";
        return Protocol.BROADCAST_AUCTION_FINISHED + Protocol.DELIMITER
                + finishedAuction.getId() + Protocol.DELIMITER
                + winnerName + Protocol.DELIMITER
                + finishedAuction.getCurrentPrice();
    }

    private void notifyFinalBalanceChanges(Auction finishedAuction, User originalSeller) {
        Bidder winner = finishedAuction.getHighestBidder();
        if (winner != null) {
            clientNotifier.sendBalanceUpdateToUser(winner.getId());
        }
        if (originalSeller instanceof Seller) {
            clientNotifier.sendBalanceUpdateToUser(originalSeller.getId());
        }
    }
}
