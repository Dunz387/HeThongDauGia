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
    
    // T10: Lưu lại thời gian có hoạt động (đặt giá) gần nhất để tính giờ vòng đấu
    private transient LocalDateTime lastActivityTime = LocalDateTime.now();

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

    public Item getItem() {
        return item;
    }

    public double getStartingPrice() {
        return startingPrice;
    }

    public double getCurrentPrice() {
        return currentPrice;
    }

    public void setStartingPrice(double startingPrice) {
        this.startingPrice = startingPrice;
    }

    /**
     * Setter cho currentPrice — dùng khi khôi phục dữ liệu từ DB.
     * KHÔNG dùng trong logic đấu giá bình thường (dùng placeBid() thay thế).
     */
    public void setCurrentPrice(double currentPrice) {
        this.currentPrice = currentPrice;
    }

    public double getBidIncrement() {
        return bidIncrement;
    }

    public Bidder getHighestBidder() {
        return highestBidder;
    }

    /**
     * Setter cho highestBidder — dùng khi khôi phục dữ liệu từ DB.
     * KHÔNG dùng trong logic đấu giá bình thường (dùng placeBid() thay thế).
     */
    public void setHighestBidder(Bidder highestBidder) {
        this.highestBidder = highestBidder;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

    public AuctionStatus getStatus() {
        return status;
    }

    public void setStatus(AuctionStatus status) {
        this.status = status;
    }

    public List<BidTransaction> getBidHistory() {
        if (bidHistory == null)
            bidHistory = new ArrayList<>();
        return bidHistory;
    }

    public void placeBid(Bidder bidder, double bidAmount) throws InvalidBidException, AuctionClosedException {
        if (lock == null)
            lock = new ReentrantLock();
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
                throw new InvalidBidException(
                        "Không đủ số dư khả dụng (Tiền của bạn có thể đang bị giam ở phòng khác).");
            }

            if (previousBidder != null) {
                previousBidder.unlockBalance(previousBidAmount);
            }

            this.currentPrice = bidAmount;
            this.highestBidder = bidder;

            BidTransaction transaction = new BidTransaction("TX-" + System.currentTimeMillis(), this, bidder, bidAmount,
                    java.time.LocalDateTime.now());
            if (bidHistory == null)
                bidHistory = new ArrayList<>();
            bidHistory.add(transaction);
            
            this.lastActivityTime = LocalDateTime.now(); // Cập nhật thời gian khi có bid mới

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

    public LocalDateTime getLastActivityTime() {
        if (lastActivityTime == null) lastActivityTime = LocalDateTime.now();
        return lastActivityTime;
    }

    public void updateActivityTime() {
        this.lastActivityTime = LocalDateTime.now();
    }
}