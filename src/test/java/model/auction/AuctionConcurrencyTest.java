package model.auction;

import exception.AuctionClosedException;
import exception.InvalidBidException;
import model.item.Electronics;
import model.item.Item;
import model.user.Bidder;
import model.user.Seller;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

public class AuctionConcurrencyTest {

    private Auction auction;
    private Item testItem;
    private Seller seller;

    @BeforeEach
    public void setUp() {
        seller = new Seller("s1", "seller", "pass", 0);
        testItem = new Electronics("item1", "Test Phone", "A smartphone", seller);
        // Bắt đầu một phiên đấu giá ngay bây giờ, kéo dài 1 giờ, giá khởi điểm 100, bước giá 10
        auction = new Auction("a1", testItem, 100.0, 10.0, LocalDateTime.now().plusHours(1));
        auction.setStatus(AuctionStatus.RUNNING); // Phải trong trạng thái RUNNING để đặt giá
    }

    @Test
    public void testConcurrentBiddingSameAmount() throws InterruptedException {
        int numberOfBidders = 100;
        ExecutorService executorService = Executors.newFixedThreadPool(numberOfBidders);
        CountDownLatch latch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(numberOfBidders);

        List<Bidder> bidders = new ArrayList<>();
        for (int i = 0; i < numberOfBidders; i++) {
            Bidder bidder = new Bidder("b" + i, "bidder" + i, "pass", 10000.0); // 10k số dư
            bidders.add(bidder);
        }

        AtomicInteger successfulBids = new AtomicInteger(0);
        AtomicInteger failedBids = new AtomicInteger(0);

        // Tất cả người dùng sẽ cố gắng đặt giá cùng một mức: 150.0
        // Nhờ có ReentrantLock trong Auction.placeBid(), CHỈ CÓ MỘT người được thành công.
        double targetBidAmount = 150.0;

        for (Bidder bidder : bidders) {
            executorService.submit(() -> {
                try {
                    latch.await(); // Đợi tất cả thread sẵn sàng
                    auction.placeBid(bidder, targetBidAmount);
                    successfulBids.incrementAndGet();
                } catch (InvalidBidException | AuctionClosedException e) {
                    failedBids.incrementAndGet();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        latch.countDown(); // Phát lệnh chạy đồng loạt
        doneLatch.await(10, TimeUnit.SECONDS); // Chờ tất cả thực thi xong

        executorService.shutdown();

        // Kiểm tra kết quả
        assertEquals(1, successfulBids.get(), "Chỉ có một lượt đặt giá được phép thành công khi đặt cùng 1 số tiền cùng lúc.");
        assertEquals(numberOfBidders - 1, failedBids.get(), "Các lượt khác phải bị từ chối với InvalidBidException.");
        
        assertNotNull(auction.getHighestBidder(), "Phải có người trả giá cao nhất.");
        assertEquals(targetBidAmount, auction.getCurrentPrice(), "Giá hiện tại phải bằng giá thắng cuộc.");
        assertEquals(1, auction.getBidHistory().size(), "Lịch sử giao dịch chỉ được chứa đúng một giao dịch.");
    }
    
    @Test
    public void testSequentialBiddingIncrements() throws InterruptedException {
        // Mô phỏng nhiều thread cố gắng đặt giá ngày càng cao đồng thời
        int numberOfBidders = 5;
        ExecutorService executorService = Executors.newFixedThreadPool(numberOfBidders);
        CountDownLatch latch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(numberOfBidders);

        List<Bidder> bidders = new ArrayList<>();
        for (int i = 0; i < numberOfBidders; i++) {
            Bidder bidder = new Bidder("b" + i, "bidder" + i, "pass", 100000.0); // Cho số dư lớn
            bidders.add(bidder);
        }
        
        // Mỗi thread cố gắng đặt thành công 5 lần bằng cách kiểm tra giá hiện tại
        for (int i = 0; i < numberOfBidders; i++) {
            Bidder bidder = bidders.get(i);
            executorService.submit(() -> {
                try {
                    latch.await();
                    for(int j = 0; j < 5; j++) {
                        boolean success = false;
                        while(!success) {
                            try {
                                double nextBid = auction.getCurrentPrice() + auction.getDynamicIncrement();
                                auction.placeBid(bidder, nextBid);
                                success = true; // Đặt thành công mới thoát vòng lặp
                            } catch (InvalidBidException e) {
                                // Nếu bị thread khác vượt mặt (lock tranh chấp), thử lại
                                Thread.sleep(10); // Tránh busy-waiting quá nặng
                            } catch (AuctionClosedException e) {
                                break;
                            }
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    doneLatch.countDown();
                }
            });
        }
        
        latch.countDown();
        doneLatch.await(30, TimeUnit.SECONDS);
        executorService.shutdown();
        
        // Tổng số lượt đặt giá thành công phải là numberOfBidders * 5
        assertEquals(numberOfBidders * 5, auction.getBidHistory().size(), "Tất cả các thread phải đặt thành công 5 lần mỗi thread");
        
        // Xác minh lịch sử đấu giá luôn tăng ngặt
        List<BidTransaction> history = auction.getBidHistory();
        double previousAmt = -1;
        for (BidTransaction tx : history) {
            assertTrue(tx.getBidAmount() > previousAmt, "Lịch sử đặt giá phải luôn tăng dần");
            previousAmt = tx.getBidAmount();
        }
        
        // Xác minh giá hiện tại bằng mức giá của giao dịch cuối cùng
        assertEquals(previousAmt, auction.getCurrentPrice());
    }
}
