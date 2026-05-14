package model.user;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class BidderTest {

    private Bidder bidder;

    @BeforeEach
    public void setUp() {
        bidder = new Bidder("B1", "testuser", "password123", 1000.0);
    }

    @Test
    public void testLockBalanceSuccess() {
        boolean success = bidder.lockBalance(400.0);
        assertTrue(success, "Phải khóa số dư thành công");
        
        // Lock tiếp 600
        boolean success2 = bidder.lockBalance(600.0);
        assertTrue(success2, "Khóa nốt 600 thành công");
        
        // Lock quá số dư
        boolean success3 = bidder.lockBalance(1.0);
        assertFalse(success3, "Không thể khóa khi hết số dư khả dụng");
    }

    @Test
    public void testUnlockBalance() {
        bidder.lockBalance(500.0);
        bidder.unlockBalance(500.0);
        
        // Lúc này số dư khả dụng phải về lại 1000
        boolean success = bidder.lockBalance(1000.0);
        assertTrue(success, "Đã unlock nên có thể khóa lại toàn bộ số dư");
    }

    @Test
    public void testDeductBalance() {
        bidder.lockBalance(300.0);
        // deductBalance trừ thẳng vào balance tổng (và mở khóa 300)
        boolean success = bidder.deductBalance(300.0);
        
        assertTrue(success, "Trừ tiền thành công");
        assertEquals(700.0, bidder.getBalance(), "Tổng số dư còn lại 700");
    }

    @Test
    public void testAddBalance() {
        bidder.addBalance(500.0);
        assertEquals(1500.0, bidder.getBalance(), "Cộng số dư thành công");
    }
}
