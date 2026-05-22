package com.auction.model.entity;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class AutoBidConfigExtraTest {

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
    void autoBidConfigShouldBeCreatedSuccessfully() {
        Bidder bidder = createBidder();
        Auction auction = createAuction();

        AutoBidConfig config = new AutoBidConfig(bidder, auction, 500.0, 50.0);

        assertNotNull(config.getId());
        assertEquals(bidder, config.getBidder());
        assertEquals(auction, config.getAuction());
        assertEquals(500.0, config.getMaxBid());
    }

    @Test
    void bidderCannotBeNull() {
        Auction auction = createAuction();

        assertThrows(IllegalArgumentException.class, () ->
                new AutoBidConfig(null, auction, 500.0, 50.0)
        );
    }

    @Test
    void auctionCannotBeNull() {
        Bidder bidder = createBidder();

        assertThrows(IllegalArgumentException.class, () ->
                new AutoBidConfig(bidder, null, 500.0, 50.0)
        );
    }

    @Test
    void maxBidMustBePositive() {
        Bidder bidder = createBidder();
        Auction auction = createAuction();

        assertThrows(IllegalArgumentException.class, () ->
                new AutoBidConfig(bidder, auction, 0, 50.0)
        );

        assertThrows(IllegalArgumentException.class, () ->
                new AutoBidConfig(bidder, auction, -100.0, 50.0)
        );
    }

    @Test
    void incrementMustBePositive() {
        Bidder bidder = createBidder();
        Auction auction = createAuction();

        assertThrows(IllegalArgumentException.class, () ->
                new AutoBidConfig(bidder, auction, 500.0, 0)
        );

        assertThrows(IllegalArgumentException.class, () ->
                new AutoBidConfig(bidder, auction, 500.0, -10.0)
        );
    }

    @Test
    void computeNextBidShouldAddIncrement() {
        Bidder bidder = createBidder();
        Auction auction = createAuction();

        AutoBidConfig config = new AutoBidConfig(bidder, auction, 500.0, 50.0);

        assertEquals(150.0, config.computeNextBid(100.0));
    }

    @Test
    void computeNextBidShouldReturnMinusOneWhenNextBidWouldExceedMaxBid() {
        Bidder bidder = createBidder();
        Auction auction = createAuction();

        AutoBidConfig config = new AutoBidConfig(bidder, auction, 500.0, 50.0);

        assertEquals(-1.0, config.computeNextBid(480.0));
    }

    @Test
    void computeNextBidShouldReturnMinusOneWhenAlreadyAtMaxBid() {
        Bidder bidder = createBidder();
        Auction auction = createAuction();

        AutoBidConfig config = new AutoBidConfig(bidder, auction, 500.0, 50.0);

        assertEquals(-1.0, config.computeNextBid(500.0));
    }

    @Test
    void compareToShouldOrderByRegisteredTime() throws InterruptedException {
        Bidder bidder1 = new Bidder("bidder1", "123456", "b1@gmail.com", 1000.0);
        Bidder bidder2 = new Bidder("bidder2", "123456", "b2@gmail.com", 1000.0);
        Auction auction = createAuction();

        AutoBidConfig config1 = new AutoBidConfig(bidder1, auction, 500.0, 50.0);
        Thread.sleep(5);
        AutoBidConfig config2 = new AutoBidConfig(bidder2, auction, 500.0, 50.0);

        assertTrue(config1.compareTo(config2) < 0);
    }

    @Test
    void twoConfigsShouldReferenceSameAuctionWhenCreatedForSameAuction() {
        Auction auction = createAuction();

        AutoBidConfig config1 = new AutoBidConfig(
                new Bidder("bidder1", "123456", "b1@gmail.com", 1000.0),
                auction,
                500.0,
                50.0
        );

        AutoBidConfig config2 = new AutoBidConfig(
                new Bidder("bidder2", "123456", "b2@gmail.com", 1000.0),
                auction,
                700.0,
                100.0
        );

        assertSame(auction, config1.getAuction());
        assertSame(auction, config2.getAuction());
    }
}