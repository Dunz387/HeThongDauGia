package model;

import model.UserType.User;

import java.time.LocalDateTime;
import java.util.UUID;

public class BidTransaction {

    private String transactionId;
    private User bidder;
    private String auctionId;
    private double amount;
    private LocalDateTime timestamp;

    /**
     * Constructor phục vụ cho việc tạo giao dịch mới
     */
    public BidTransaction(User bidder, String auctionId, double amount) {
        this.transactionId = UUID.randomUUID().toString(); // Tự động sinh mã giao dịch
        this.bidder = bidder;
        this.auctionId = auctionId;
        this.amount = amount;
        this.timestamp = LocalDateTime.now(); // Ghi nhận thời gian thực tế ngay lúc khởi tạo
    }

    // --- Getters ---

    public String getTransactionId() {
        return transactionId;
    }

    public User getBidder() {
        return bidder;
    }

    public String getAuctionId() {
        return auctionId;
    }

    public double getAmount() {
        return amount;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    // --- Setters (Thường hạn chế Setter cho Transaction để đảm bảo tính bất biến) ---

    @Override
    public String toString() {
        return "BidTransaction{" +
                "ID='" + transactionId + '\'' +
                ", Bidder=" + bidder.getUsername() + // Giả sử User có hàm getUsername()
                ", Amount=" + amount +
                ", Time=" + timestamp +
                '}';
    }
}