package model.auction;

public interface AuctionSubject {
    void addObserver(AuctionObserver observer);
    void removeObserver(AuctionObserver observer);
    void notifyObservers(); // Kêu gọi tất cả mọi người khi có thay đổi
}
