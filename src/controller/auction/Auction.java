package controller.auction;

import model.BidTransaction;
import model.Item.Item;
import model.UserType.User;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class Auction {
    private String auctionId;
    private Item item;
    private User seller;
    private double startingPrice;
    private double minIncrement;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private AuctionStatus status;

    private List<BidTransaction> bids;
    private BidTransaction currentHighestBid;

    // Constructor
    public Auction(String auctionId, Item item, User seller, double startingPrice, double minIncrement, LocalDateTime startTime, LocalDateTime endTime) {
        this.auctionId = auctionId;
        this.item = item;
        this.seller = seller;
        this.startingPrice = startingPrice;
        this.minIncrement = minIncrement;
        this.startTime = startTime;
        this.endTime = endTime;
        this.status = AuctionStatus.PENDING;
        this.bids = new ArrayList<>();
        this.currentHighestBid = null;
    }

    // Kích hoạt phiên đấu giá
    public void startAuction() {
        if (LocalDateTime.now().isAfter(startTime) && status == AuctionStatus.PENDING) {
            this.status = AuctionStatus.ACTIVE;
        }
    }

    // Xử lý logic đặt giá
    public synchronized boolean placeBid(User bidder, double amount) {
        // 1. Kiểm tra trạng thái và thời gian
        if (this.status != AuctionStatus.ACTIVE || LocalDateTime.now().isAfter(endTime)) {
            throw new IllegalStateException("Phiên đấu giá không hoạt động hoặc đã kết thúc.");
        }

        // 2. Kiểm tra người đặt giá không được là người bán
        if (bidder.equals(this.seller)) {
            throw new IllegalArgumentException("Người bán không thể tự ra giá.");
        }

        // 3. Kiểm tra số tiền hợp lệ (lớn hơn giá khởi điểm và lớn hơn giá cao nhất + bước giá)
        double requiredMinAmount = (currentHighestBid == null)
                ? startingPrice
                : currentHighestBid.getAmount() + minIncrement;

        if (amount < requiredMinAmount) {
            return false; // Báo lỗi: Giá đưa ra quá thấp
        }

        // 4. Ghi nhận lượt ra giá mới
        BidTransaction newBid = new BidTransaction(bidder, auctionId, amount);
        this.bids.add(newBid);
        this.currentHighestBid = newBid;

        return true; // Đặt giá thành công
    }

    // Kết thúc phiên đấu giá
    public void endAuction() {
        if (LocalDateTime.now().isAfter(endTime)) {
            this.status = AuctionStatus.ENDED;
            // Kích hoạt Event/Notification thông báo cho Winner và Seller tại đây
        }
    }

    // Getters
    public BidTransaction getCurrentHighestBid() {
        return currentHighestBid;
    }

    public User getWinningBidder() {
        if (status == AuctionStatus.ENDED && currentHighestBid != null) {
            return currentHighestBid.getBidder();
        }
        return null;
    }
}

// Enum hỗ trợ trạng thái
enum AuctionStatus {
    PENDING, ACTIVE, ENDED, CANCELED
}