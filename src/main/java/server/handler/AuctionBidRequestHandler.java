package server.handler;

import model.auction.Auction;
import model.user.Bidder;
import model.user.User;
import server.ClientHandler;
import service.auction.AuctionManager;
import shared.Protocol;

final class AuctionBidRequestHandler {
    private final ClientHandler clientHandler;
    private final AuctionManager manager;

    AuctionBidRequestHandler(ClientHandler clientHandler, AuctionManager manager) {
        this.clientHandler = clientHandler;
        this.manager = manager;
    }

    void handleBid(String auctionId, String amountStr) {
        try {
            if (!(getUser() instanceof Bidder)) {
                failBid("Chỉ người mua mới được đặt giá!");
                return;
            }

            Bidder bidder = (Bidder) getUser();
            Auction auction = manager.getAuctionById(auctionId);
            if (auction == null) {
                failBid("Không tìm thấy sản phẩm!");
                return;
            }

            String result = manager.processBid(bidder, auction, Double.parseDouble(amountStr));
            if ("Thành công!".equals(result)) {
                clientHandler.sendData(Protocol.REQ_BID + Protocol.DELIMITER + Protocol.RES_SUCCESS);
                clientHandler.sendData(Protocol.RES_UPDATE_BALANCE + Protocol.DELIMITER + bidder.getAvailableBalance());
            } else {
                failBid(result);
            }
        } catch (Exception e) {
            failBid("Lỗi hệ thống đặt giá.");
        }
    }

    void handleAutoBid(String auctionId, String maxBidStr, String incrementStr) {
        try {
            if (!(getUser() instanceof Bidder)) {
                failAutoBid("Chỉ người mua mới được đặt auto-bid!");
                return;
            }

            Bidder bidder = (Bidder) getUser();
            Auction auction = manager.getAuctionById(auctionId);
            if (auction == null) {
                failAutoBid("Không tìm thấy sản phẩm!");
                return;
            }

            if ("CANCEL".equalsIgnoreCase(maxBidStr)) {
                auction.cancelAutoBid(bidder);
                clientHandler.sendData(Protocol.REQ_AUTOBID + Protocol.DELIMITER
                        + Protocol.RES_SUCCESS + Protocol.DELIMITER + "CANCEL");
                return;
            }

            auction.registerAutoBid(bidder, Double.parseDouble(maxBidStr), parseIncrement(incrementStr));
            clientHandler.sendData(Protocol.REQ_AUTOBID + Protocol.DELIMITER
                    + Protocol.RES_SUCCESS + Protocol.DELIMITER + "REGISTER");
        } catch (Exception e) {
            failAutoBid("Lỗi hệ thống auto-bid.");
        }
    }

    private double parseIncrement(String incrementStr) {
        if (incrementStr == null || incrementStr.trim().isEmpty()) {
            return 0.0;
        }
        return Double.parseDouble(incrementStr);
    }

    private void failBid(String message) {
        clientHandler.sendData(Protocol.REQ_BID + Protocol.DELIMITER
                + Protocol.RES_FAIL + Protocol.DELIMITER + message);
    }

    private void failAutoBid(String message) {
        clientHandler.sendData(Protocol.REQ_AUTOBID + Protocol.DELIMITER
                + Protocol.RES_FAIL + Protocol.DELIMITER + message);
    }

    private User getUser() {
        return clientHandler.getLoggedInUser();
    }
}
