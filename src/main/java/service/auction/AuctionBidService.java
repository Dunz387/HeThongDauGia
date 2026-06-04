package service.auction;

import dao.auction.AuctionDAO;
import model.auction.Auction;
import model.auction.AuctionStatus;
import model.auction.BidTransaction;
import model.user.Bidder;

import java.util.List;

final class AuctionBidService {
    private AuctionBidService() {
    }

    static String processBid(Bidder bidder, Auction auction, double bidAmount) {
        if (auction == null || bidder == null) {
            return "Lỗi dữ liệu";
        }
        if (auction.getStatus() != AuctionStatus.RUNNING) {
            return "Phiên đã kết thúc";
        }
        try {
            auction.placeBid(bidder, bidAmount);
            AuctionDAO.updateAuction(auction);
            saveLatestBid(auction);
            return "Thành công!";
        } catch (Exception e) {
            return e.getMessage();
        }
    }

    private static void saveLatestBid(Auction auction) {
        List<BidTransaction> history = auction.getBidHistory();
        if (!history.isEmpty()) {
            AuctionDAO.saveBidTransaction(history.get(history.size() - 1));
        }
    }
}
