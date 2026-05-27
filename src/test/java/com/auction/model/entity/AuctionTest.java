package com.auction.model.entity;

import com.auction.exception.InvalidBidException;
import com.auction.model.enums.AuctionStatus;
import com.auction.model.enums.ItemType;
import com.auction.pattern.factory.ItemFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.*;

class AuctionTest {

    private Seller seller;
    private Item laptop;
    private Auction auction;
    private Bidder bidderA;
    private Bidder bidderB;

    @BeforeEach
    void setUp() {
        seller = new Seller("seller1", "pass123", "seller@uet.edu.vn");

        // Dùng Factory để tạo Item
        laptop = ItemFactory.getInstance().createItem(
                ItemType.ELECTRONICS, "MacBook M3", "New 99%", 1000.0,
                Map.of("brand", "Apple", "warrantyMonths", 12)
        );

        // Tạo phiên đấu giá kéo dài 1 ngày
        auction = new Auction(laptop, seller, LocalDateTime.now(), LocalDateTime.now().plusDays(1));

        bidderA = new Bidder("tung_bidder", "123456", "tung@test.com", 50000.0);
        bidderB = new Bidder("khach_hang", "123456", "kh@test.com", 50000.0);
    }

    @Test
    void testAuctionLifecycle_StartAndEnd() {
        assertEquals(AuctionStatus.OPEN, auction.getStatus(), "Khởi tạo phải là OPEN");

        auction.startAuction();
        assertEquals(AuctionStatus.RUNNING, auction.getStatus(), "Bắt đầu phải chuyển sang RUNNING");

        auction.endAuction();
        assertEquals(AuctionStatus.CANCELED, auction.getStatus(), "Kết thúc khi không có bid phải là CANCELED");
    }

    @Test
    void testPlaceBid_Success() {
        auction.startAuction();

        // Đặt giá hợp lệ
        boolean result = bidderA.placeBid(auction, 1500.0);

        assertTrue(result);
        assertEquals(1500.0, auction.getCurrentHighestPrice());
        assertEquals(bidderA, auction.getCurrentLeader());
        assertEquals(1, auction.getBidHistory().size());
    }

    @Test
    void testPlaceBid_InvalidBidException() {
        auction.startAuction();
        bidderA.placeBid(auction, 1500.0);

        // Bidder B đặt giá thấp hơn giá hiện tại (1500) -> Phải văng lỗi InvalidBidException
        Exception exception = assertThrows(InvalidBidException.class, () -> {
            bidderB.placeBid(auction, 1200.0);
        });
        assertTrue(exception.getMessage().contains("phải cao hơn giá hiện tại"));
    }

    @Test
    void testAutoBid_TriggeredCorrectly() {
        auction.startAuction();

        // Bidder A cài Auto-Bid: Max 5000, bước giá 500
        bidderA.setupAutoBid(auction, 5000.0, 500.0);

        // Bidder B vào đặt tay 1500
        bidderB.placeBid(auction, 1500.0);

        // Hệ thống phải tự động kích hoạt Auto-bid của A đè lên B (1500 + 500 = 2000)
        assertEquals(2000.0, auction.getCurrentHighestPrice(), "Auto-bid phải kích hoạt và nâng giá lên 2000");
        assertEquals(bidderA, auction.getCurrentLeader(), "Bidder A phải giành lại top 1");
    }

    /**
     * BÀI TEST QUAN TRỌNG NHẤT: CHỨNG MINH ĐA LUỒNG (MULTI-THREADING)
     * Dùng CountDownLatch để giả lập 100 người cùng bấm nút "Đặt giá" trong đúng 1 mili-giây.
     */
    @Test
    void testConcurrentBidding_WithReentrantLock() throws InterruptedException {
        auction.startAuction();
        int numberOfThreads = 100;
        ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads);
        CountDownLatch latch = new CountDownLatch(1); // Cổng xuất phát chung
        CountDownLatch doneLatch = new CountDownLatch(numberOfThreads); // Đợi tất cả chạy xong

        for (int i = 0; i < numberOfThreads; i++) {
            final int increment = i;
            executor.submit(() -> {
                try {
                    latch.await(); // Tất cả luồng phải đứng đợi ở đây
                    // Mỗi luồng cố gắng đặt giá cao hơn giá gốc (1000 + 100, 200,...)
                    // Chỉ những luồng chạy sau (do khóa lock) và có giá cao hơn mới lọt qua được
                    BidTransaction tx = new BidTransaction(bidderA, auction, 1000.0 + (increment * 10));
                    auction.placeBid(tx);
                } catch (Exception ignored) {
                    // Sẽ có rất nhiều luồng bị bắn InvalidBidException vì giá bị cũ so với luồng lọt vào trước
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        latch.countDown(); // MỞ CỔNG: Phóng cả 100 luồng chạy cùng một thời điểm
        doneLatch.await(); // Chờ cả 100 luồng chạy xong
        executor.shutdown();

        // Kiểm tra tính toàn vẹn dữ liệu
        // Nếu không có ReentrantLock, dữ liệu trong ArrayList (bidHistory) sẽ bị mất hoặc văng lỗi ConcurrentModificationException
        assertTrue(auction.getBidHistory().size() > 0, "Phải có ít nhất 1 bid thành công");
        assertNotNull(auction.getCurrentLeader());
    }
}