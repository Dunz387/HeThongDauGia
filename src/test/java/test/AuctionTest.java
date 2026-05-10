package test;

import exception.AuctionClosedException;
import exception.InvalidBidException;
import model.auction.Auction;
import model.auction.AuctionStatus;
import model.item.Electronics;
import model.item.Item;
import model.user.Bidder;
import model.user.Seller;
import service.AuctionManager;
import org.junit.jupiter.api.*;

import java.time.LocalDateTime;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class AuctionTest {

    private AuctionManager manager;
    private Auction auction;
    private Bidder bidderAn;
    private Bidder bidderBinh;
    private Seller seller;

    @BeforeEach
    void setUp() {
        // Lấy instance từ AuctionManager (Singleton)
        manager = AuctionManager.getInstance();

        // Khởi tạo dữ liệu giả lập cho mỗi bài test
        seller = new Seller("S01", "HeThong_Admin", "123", 0.0);
        bidderAn = new Bidder("B01", "An_Quang", "123", 1000.0);
        bidderBinh = new Bidder("B02", "Binh_Dang", "123", 2000.0);

        Item laptop = new Electronics("I01", "Macbook Pro", "Mô tả sản phẩm", seller, "Apple", 12);

        // Tạo đấu giá: Giá khởi điểm $500, bước giá tối thiểu $50
        auction = new Auction("AUC-99", laptop, 500.0, 50.0, LocalDateTime.now().plusMinutes(5));
        auction.setStatus(AuctionStatus.RUNNING);
    }

    @Test
    @Order(1)
    @DisplayName("1. Kiểm tra đặt giá hợp lệ: Giá tăng và đổi người dẫn đầu")
    void testValidBidding() throws Exception {
        // An đặt $600 (Hợp lệ: > 500 + 50)
        auction.placeBid(bidderAn, 600.0);

        assertEquals(600.0, auction.getCurrentPrice(), "Giá hiện tại phải cập nhật lên 600");
        assertEquals(bidderAn, auction.getHighestBidder(), "An phải là người dẫn đầu");
    }

    @Test
    @Order(2)
    @DisplayName("2. Kiểm tra chặn đặt giá: Sai bước giá tối thiểu")
    void testInvalidBidStep() {
        // Giá hiện tại là 500, bước giá 50 -> Ít nhất phải đặt 550
        assertThrows(InvalidBidException.class, () -> {
            auction.placeBid(bidderAn, 520.0);
        }, "Phải chặn đặt giá $520 vì chưa đủ bước giá tối thiểu");
    }

    @Test
    @Order(3)
    @DisplayName("3. Kiểm tra ví tiền: Chặn khi số dư không đủ")
    void testInsufficientBalance() {
        // An chỉ có $1000, thử đặt $1500 qua Manager
        String result = manager.processBid(bidderAn, auction, 1500.0);
        assertTrue(result.contains("không đủ tiền"), "Manager phải báo lỗi thiếu tiền trong ví");
    }

    @Test
    @Order(4)
    @DisplayName("4. Kiểm tra thanh toán: Trừ tiền người thắng, cộng tiền người bán")
    void testPaymentAndConclusion() throws Exception {
        auction.placeBid(bidderBinh, 800.0);

        // Gọi thẳng hàm, không gán vào biến boolean
        manager.concludeAuction(auction);

        // Xóa dòng assertTrue(success...) đi và giữ nguyên các dòng dưới:
        assertEquals(AuctionStatus.PAID, auction.getStatus(), "Trạng thái phải chuyển sang PAID");
        assertEquals(1200.0, bidderBinh.getBalance(), "Bình phải còn 1200 (2000 - 800)");
        assertEquals(800.0, seller.getBalance(), "Người bán phải nhận được 800");
    }
    @Test
    @Order(5)
    @DisplayName("5. Kiểm tra đa luồng: Tranh chấp giá (Stress Test)")
    void testConcurrency() throws InterruptedException {
        int threads = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch latch = new CountDownLatch(1);

        for (int i = 0; i < threads; i++) {
            final double bid = 600.0 + (i * 100); // Các mức giá khác nhau từ 600 đến 1500
            executor.submit(() -> {
                try {
                    latch.await(); // Đợi lệnh xuất phát cùng lúc
                    auction.placeBid(new Bidder("ID", "User", "123", 5000.0), bid);
                } catch (Exception ignored) {}
            });
        }

        latch.countDown(); // BẮT ĐẦU TRANH CHẤP
        executor.shutdown();
        Thread.sleep(2000); // Đợi xử lý

        // Sau khi tranh chấp, giá hiện tại phải là mức giá hợp lệ cao nhất được đặt thành công
        assertTrue(auction.getCurrentPrice() >= 600.0);
        assertNotNull(auction.getHighestBidder());
        System.out.println("Giá chốt sau tranh chấp đa luồng: $" + auction.getCurrentPrice());
    }
}