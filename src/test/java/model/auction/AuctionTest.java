package model.auction;

import exception.AuctionClosedException;
import exception.InvalidBidException;
import model.item.Item;
import model.item.ItemFactory;
import model.user.Bidder;
import model.user.Seller;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

public class AuctionTest {

    private Auction auction;
    private Bidder bidder1;
    private Bidder bidder2;
    private Seller seller;

    @BeforeEach
    public void setUp() {
        seller = new Seller("S1", "seller_test", "123", 0.0);
        Item item = ItemFactory.createItem("ELECTRONICS", "I1", "Laptop", "Gaming Laptop", seller, "Asus", 12);
        
        // Tạo phiên đấu giá mở trong 10 phút, giá khởi điểm 1000, bước giá 100
        auction = new Auction("A1", item, 1000.0, 100.0, LocalDateTime.now().plusMinutes(10));
        auction.setStatus(AuctionStatus.RUNNING);

        bidder1 = new Bidder("B1", "bidder1", "123", 5000.0); // 5000 số dư
        bidder2 = new Bidder("B2", "bidder2", "123", 2000.0); // 2000 số dư
    }

    @Test
    public void testPlaceValidBid() throws Exception {
        auction.placeBid(bidder1, 1100.0);

        assertEquals(1100.0, auction.getCurrentPrice(), "Giá hiện tại phải là 1100");
        assertEquals(bidder1, auction.getHighestBidder(), "Người đặt giá cao nhất phải là bidder1");
        
        // Bidder1 bị giam 1100
        assertEquals(5000.0, bidder1.getBalance(), "Balance tổng không đổi (chỉ lock)");
        // Nếu bidder có hàm getLockedBalance, ta có thể test (tuy nhiên ta biết logic lock trừ vào khả dụng)
    }

    @Test
    public void testBidLowerThanMinRequiredThrowsException() {
        // Giá khởi điểm 1000, bước giá 100 => bid tối thiểu 1100
        Exception exception = assertThrows(InvalidBidException.class, () -> {
            auction.placeBid(bidder1, 1050.0);
        });
        
        assertTrue(exception.getMessage().contains("Giá đặt phải lớn hơn hoặc bằng"));
    }

    @Test
    public void testBidderNotEnoughBalanceThrowsException() {
        // Bidder2 chỉ có 2000
        Exception exception = assertThrows(InvalidBidException.class, () -> {
            auction.placeBid(bidder2, 2500.0);
        });

        assertTrue(exception.getMessage().contains("Không đủ số dư khả dụng"));
    }

    @Test
    public void testPlaceBidOnClosedAuctionThrowsException() {
        auction.setStatus(AuctionStatus.FINISHED);

        Exception exception = assertThrows(AuctionClosedException.class, () -> {
            auction.placeBid(bidder1, 1500.0);
        });

        assertTrue(exception.getMessage().contains("không trong trạng thái đang diễn ra"));
    }

    @Test
    public void testUnlockBalanceForPreviousBidder() throws Exception {
        // Bidder1 đặt 1200
        auction.placeBid(bidder1, 1200.0);
        
        // Bidder2 đặt 1500
        auction.placeBid(bidder2, 1500.0);

        assertEquals(1500.0, auction.getCurrentPrice());
        assertEquals(bidder2, auction.getHighestBidder());
        
        // Bidder1 phải được hoàn tiền đã lock (mở khóa)
        // Vì class Bidder có hàm lockBalance và unlockBalance, nếu lock thì số dư thực khả dụng giảm
        // Khi unlock, số dư khả dụng về lại như cũ. Ta có thể test việc bidder1 có thể đặt tiếp 5000.
        assertTrue(bidder1.lockBalance(5000.0), "Bidder1 phải được hoàn tiền lock nên có thể dùng toàn bộ 5000 để lock lại");
    }
}
