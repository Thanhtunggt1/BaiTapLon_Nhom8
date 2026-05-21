package com.auction.model.entity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class AutoBidConfigTest {

    private Bidder bidder1;
    private Bidder bidder2;
    private Auction dummyAuction;

    @BeforeEach
    void setUp() {
        bidder1 = new Bidder("user1", "123456", "u1@test.com", 10000);
        bidder2 = new Bidder("user2", "123456", "u2@test.com", 10000);
        Seller seller = new Seller("s", "123456", "s@test.com");
        Item item = new Item("Tượng", "Cổ", 500) {};
        dummyAuction = new Auction(item, seller, LocalDateTime.now(), LocalDateTime.now().plusDays(1));
    }

    @Test
    void testComputeNextBid_WithinMaxBid() {
        // Cài Max = 5000, Bước giá = 500
        AutoBidConfig config = new AutoBidConfig(bidder1, dummyAuction, 5000.0, 500.0);

        // Giá hiện tại là 1000 -> Giá tiếp theo dự kiến là 1500 (Vẫn nhỏ hơn 5000)
        double nextBid = config.computeNextBid(1000.0);
        assertEquals(1500.0, nextBid);
    }

    @Test
    void testComputeNextBid_ExceedsMaxBid_ReturnsMinusOne() {
        // Cài Max = 1200, Bước giá = 500
        AutoBidConfig config = new AutoBidConfig(bidder1, dummyAuction, 1200.0, 500.0);

        // Giá hiện tại đã bị đẩy lên 1000 -> Giá tiếp theo cần là 1500 (Vượt quá ngân sách 1200)
        // Hệ thống phải trả về -1 (Giá trị lính canh)
        double nextBid = config.computeNextBid(1000.0);
        assertEquals(-1.0, nextBid, "Khi vượt quá maxBid, phải trả về -1");
    }

    @Test
    void testCompareTo_SortingByRegistrationTime() throws InterruptedException {
        AutoBidConfig configEarly = new AutoBidConfig(bidder1, dummyAuction, 5000.0, 500.0);

        // Dừng luồng 100 mili-giây để đảm bảo thời gian đăng ký của người thứ 2 chắc chắn xảy ra sau
        Thread.sleep(100);

        AutoBidConfig configLate = new AutoBidConfig(bidder2, dummyAuction, 5000.0, 500.0);

        // compareTo trả về số âm nếu configEarly đăng ký trước configLate
        assertTrue(configEarly.compareTo(configLate) < 0, "Người đăng ký sớm hơn phải có thứ tự ưu tiên nhỏ hơn (đứng trước)");
        assertTrue(configLate.compareTo(configEarly) > 0);
    }
}
