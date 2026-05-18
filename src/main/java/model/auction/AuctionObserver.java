package model.auction;

import model.user.Bidder;

public interface AuctionObserver {
    // Hàm này sẽ được gọi tự động khi có giá mới
    void update(Auction auction, double newPrice, String topBidderName, Bidder previousBidder);
    
    // Tự động cộng thêm giây nếu bị đấu giá vào phút chót (Anti-sniping)
    default void onTimeExtended(Auction auction, int addedSeconds) {}

    // Thông báo khi Auto-Bid bị vượt giá hoặc hết hạn
    default void onAutoBidExpired(Auction auction, Bidder bidder) {}
}
