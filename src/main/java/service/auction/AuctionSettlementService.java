package service.auction;

import dao.auction.AuctionDAO;
import dao.user.UserDAO;
import model.auction.Auction;
import model.auction.AuctionStatus;
import model.user.Bidder;
import model.user.Seller;
import model.user.User;

import java.util.function.BiConsumer;
import java.util.logging.Logger;

final class AuctionSettlementService {
    private static final Logger LOGGER = Logger.getLogger(AuctionSettlementService.class.getName());

    private AuctionSettlementService() {
    }

    static void concludeAuction(Auction auction, BiConsumer<Auction, User> callback) {
        if (auction == null || auction.getStatus() != AuctionStatus.RUNNING) {
            return;
        }

        auction.setStatus(AuctionStatus.FINISHED);
        User seller = auction.getSeller();
        settleWinningBid(auction, seller);

        AuctionDAO.updateAuction(auction);
        LOGGER.info(String.format("[AuctionManager] Phiên %s đã kết thúc.", auction.getId()));

        if (callback != null) {
            callback.accept(auction, seller);
        }
    }

    private static void settleWinningBid(Auction auction, User seller) {
        Bidder highestBidder = auction.getHighestBidder();
        if (highestBidder == null) {
            return;
        }

        double winPrice = auction.getCurrentPrice();
        if (!highestBidder.deductBalance(winPrice)) {
            return;
        }

        UserDAO.updateUserBalance(highestBidder.getId(), highestBidder.getBalance());
        auction.getItem().setOwner(highestBidder);
        paySeller(seller, winPrice);
        LOGGER.info(String.format("[THANH TOAN] Đã chuyển %.2f từ %s cho %s",
                winPrice, highestBidder.getUsername(), sellerName(seller)));
    }

    private static void paySeller(User seller, double winPrice) {
        if (seller instanceof Seller) {
            Seller sellerAccount = (Seller) seller;
            sellerAccount.receivePayment(winPrice);
            UserDAO.updateUserBalance(seller.getId(), sellerAccount.getBalance());
        }
    }

    private static String sellerName(User seller) {
        return seller != null ? seller.getUsername() : "Hệ thống";
    }
}
