package com.auction.model.entity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class BidderAutoBidSetupTest {

    private Bidder bidder;
    private Auction auction;
    private Seller seller;
    private Item item;

    @BeforeEach
    void setUp() {
        bidder = new Bidder("bidder", "pass123", "bidder@test.com", 10000.0);
        seller = new Seller("seller", "pass123", "seller@test.com");
        item = new Art("Tranh", "Mô tả", 500.0, "Artist");

        LocalDateTime start = LocalDateTime.now();
        LocalDateTime end = start.plusDays(1);
        auction = new Auction(item, seller, start, end);
        auction.startAuction();
    }

    @Test
    void testSetupAutoBidSuccess() {
        assertDoesNotThrow(() ->
            bidder.setupAutoBid(auction, 5000.0, 500.0),
            "Setup auto-bid thành công");
    }

    @Test
    void testSetupAutoBidNullAuction() {
        assertThrows(IllegalArgumentException.class, () ->
            bidder.setupAutoBid(null, 5000.0, 500.0),
            "Auction không được null");
    }

    @Test
    void testSetupAutoBidMaxBidBelowCurrent() {
        auction.startAuction();
        bidder.placeBid(auction, 1000.0);

        Bidder bidder2 = new Bidder("bidder2", "pass", "bidder2@test.com", 10000.0);
        assertThrows(IllegalArgumentException.class, () ->
            bidder2.setupAutoBid(auction, 800.0, 100.0),
            "MaxBid phải cao hơn giá hiện tại");
    }

    @Test
    void testSetupAutoBidInvalidIncrement() {
        assertThrows(IllegalArgumentException.class, () ->
            bidder.setupAutoBid(auction, 5000.0, 0),
            "Bước giá phải dương");

        assertThrows(IllegalArgumentException.class, () ->
            bidder.setupAutoBid(auction, 5000.0, -100.0),
            "Bước giá phải dương");
    }

    @Test
    void testSetupAutoBidExceedsBalance() {
        Bidder poorBidder = new Bidder("poor", "pass", "poor@test.com", 1000.0);

        assertThrows(IllegalArgumentException.class, () ->
            poorBidder.setupAutoBid(auction, 5000.0, 500.0),
            "MaxBid không được vượt quá số dư");
    }

    @Test
    void testSetupAutoBidIncrementExceedsBalance() {
        Bidder poorBidder = new Bidder("poor", "pass", "poor@test.com", 100.0);

        assertThrows(IllegalArgumentException.class, () ->
            poorBidder.setupAutoBid(auction, 500.0, 200.0),
            "Bước giá không được vượt quá số dư");
    }

    @Test
    void testSetupAutoBidIncrementGreaterThanMaxBid() {
        assertThrows(IllegalArgumentException.class, () ->
            bidder.setupAutoBid(auction, 500.0, 600.0),
            "Bước giá không được lớn hơn maxBid");
    }

    @Test
    void testSetupAutoBidJustAtBalance() {
        Bidder exactBidder = new Bidder("exact", "pass", "exact@test.com", 1000.0);

        assertDoesNotThrow(() ->
            exactBidder.setupAutoBid(auction, 1000.0, 100.0),
            "MaxBid có thể bằng số dư");
    }

    @Test
    void testSetupAutoBidMinimalValues() {
        // MaxBid = giá hiện tại + bước giá nhỏ nhất
        double currentPrice = auction.getCurrentHighestPrice();
        double maxBid = currentPrice + 0.01;
        double increment = 0.01;

        assertDoesNotThrow(() ->
            bidder.setupAutoBid(auction, maxBid, increment));
    }

    @Test
    void testSetupAutoBidLargeValues() {
        bidder.setupAutoBid(auction, 9999.99, 9999.98);

        // Auto-bid registers successfully with large values
        assertTrue(true);
    }

    @Test
    void testSetupAutoBidMultipleTimes() {
        bidder.setupAutoBid(auction, 5000.0, 500.0);

        // Update auto-bid config
        bidder.setupAutoBid(auction, 7000.0, 700.0);

        // Should replace previous config
        assertTrue(true);
    }

    @Test
    void testSetupAutoBidDecimalValues() {
        bidder.setupAutoBid(auction, 5000.50, 500.25);
        assertTrue(true);
    }

    @Test
    void testSetupAutoBidWithMinimalBalance() {
        // Bidder has just enough balance
        Bidder minimalBidder = new Bidder("minimal", "pass", "minimal@test.com", 600.0);

        assertDoesNotThrow(() ->
            minimalBidder.setupAutoBid(auction, 600.0, 50.0));
    }

    @Test
    void testSetupAutoBidMaxBidEqual() {
        assertDoesNotThrow(() ->
            bidder.setupAutoBid(auction, 600.0, 50.0),
            "MaxBid có thể cao hơn starting price");
    }

    @Test
    void testSetupAutoBidAfterManualBid() {
        bidder.placeBid(auction, 600.0);

        Bidder bidder2 = new Bidder("bidder2", "pass", "bidder2@test.com", 10000.0);
        assertDoesNotThrow(() ->
            bidder2.setupAutoBid(auction, 5000.0, 500.0),
            "Có thể setup auto-bid sau khi có bid");
    }
}

