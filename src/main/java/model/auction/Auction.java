package model.auction;

import exception.AuctionClosedException;
import exception.InvalidBidException;
import model.base.Entity;
import model.item.Item;
import model.user.Bidder;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;
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

    // Auto-bidding
    public static class AutoBidConfig implements Comparable<AutoBidConfig> {
        public Bidder bidder;
        public double maxBid;
        public double increment;
        public long registerTime;

        public AutoBidConfig(Bidder bidder, double maxBid, double increment, long registerTime) {
            this.bidder = bidder;
            this.maxBid = maxBid;
            this.increment = increment;
            this.registerTime = registerTime;
        }

        @Override
        public int compareTo(AutoBidConfig other) {
            return Long.compare(this.registerTime, other.registerTime);
        }
    }
    
    private transient PriorityQueue<AutoBidConfig> autoBids;

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
        this.autoBids = new PriorityQueue<>();
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
        return bidHistory;
    }

    public void registerAutoBid(Bidder bidder, double maxBid, double increment) {
        if (lock == null) {
            lock = new ReentrantLock();
        }
        lock.lock();
        try {
            if (autoBids == null) {
                autoBids = new PriorityQueue<>();
            }
            // Hủy cấu hình auto-bid cũ của bidder này nếu có để tránh trùng lặp
            autoBids.removeIf(config -> config.bidder.getId().equals(bidder.getId()));
            
            autoBids.add(new AutoBidConfig(bidder, maxBid, increment, System.currentTimeMillis()));
            triggerAutoBidding();
        } finally {
            lock.unlock();
        }
    }

    public void cancelAutoBid(Bidder bidder) {
        if (lock == null) {
            lock = new ReentrantLock();
        }
        lock.lock();
        try {
            if (autoBids == null) {
                autoBids = new PriorityQueue<>();
            }
            autoBids.removeIf(config -> config.bidder.getId().equals(bidder.getId()));
        } finally {
            lock.unlock();
        }
    }

    private transient boolean isAutoBidding = false;

    public void triggerAutoBidding() {
        if (lock == null) {
            lock = new ReentrantLock();
        }
        lock.lock();
        try {
            if (autoBids == null) {
                autoBids = new PriorityQueue<>();
            }
            if (autoBids.isEmpty() || isAutoBidding) return;
            
            isAutoBidding = true;
            try {
                while (true) {
                    // Cần copy ra một danh sách tạm để duyệt qua PriorityQueue
                    List<AutoBidConfig> activeAutoBids = new ArrayList<>(autoBids);
                    activeAutoBids.sort(null); // Sắp xếp theo registerTime
                    
                    boolean bidPlacedThisIteration = false;
                    for (AutoBidConfig config : activeAutoBids) {
                        // Nếu người này đang là người giữ giá cao nhất thì không cần tự động đặt
                        if (this.highestBidder != null && this.highestBidder.getId().equals(config.bidder.getId())) continue;
                        
                        double nextBid = this.currentPrice + Math.max(config.increment, getDynamicIncrement());
                        // Chỉ đặt nếu giá tiếp theo <= maxBid
                        if (nextBid <= config.maxBid) {
                            try {
                                placeBid(config.bidder, nextBid);
                                bidPlacedThisIteration = true;
                                break; // Phá vỡ vòng lặp để lượt while tiếp theo xử lý
                            } catch (Exception e) {
                                // Auto-bid thất bại (không đủ tiền, v.v.), xóa khỏi danh sách autoBids
                                autoBids.remove(config);
                            }
                        }
                    }
                    if (!bidPlacedThisIteration) break;
                }
            } finally {
                isAutoBidding = false;
            }
        } finally {
            lock.unlock();
        }
    }

    public double getDynamicIncrement() {
        // Quy tắc bước nhảy 10% (Universal Auction Language)
        // Bước giá tối thiểu = 10% giá hiện tại
        double increment = this.currentPrice * 0.1;
        
        // Làm tròn bước giá cho đẹp (Ví dụ: 10.5 -> 10, 155 -> 150 hoặc 160)
        // Ở đây ta có thể làm tròn xuống hàng đơn vị hoặc hàng chục tùy quy mô
        if (increment < 1) return 1.0;
        if (increment < 10) return Math.floor(increment);
        if (increment < 100) return Math.floor(increment / 5) * 5;
        return Math.floor(increment / 10) * 10;
    }

    public void placeBid(Bidder bidder, double bidAmount) throws InvalidBidException, AuctionClosedException {
        if (lock == null)
            lock = new ReentrantLock();
        lock.lock();
        try {
            if (this.status != AuctionStatus.RUNNING) {
                throw new AuctionClosedException("Phiên đấu giá không trong trạng thái đang diễn ra!");
            }

            double minRequiredBid = this.currentPrice + getDynamicIncrement();
            if (bidAmount < minRequiredBid) {
                throw new InvalidBidException(String.format("Giá đặt phải lớn hơn hoặc bằng $%.2f (Bước nhảy tối thiểu: $%.2f)", 
                        minRequiredBid, getDynamicIncrement()));
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
            notifyObservers(previousBidder);

            // Anti-sniping: Nếu còn dưới 30s thì tự động cộng thêm 30s
            LocalDateTime now = LocalDateTime.now();
            if (this.endTime.minusSeconds(30).isBefore(now) && this.endTime.isAfter(now)) {
                this.endTime = this.endTime.plusSeconds(30);
                if (observers != null) {
                    for (AuctionObserver obs : observers) {
                        obs.onTimeExtended(this, 30);
                    }
                }
            }

        } finally {
            lock.unlock();
        }
        
        // Kích hoạt auto-bidding cho người khác (chỉ khi không phải đang trong vòng lặp auto-bid)
        if (!isAutoBidding) {
            triggerAutoBidding();
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

    public void notifyObservers(Bidder previousBidder) {
        if (observers != null) {
            for (AuctionObserver obs : observers) {
                String topBidderName = (highestBidder != null) ? highestBidder.getUsername() : "Chưa có";
                obs.update(this, this.currentPrice, topBidderName, previousBidder);
            }
        }
    }

    @Override
    public void notifyObservers() {
        notifyObservers(null);
    }

    public LocalDateTime getLastActivityTime() {
        if (lastActivityTime == null) lastActivityTime = LocalDateTime.now();
        return lastActivityTime;
    }

    public void updateActivityTime() {
        this.lastActivityTime = LocalDateTime.now();
    }
}