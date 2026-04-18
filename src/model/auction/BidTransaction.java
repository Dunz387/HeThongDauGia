package model.auction;
import model.base.Entity;
import model.user.Bidder;
import java.time.LocalDateTime;

public class BidTransaction extends Entity {

    private Auction auction;
    private Bidder bidder;
    private double bidAmount;
    private LocalDateTime timestamp;

    public BidTransaction(String id, Auction auction, Bidder bidder, double bidAmount, LocalDateTime timestamp) {
        super(id);
        this.auction = auction;
        this.bidder = bidder;
        this.bidAmount = bidAmount;
        this.timestamp = timestamp;
    }

    // Chỉ có Setter

    public Auction getAuction() {
        return auction;
    }

    public Bidder getBidder() {
        return bidder;
    }

    public double getBidAmount() {
        return bidAmount;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    @Override
    public String toString() {
        return String.format("[%s] Người chơi %s đã đặt $%.2f",
                timestamp.toString(), bidder.getUsername(), bidAmount);
    }
}