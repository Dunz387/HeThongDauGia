package model.auction;

public enum AuctionStatus {
    OPEN,       // Vừa tạo, chưa bắt đầu
    RUNNING,    // Đang diễn ra
    FINISHED,   // Đã hết giờ (đang chờ thanh toán)
    PAID,       // Người thắng đã thanh toán xong
    CANCELED    // Hủy (do không ai mua hoặc lỗi)
}
