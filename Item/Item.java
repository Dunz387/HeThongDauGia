import java.time.LocalDateTime;

public abstract class Item {
    protected String id;
    protected String name;
    protected String description;
    protected double startingPrice;
    protected double currentPrice;
    protected LocalDateTime startTime;
    protected LocalDateTime endTime;

    // Constructor
    public Item(String id, String name, String description,
                double startingPrice, LocalDateTime startTime,
                LocalDateTime endTime) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.startingPrice = startingPrice;
        this.currentPrice = startingPrice;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    // Getter
    public String getId() { return id; }
    public String getName() { return name; }
    public double getCurrentPrice() { return currentPrice; }
    public LocalDateTime getStartTime() { return startTime; }
    public LocalDateTime getEndTime() { return endTime; }

    // Setter
    public void setCurrentPrice(double currentPrice) {
        this.currentPrice = currentPrice;
    }

    // Kiểm tra phiên còn hoạt động
    public boolean isActive() {
        LocalDateTime now = LocalDateTime.now();
        return now.isAfter(startTime) && now.isBefore(endTime);
    }

    // Đặt giá
    public boolean placeBid(double bidAmount) {
        if (!isActive()) {
            System.out.println("Auction is not active!");
            return false;
        }

        if (bidAmount <= currentPrice) {
            System.out.println("Bid must be higher than current price!");
            return false;
        }

        currentPrice = bidAmount;
        return true;
    }

    // Abstract method (bắt buộc class con override)
    public abstract void printInfo();
}