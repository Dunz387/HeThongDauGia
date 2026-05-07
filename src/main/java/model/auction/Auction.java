package model.auction;

import exception.AuctionClosedException;
import exception.InvalidBidException;
import model.base.Entity;
import model.item.Item;
import model.user.Bidder;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

public class Auction extends Entity implements AuctionSubject {

    private Item item;
    private double startingPrice;
    private double currentPrice;
    private double bidIncrement;

    private Bidder highestBidder;
    private LocalDateTime endTime;
    private AuctionStatus status;
    private String reason;

    private List<BidTransaction> bidHistory;

    // Dùng transient để bỏ qua khi lưu file/gửi mạng
    private transient List<AuctionObserver> observers;
    private transient ReentrantLock lock = new ReentrantLock();

    public Auction(String id, Item item, double startingPrice, double bidIncrement, LocalDateTime endTime) {
        super(id);
        this.item = item;
        this.startingPrice = startingPrice;
        this.currentPrice = startingPrice;
        this.bidIncrement = bidIncrement;
        this.endTime = endTime;

        this.highestBidder = null;
        this.status = AuctionStatus.OPEN;
        this.bidHistory = new ArrayList<>();
        this.observers = new ArrayList<>();
    }

    public Item getItem() { return item; }
    public double getCurrentPrice() { return currentPrice; }
    public Bidder getHighestBidder() { return highestBidder; }
    public LocalDateTime getEndTime() { return endTime; }
    public AuctionStatus getStatus() { return status; }
    public void setStatus(AuctionStatus status) { this.status = status; }

    public void placeBid(Bidder bidder, double bidAmount) throws InvalidBidException, AuctionClosedException {
        if (lock == null) lock = new ReentrantLock();
        lock.lock();
        try {
            if (this.status != AuctionStatus.RUNNING) {
                throw new AuctionClosedException("Phiên đấu giá không trong trạng thái đang diễn ra!");
            }

            double minRequiredBid = this.currentPrice + bidIncrement;
            if (bidAmount < minRequiredBid) {
                throw new InvalidBidException(String.format("Giá đặt phải lớn hơn hoặc bằng $%.2f", minRequiredBid));
            }

            Bidder previousBidder = this.highestBidder;
            double previousBidAmount = this.currentPrice;

            if (!bidder.lockBalance(bidAmount)) {
                throw new InvalidBidException("Không đủ số dư khả dụng (Tiền của bạn có thể đang bị giam ở phòng khác).");
            }

            if (previousBidder != null) {
                previousBidder.unlockBalance(previousBidAmount);
            }

            this.currentPrice = bidAmount;
            this.highestBidder = bidder;

            BidTransaction transaction = new BidTransaction("TX-" + System.currentTimeMillis(), this, bidder, bidAmount, java.time.LocalDateTime.now());
            if (bidHistory == null) bidHistory = new ArrayList<>();
            bidHistory.add(transaction);

            // Báo cho Server biết có người vừa đặt giá!
            notifyObservers();

        } finally {
            lock.unlock();
        }
    }

    // --- CÁC HÀM CỦA AUCTION SUBJECT ---
    @Override
    public void addObserver(AuctionObserver observer) {
        if (observers == null) {
            observers = new ArrayList<>();
        }
        if (!observers.contains(observer)) {
            observers.add(observer);
        }
    }

    @Override
    public void removeObserver(AuctionObserver observer) {
        if (observers != null) {
            observers.remove(observer);
        }
    }

    @Override
    public void notifyObservers() {
        if (observers != null) {
            for (AuctionObserver obs : observers) {
                String topBidderName = (highestBidder != null) ? highestBidder.getUsername() : "Chưa có";
                obs.update(this, this.currentPrice, topBidderName);
            }
        }
    }
}