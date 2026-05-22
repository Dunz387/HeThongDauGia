package model.auction;

import exception.AuctionClosedException;
import exception.InvalidBidException;
import model.base.Entity;
import model.item.Item;
import model.user.Bidder;
import model.user.User;

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

    // Lưu người bán gốc (không thay đổi khi chuyển quyền sở hữu item)
    private User seller;

    private Bidder highestBidder;
    private LocalDateTime endTime;
    private AuctionStatus status;
    private String reason;

    private List<BidTransaction> bidHistory;
    
    // Lưu thời gian đặt giá gần nhất
    private transient LocalDateTime lastActivityTime = LocalDateTime.now();

    // Dùng transient để bỏ qua khi tuần tự hóa (Serialize)
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
        this.seller = item.getOwner(); // Lưu người bán gốc từ owner của item

        this.highestBidder = null;
        this.status = AuctionStatus.OPEN;
        this.bidHistory = new ArrayList<>();
        this.observers = new ArrayList<>();
        this.autoBids = new PriorityQueue<>();
    }

    public Item getItem() {
        return item;
    }

    /** Lấy người bán gốc (không bị thay đổi khi chuyển quyền sở hữu item) */
    public User getSeller() {
        return seller != null ? seller : item.getOwner();
    }

    /** Thiết lập người bán gốc (chỉ dùng khi tải từ DB) */
    public void setSeller(User seller) {
        this.seller = seller;
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
     * Thiết lập giá hiện tại (chỉ dùng khi tải từ DB).
     * Không dùng trong logic đấu giá (sử dụng placeBid).
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
     * Thiết lập người trả giá cao nhất (chỉ dùng khi tải từ DB).
     * Không dùng trong logic đấu giá (sử dụng placeBid).
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
            // Xóa cấu hình tự động đặt giá cũ của người dùng này
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
                // Bước 1: Dọn dẹp trước khi chạy - Xóa các cấu hình đã bị vượt giá từ trước
                List<AutoBidConfig> expiredConfigs = new ArrayList<>();
                for (AutoBidConfig config : autoBids) {
                    if (config.maxBid <= this.currentPrice) {
                        expiredConfigs.add(config);
                    }
                }
                for (AutoBidConfig expired : expiredConfigs) {
                    autoBids.remove(expired);
                    notifyAutoBidExpired(expired.bidder);
                }

                // Sử dụng giải thuật Proxy Bidding chuyên nghiệp
                List<AutoBidConfig> activeList = new ArrayList<>(autoBids);
                if (activeList.isEmpty()) return;

                // Sắp xếp các cấu hình:
                // 1. maxBid giảm dần (ai đặt giá cao hơn sẽ ưu tiên thắng)
                // 2. registerTime tăng dần (nếu cùng maxBid, ai đăng ký trước sẽ thắng)
                activeList.sort((c1, c2) -> {
                    if (c1.maxBid != c2.maxBid) {
                        return Double.compare(c2.maxBid, c1.maxBid);
                    }
                    return Long.compare(c1.registerTime, c2.registerTime);
                });

                AutoBidConfig winner = activeList.get(0);
                if (activeList.size() == 1) {
                    // Chỉ có 1 người tự động đặt giá hợp lệ
                    if (this.highestBidder == null || !this.highestBidder.getId().equals(winner.bidder.getId())) {
                        double nextBid = this.currentPrice + Math.max(winner.increment, getDynamicIncrement());
                        if (nextBid <= winner.maxBid) {
                            try {
                                placeBid(winner.bidder, nextBid);
                            } catch (Exception e) {
                                autoBids.remove(winner);
                                notifyAutoBidExpired(winner.bidder);
                            }
                        }
                    }
                } else {
                    // Có từ 2 người cạnh tranh tự động đặt giá trở lên
                    AutoBidConfig challenger = activeList.get(1);
                    double targetBid;
                    if (winner.maxBid == challenger.maxBid) {
                        // Trường hợp bằng giá max: Người đăng ký trước thắng ở đúng mức giá max
                        targetBid = winner.maxBid;
                    } else {
                        // Trường hợp lệch giá max: Người có max cao hơn thắng ở mức giá = max của người kia + bước nhảy
                        double minIncrement = getDynamicIncrement();
                        targetBid = challenger.maxBid + Math.max(winner.increment, minIncrement);
                        // Đảm bảo không vượt quá maxBid của người thắng
                        if (targetBid > winner.maxBid) {
                            targetBid = winner.maxBid;
                        }
                        // Đảm bảo tuân thủ bước giá tối thiểu so với giá hiện tại
                        double minRequired = this.currentPrice + minIncrement;
                        if (targetBid < minRequired) {
                            targetBid = minRequired;
                        }
                    }

                    // Chỉ đặt giá nếu người thắng chưa phải là người giữ giá cao nhất OR giá hiện tại chưa đạt targetBid
                    if (this.highestBidder == null || !this.highestBidder.getId().equals(winner.bidder.getId()) || this.currentPrice < targetBid) {
                        try {
                            placeBid(winner.bidder, targetBid);
                        } catch (Exception e) {
                            autoBids.remove(winner);
                            notifyAutoBidExpired(winner.bidder);
                        }
                    }
                }

                // Bước 2: Dọn dẹp sau khi chạy - Quét và xóa các cấu hình đã hết hạn/chạm trần sau lượt đặt giá mới
                List<AutoBidConfig> expiredConfigsAfter = new ArrayList<>();
                for (AutoBidConfig config : autoBids) {
                    if (config.maxBid <= this.currentPrice) {
                        expiredConfigsAfter.add(config);
                    }
                }
                for (AutoBidConfig expired : expiredConfigsAfter) {
                    autoBids.remove(expired);
                    notifyAutoBidExpired(expired.bidder);
                }
            } finally {
                isAutoBidding = false;
            }
        } finally {
            lock.unlock();
        }
    }

    public double getDynamicIncrement() {
        // Bước giá tối thiểu = 10% giá hiện tại
        double increment = this.currentPrice * 0.1;
        
        // Làm tròn bước giá cho dễ nhìn
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

            // Thông báo cho máy chủ có lượt đặt giá mới
            notifyObservers(previousBidder);

            // Chống bắn tỉa: Thêm 30s nếu thời gian còn dưới 30s
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
        
        // Kích hoạt tự động đặt giá cho người khác
        if (!isAutoBidding) {
            triggerAutoBidding();
        }
    }

    // --- CÁC HÀM OBSERVER ---
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

    public void notifyAutoBidExpired(Bidder bidder) {
        if (observers != null) {
            for (AuctionObserver obs : observers) {
                obs.onAutoBidExpired(this, bidder);
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