package model.auction;

public interface AuctionObserver {
    // Hàm này sẽ được gọi tự động khi có giá mới
    void update(Auction auction, double newPrice, String topBidderName);
}
