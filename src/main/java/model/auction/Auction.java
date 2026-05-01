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
    private double bidIncrement;        // Bước giá tối thiểu

    private Bidder highestBidder;
    private LocalDateTime endTime;
    private AuctionStatus status;
    private String reason;

    // Lịch sử giao dịch
    private List<BidTransaction> bidHistory;

    // Thêm danh sách Observer (dùng transient để bỏ qua khi lưu file sau này)
    private transient List<AuctionObserver> observers;

    private transient ReentrantLock lock = new ReentrantLock();

    public Auction(String id, Item item, double startingPrice, double bidIncrement, LocalDateTime endTime) {
        super(id);
        this.item = item;
        this.startingPrice = startingPrice;
        this.currentPrice = startingPrice; // Ban đầu giá cao nhất chính là giá khởi điểm
        this.bidIncrement = bidIncrement;
        this.endTime = endTime;

        this.highestBidder = null;
        this.status = AuctionStatus.OPEN; // Mặc định vừa tạo là OPEN
        this.bidHistory = new ArrayList<>();
        this.observers = new ArrayList<>(); // Khởi tạo danh sách
    }

    // TRIỂN KHAI OBSERVER PATTERN

    @Override
    public void addObserver(AuctionObserver observer) {
        // Đề phòng trường hợp đọc từ file lên observers bị null
        if (observers == null) observers = new ArrayList<>();
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
        if (observers == null) return;
        // Lấy tên người dẫn đầu hiện tại
        String topBidderName = (highestBidder != null) ? highestBidder.getUsername() : "Chưa có";

        for (AuctionObserver observer : observers) {
            observer.update(this, this.currentPrice, topBidderName);
        }
    }

    // GETTER
    public Item getItem() { return item; }
    public double getCurrentPrice() { return currentPrice; }
    public Bidder getHighestBidder() { return highestBidder; }
    public AuctionStatus getStatus() { return status; }
    public LocalDateTime getEndTime() { return endTime; }

    // Dành cho Admin/Manager đổi trạng thái
    public void setStatus(AuctionStatus status) { this.status = status; }

    public String getReason() { return reason; }
    public void setReason(String reason) {
        this.reason = reason;
    }

    // Logic ĐÃ ĐƯỢC NÂNG CẤP EXCEPTION (Mục 2)
    public void placeBid(Bidder bidder, double bidAmount) throws AuctionClosedException, InvalidBidException {
        lock.lock();
        try {
            if (this.status != AuctionStatus.RUNNING) {
                throw new AuctionClosedException("Phiên đấu giá chưa mở hoặc đã kết thúc.");
            }

            // Kiểm tra giá đặt có hợp lệ không (Lớn hơn giá hiện tại + bước giá)
            double minRequiredBid = currentPrice + bidIncrement;
            if (bidAmount < minRequiredBid) {
                throw new InvalidBidException(String.format("Giá đặt phải lớn hơn hoặc bằng $%.2f", minRequiredBid));
            }

            // Cập nhật thông tin người thắng mới
            this.currentPrice = bidAmount;
            this.highestBidder = bidder;

            // Tạo biên lai (Transaction) và lưu vào lịch sử
            BidTransaction transaction = new BidTransaction("TX-" + System.currentTimeMillis(), this, bidder, bidAmount, LocalDateTime.now());
            bidHistory.add(transaction);

            // BÁO ĐỘNG CHO TẤT CẢ OBSERVER (UI) NGAY KHI ĐẶT GIÁ THÀNH CÔNG
            notifyObservers();

        } finally {
            lock.unlock();
        }
    }

    private void readObject(java.io.ObjectInputStream in) throws java.io.IOException, ClassNotFoundException {
        in.defaultReadObject();
        this.lock = new ReentrantLock();
        this.observers = new ArrayList<>();
    }
}
