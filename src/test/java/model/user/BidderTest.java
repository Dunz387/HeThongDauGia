package model.user;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class BidderTest {
    private Bidder bidder;

    @BeforeEach
    public void setUp() {
        bidder = new Bidder("B1", "bidder1", "pass1", 1000.0);
    }

    @Test
    public void testAddBalance_Success() {
        // Tình huống: Nạp tiền thành công
        bidder.addBalance(500.0);
        assertEquals(1500.0, bidder.getBalance());
        assertEquals(1500.0, bidder.getAvailableBalance());
    }

    @Test
    public void testAddBalance_ZeroOrNegative() {
        // Tình huống: Nạp số tiền bằng 0 hoặc âm
        bidder.addBalance(0.0);
        assertEquals(1000.0, bidder.getBalance(), "Balance should not change when adding 0");

        bidder.addBalance(-200.0);
        assertEquals(1000.0, bidder.getBalance(), "Balance should not change when adding negative amount");
    }

    @Test
    public void testLockBalance_Success() {
        // Đặt cọc / Giam tiền khi đấu giá
        assertTrue(bidder.lockBalance(300.0));
        assertEquals(1000.0, bidder.getBalance(), "Total balance should not change");
        assertEquals(700.0, bidder.getAvailableBalance(), "Available balance should decrease");
        assertEquals(300.0, bidder.getLockedBalance(), "Locked balance should increase");
    }

    @Test
    public void testLockBalance_Insufficient() {
        // Giam số tiền lớn hơn khả dụng
        assertFalse(bidder.lockBalance(1200.0));
        assertEquals(1000.0, bidder.getBalance());
        assertEquals(1000.0, bidder.getAvailableBalance());
        assertEquals(0.0, bidder.getLockedBalance());
    }

    @Test
    public void testDeductBalance_Success() {
        // Thanh toán sau khi đấu giá thành công (cần có đủ lockedBalance)
        bidder.lockBalance(300.0);
        assertTrue(bidder.deductBalance(300.0));
        assertEquals(700.0, bidder.getBalance());
        assertEquals(700.0, bidder.getAvailableBalance());
        assertEquals(0.0, bidder.getLockedBalance());
    }

    @Test
    public void testDeductBalance_InsufficientLocked() {
        // Thanh toán nhưng không đủ tiền đang giam
        bidder.lockBalance(100.0);
        assertFalse(bidder.deductBalance(200.0));
        assertEquals(1000.0, bidder.getBalance());
        assertEquals(900.0, bidder.getAvailableBalance());
        assertEquals(100.0, bidder.getLockedBalance());
    }
}
