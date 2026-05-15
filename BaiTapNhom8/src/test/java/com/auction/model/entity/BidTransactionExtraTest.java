package com.auction.model.entity;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class BidTransactionExtraTest {

    private Art createItem() {
        return new Art("Tranh cổ", "Tranh đẹp", 100.0, "Picasso", 1990);
    }

    private Seller createSeller() {
        return new Seller("seller1", "123456", "seller@gmail.com");
    }

    private Bidder createBidder() {
        return new Bidder("bidder1", "123456", "bidder@gmail.com", 1000.0);
    }

    private Auction createAuction() {
        return new Auction(
                createItem(),
                createSeller(),
                LocalDateTime.now().minusMinutes(1),
                LocalDateTime.now().plusMinutes(10)
        );
    }

    @Test
    void bidTransactionShouldBeCreatedSuccessfully() {
        Bidder bidder = createBidder();
        Auction auction = createAuction();

        BidTransaction bid = new BidTransaction(bidder, auction, 150.0);

        assertNotNull(bid.getId());
        assertEquals(bidder, bid.getBidder());
        assertEquals(auction, bid.getAuction());
        assertEquals(150.0, bid.getAmount());
    }

    @Test
    void bidTransactionShouldThrowExceptionWhenBidderIsNull() {
        Auction auction = createAuction();

        assertThrows(IllegalArgumentException.class, () ->
                new BidTransaction(null, auction, 150.0)
        );
    }

    @Test
    void bidTransactionShouldThrowExceptionWhenAuctionIsNull() {
        Bidder bidder = createBidder();

        assertThrows(IllegalArgumentException.class, () ->
                new BidTransaction(bidder, null, 150.0)
        );
    }

    @Test
    void bidAmountCannotBeZero() {
        Bidder bidder = createBidder();
        Auction auction = createAuction();

        assertThrows(IllegalArgumentException.class, () ->
                new BidTransaction(bidder, auction, 0)
        );
    }

    @Test
    void bidAmountCannotBeNegative() {
        Bidder bidder = createBidder();
        Auction auction = createAuction();

        assertThrows(IllegalArgumentException.class, () ->
                new BidTransaction(bidder, auction, -100.0)
        );
    }

    @Test
    void bidShouldBeInvalidWhenAuctionIsNotRunning() {
        Bidder bidder = createBidder();
        Auction auction = createAuction();

        BidTransaction bid = new BidTransaction(bidder, auction, 150.0);

        assertFalse(bid.isValid());
    }

    @Test
    void bidShouldBeValidWhenAuctionIsRunningAndAmountHigherThanCurrentPrice() {
        Bidder bidder = createBidder();
        Auction auction = createAuction();

        auction.startAuction();

        BidTransaction bid = new BidTransaction(bidder, auction, 150.0);

        assertTrue(bid.isValid());
    }

    @Test
    void bidShouldBeInvalidWhenAmountLowerThanCurrentPrice() {
        Bidder bidder = createBidder();
        Auction auction = createAuction();

        auction.startAuction();

        BidTransaction bid = new BidTransaction(bidder, auction, 50.0);

        assertFalse(bid.isValid());
    }

    @Test
    void toStringShouldContainImportantInformation() {
        Bidder bidder = createBidder();
        Auction auction = createAuction();

        BidTransaction bid = new BidTransaction(bidder, auction, 150.0);

        String result = bid.toString();

        assertTrue(result.contains("BidTransaction"));
        assertTrue(result.contains("bidder1"));
        assertTrue(result.contains("150"));
    }
}
