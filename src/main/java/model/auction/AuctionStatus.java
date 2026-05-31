package model.auction;

public enum AuctionStatus {
    OPEN("⏳ Chưa bắt đầu"),
    RUNNING("🔥 Đang diễn ra"),
    FINISHED("✅ Đã kết thúc"),
    PAID("💰 Đã thanh toán"),
    CANCELED("❌ Đã hủy");

    private final String displayName;

    AuctionStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
