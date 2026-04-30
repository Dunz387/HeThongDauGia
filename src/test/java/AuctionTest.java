import exception.AuctionClosedException;
import exception.InvalidBidException;
import model.auction.Auction;
import model.auction.AuctionStatus;
import model.item.Electronics;
import model.item.Item;
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

    @BeforeEach
    public void setUp() {
        Seller seller = new Seller("S1", "seller_test", "123", 0);
        Item laptop = new Electronics("I1", "Laptop Gaming", "Mới 100%", seller, "Asus", 24);

        auction = new Auction("A1", laptop, 1000.0, 100.0, LocalDateTime.now().plusDays(1));
        auction.setStatus(AuctionStatus.RUNNING);

        bidder1 = new Bidder("B1", "nguoi_choi_1", "123", 5000);
        bidder2 = new Bidder("B2", "nguoi_choi_2", "123", 5000);
    }

    @Test
    public void testDatGiaHopLe() throws Exception {
        auction.placeBid(bidder1, 1200.0);
        assertEquals(1200.0, auction.getCurrentPrice());
        assertEquals(bidder1, auction.getHighestBidder());
    }

    @Test
    public void testDatGiaQuaThap() {
        assertThrows(InvalidBidException.class, () -> {
            auction.placeBid(bidder2, 1050.0);
        });
    }

    @Test
    public void testDatGiaKhiPhienDaKetThuc() {
        auction.setStatus(AuctionStatus.FINISHED);
        assertThrows(AuctionClosedException.class, () -> {
            auction.placeBid(bidder1, 2000.0);
        });
    }
}
