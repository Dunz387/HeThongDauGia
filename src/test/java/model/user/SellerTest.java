package model.user;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class SellerTest {
    private Seller seller;

    @BeforeEach
    public void setUp() {
        seller = new Seller("S1", "seller1", "pass1", 500.0);
    }

    @Test
    public void testDeductBalance_Success() {
        // Tình huống: Rút tiền thành công
        assertTrue(seller.deductBalance(200.0));
        assertEquals(300.0, seller.getBalance());
    }

    @Test
    public void testDeductBalance_Insufficient() {
        // Tình huống: Rút quá số dư
        assertFalse(seller.deductBalance(600.0));
        assertEquals(500.0, seller.getBalance(), "Balance should remain unchanged");
    }

    @Test
    public void testDeductBalance_ZeroOrNegative() {
        // Tình huống: Rút số tiền không hợp lệ (<= 0)
        assertFalse(seller.deductBalance(0.0));
        assertEquals(500.0, seller.getBalance(), "Balance should remain unchanged");

        assertFalse(seller.deductBalance(-100.0));
        assertEquals(500.0, seller.getBalance(), "Balance should remain unchanged");
    }

    @Test
    public void testReceivePayment_Success() {
        // Tình huống: Nhận tiền thanh toán từ đấu giá thành công
        seller.receivePayment(300.0);
        assertEquals(800.0, seller.getBalance());
    }
}
