package com.auction.model.entity;

import com.auction.exception.AuctionClosedException;
import com.auction.exception.InvalidBidException;
import com.auction.model.enums.AuctionStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import static org.junit.jupiter.api.Assertions.*;

public class AuctionTest {

    private Seller seller;
    private Electronics item;
    private Auction auction;
    private Bidder bidder1;
    private Bidder bidder2;

    @BeforeEach
    public void setUp() {
        seller = new Seller("seller_pro", "pass123", "seller@test.com");
        item = new Electronics("Phone", "Smart", 2000.0, "Samsung", 24);

        try {
            java.lang.reflect.Field itemsField = Seller.class.getDeclaredField("items");
            itemsField.setAccessible(true);
            ((java.util.List<Item>) itemsField.get(seller)).add(item);
        } catch (Exception ignored) {}

        LocalDateTime now = LocalDateTime.now();
        auction = new Auction(item, seller, now, now.plusMinutes(30));

        bidder1 = new Bidder("bidder1", "pass123", "b1@test.com", 10000.0);
        bidder2 = new Bidder("bidder2", "pass123", "b2@test.com", 10000.0);
    }

    @Test
    public void testStartAuction() {
        assertEquals(AuctionStatus.OPEN, auction.getStatus());
        auction.startAuction();
        assertEquals(AuctionStatus.RUNNING, auction.getStatus());
    }

    @Test
    public void testPlaceBidSuccess() {
        auction.startAuction();
        BidTransaction bid = new BidTransaction(bidder1, auction, 2500.0);

        assertTrue(auction.placeBid(bid));
        assertEquals(2500.0, auction.getCurrentHighestPrice());
        assertEquals(bidder1, auction.getCurrentLeader());
    }

    @Test
    public void testPlaceBidAuctionClosed() {
        BidTransaction bid = new BidTransaction(bidder1, auction, 2500.0);
        assertThrows(AuctionClosedException.class, () -> auction.placeBid(bid));
    }

    @Test
    public void testPlaceBidInvalidAmount() {
        auction.startAuction();
        BidTransaction bid = new BidTransaction(bidder1, auction, 1500.0);

        assertThrows(InvalidBidException.class, () -> auction.placeBid(bid));
    }

    @Test
    public void testEndAuctionWithWinner() {
        auction.startAuction();
        BidTransaction bid = new BidTransaction(bidder1, auction, 3000.0);
        auction.placeBid(bid);

        auction.endAuction();
        assertEquals(AuctionStatus.FINISHED, auction.getStatus());
        assertNotNull(auction.getFinishedTime());
    }

    @Test
    public void testEndAuctionNoWinner() {
        auction.startAuction();
        auction.endAuction();
        assertEquals(AuctionStatus.CANCELED, auction.getStatus());
    }

    @Test
    public void testMarkAsPaid() {
        auction.startAuction();
        BidTransaction bid = new BidTransaction(bidder1, auction, 3000.0);
        auction.placeBid(bid);
        auction.endAuction();

        auction.markAsPaid();
        assertEquals(AuctionStatus.PAID, auction.getStatus());
        assertEquals(7000.0, bidder1.getBalance());
    }

    @Test
    public void testCancelDueToUnpaid() {
        auction.startAuction();

        BidTransaction bid = new BidTransaction(bidder1, auction, 3000.0);
        auction.placeBid(bid);
        auction.endAuction();

        auction.cancelDueToUnpaid();

        assertEquals(AuctionStatus.CANCELED, auction.getStatus());
        assertEquals(1, bidder1.getUnpaidWarnings());
    }
}