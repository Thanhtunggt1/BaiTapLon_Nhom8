package com.auction.model.entity;

import com.auction.exception.AuctionClosedException;
import com.auction.exception.InsufficientBalanceException;
import com.auction.exception.InvalidBidException;
import com.auction.model.enums.AuctionStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class BidderPlaceBidTest {

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
    }

    @Test
    void testPlaceBidSuccess() {
        auction.startAuction();
        double initialBalance = bidder.getBalance();

        boolean result = bidder.placeBid(auction, 600.0);

        assertTrue(result);
        assertEquals(600.0, auction.getCurrentHighestPrice());
        assertEquals(bidder, auction.getCurrentLeader());
        // Balance không giảm ngay, chỉ giảm khi thanh toán
        assertEquals(initialBalance, bidder.getBalance());
    }

    @Test
    void testPlaceBidOnClosedAuction() {
        auction.startAuction();
        auction.endAuction();

        assertThrows(AuctionClosedException.class, () ->
            bidder.placeBid(auction, 600.0),
            "Không thể bid khi phiên đã kết thúc");
    }

    @Test
    void testPlaceBidOnOpenAuction() {
        // Chưa startAuction
        assertThrows(AuctionClosedException.class, () ->
            bidder.placeBid(auction, 600.0),
            "Không thể bid khi phiên ở trạng thái OPEN");
    }

    @Test
    void testPlaceBidBelowCurrentPrice() {
        auction.startAuction();
        bidder.placeBid(auction, 600.0);

        Bidder bidder2 = new Bidder("bidder2", "pass123", "bidder2@test.com", 10000.0);
        assertThrows(InvalidBidException.class, () ->
            bidder2.placeBid(auction, 550.0),
            "Bid phải cao hơn giá hiện tại");
    }

    @Test
    void testPlaceBidEqualCurrentPrice() {
        auction.startAuction();
        bidder.placeBid(auction, 600.0);

        Bidder bidder2 = new Bidder("bidder2", "pass123", "bidder2@test.com", 10000.0);
        assertThrows(InvalidBidException.class, () ->
            bidder2.placeBid(auction, 600.0),
            "Bid phải lớn hơn giá hiện tại");
    }

    @Test
    void testPlaceBidIfInsufficientBalance() {
        auction.startAuction();
        Bidder poorBidder = new Bidder("poor", "pass123", "poor@test.com", 100.0);

        assertThrows(InsufficientBalanceException.class, () ->
            poorBidder.placeBid(auction, 500.0),
            "Không đủ tiền để bid");
    }

    @Test
    void testPlaceBidWithExactBalance() {
        auction.startAuction();
        Bidder exactBidder = new Bidder("exact", "pass123", "exact@test.com", 600.0);

        boolean result = exactBidder.placeBid(auction, 600.0);
        assertTrue(result);
        assertEquals(exactBidder, auction.getCurrentLeader());
    }

    @Test
    void testNullAuctionPlaceBid() {
        assertThrows(IllegalArgumentException.class, () ->
            bidder.placeBid(null, 600.0),
            "Auction không được null");
    }

    @Test
    void testPlaceBidMultipleTimes() {
        auction.startAuction();

        boolean result1 = bidder.placeBid(auction, 600.0);
        assertTrue(result1);
        assertEquals(bidder, auction.getCurrentLeader());

        Bidder bidder2 = new Bidder("bidder2", "pass123", "bidder2@test.com", 10000.0);
        boolean result2 = bidder2.placeBid(auction, 700.0);
        assertTrue(result2);
        assertEquals(bidder2, auction.getCurrentLeader());

        boolean result3 = bidder.placeBid(auction, 800.0);
        assertTrue(result3);
        assertEquals(bidder, auction.getCurrentLeader());

        assertEquals(3, auction.getBidHistory().size());
    }

    @Test
    void testPlaceBidWithNegativeAmount() {
        auction.startAuction();

        assertThrows(Exception.class, () ->
            bidder.placeBid(auction, -100.0),
            "Bid phải là số dương");
    }

    @Test
    void testPlaceBidWithZeroAmount() {
        auction.startAuction();

        assertThrows(Exception.class, () ->
            bidder.placeBid(auction, 0.0),
            "Bid phải lớn hơn 0");
    }

    @Test
    void testPlaceBidWithVeryLargeAmount() {
        auction.startAuction();

        boolean result = bidder.placeBid(auction, 999999.99);
        assertTrue(result);
        assertEquals(999999.99, auction.getCurrentHighestPrice());
    }

    @Test
    void testPlaceBidDecimalAmount() {
        auction.startAuction();

        boolean result = bidder.placeBid(auction, 500.50);
        assertTrue(result);
        assertEquals(500.50, auction.getCurrentHighestPrice());
    }

    @Test
    void testPlaceBidJustAboveStartingPrice() {
        auction.startAuction();

        boolean result = bidder.placeBid(auction, 500.01);
        assertTrue(result);
        assertEquals(500.01, auction.getCurrentHighestPrice());
    }
}

