package view.utility.auction;

import network.ClientNetworkManager;
import shared.Protocol;

/**
 * Encapsulates auction-room network commands so controllers do not build
 * protocol messages directly.
 */
public class AuctionRoomCommandService {
    private final ClientNetworkManager networkManager;

    public AuctionRoomCommandService() {
        this(ClientNetworkManager.getInstance());
    }

    AuctionRoomCommandService(ClientNetworkManager networkManager) {
        this.networkManager = networkManager;
    }

    public boolean joinRoom(String auctionId) {
        return networkManager.sendData(Protocol.REQ_JOIN_ROOM + Protocol.DELIMITER + auctionId);
    }

    public boolean placeBid(String auctionId, double amount) {
        return networkManager.sendData(
                Protocol.REQ_BID + Protocol.DELIMITER + auctionId + Protocol.DELIMITER + amount);
    }

    public boolean enableAutoBid(String auctionId, double maxBid) {
        return networkManager.sendData(
                Protocol.REQ_AUTOBID + Protocol.DELIMITER + auctionId + Protocol.DELIMITER + maxBid);
    }

    public boolean cancelAutoBid(String auctionId) {
        return networkManager.sendData(
                Protocol.REQ_AUTOBID + Protocol.DELIMITER + auctionId + Protocol.DELIMITER + "CANCEL");
    }

    public boolean kickUser(String auctionId, String username) {
        return networkManager.sendData(
                Protocol.REQ_KICK_USER + Protocol.DELIMITER + auctionId + Protocol.DELIMITER + username);
    }
}
