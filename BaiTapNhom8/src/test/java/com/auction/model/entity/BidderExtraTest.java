package com.auction.model.entity;

import com.auction.exception.AuctionClosedException;
import com.auction.exception.InsufficientBalanceException;
import com.auction.exception.InvalidBidException;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class BidderExtraTest {

    private Seller createSeller() {
        return new Seller("seller1", "123456", "seller@gmail.com");
    }

    private Art createItem() {
        return new Art("Tranh cổ", "Tranh đẹp", 100.0, "Picasso", 1990);
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
    void bidderShouldBeCreatedSuccessfully() {
        Bidder bidder = new Bidder("bidder1", "123456", "bidder@gmail.com", 1000.0);

        assertNotNull(bidder.getId());
        assertEquals("bidder1", bidder.getUsername());
        assertEquals("bidder@gmail.com", bidder.getEmail());
        assertEquals(1000.0, bidder.getBalance());
    }

    @Test
    void bidderInitialBalanceCannotBeNegative() {
        assertThrows(IllegalArgumentException.class, () ->
                new Bidder("bidder1", "123456", "bidder@gmail.com", -1.0)
        );
    }

    @Test
    void depositShouldIncreaseBalance() {
        Bidder bidder = new Bidder("bidder1", "123456", "bidder@gmail.com", 1000.0);

        bidder.deposit(500.0);

        assertEquals(1500.0, bidder.getBalance());
    }

    @Test
    void depositCannotBeZero() {
        Bidder bidder = new Bidder("bidder1", "123456", "bidder@gmail.com", 1000.0);

        assertThrows(IllegalArgumentException.class, () ->
                bidder.deposit(0)
        );
    }

    @Test
    void depositCannotBeNegative() {
        Bidder bidder = new Bidder("bidder1", "123456", "bidder@gmail.com", 1000.0);

        assertThrows(IllegalArgumentException.class, () ->
                bidder.deposit(-100.0)
        );
    }

    @Test
    void deductShouldDecreaseBalance() throws InsufficientBalanceException {
        Bidder bidder = new Bidder("bidder1", "123456", "bidder@gmail.com", 1000.0);

        bidder.deduct(300.0);

        assertEquals(700.0, bidder.getBalance());
    }

    @Test
    void deductCannotBeGreaterThanBalance() {
        Bidder bidder = new Bidder("bidder1", "123456", "bidder@gmail.com", 1000.0);

        assertThrows(InsufficientBalanceException.class, () ->
                bidder.deduct(1500.0)
        );
    }

    @Test
    void bidderCanPlaceValidBid()
            throws AuctionClosedException, InvalidBidException, InsufficientBalanceException {

        Bidder bidder = new Bidder("bidder1", "123456", "bidder@gmail.com", 1000.0);
        Auction auction = createAuction();

        auction.startAuction();
        boolean result = bidder.placeBid(auction, 200.0);

        assertTrue(result);
        assertEquals(bidder, auction.getCurrentLeader());
        assertEquals(200.0, auction.getCurrentHighestPrice());
    }

    @Test
    void bidderCannotPlaceBidWhenAuctionIsNotRunning() {
        Bidder bidder = new Bidder("bidder1", "123456", "bidder@gmail.com", 1000.0);
        Auction auction = createAuction();

        assertThrows(AuctionClosedException.class, () ->
                bidder.placeBid(auction, 200.0)
        );
    }

    @Test
    void bidderCannotPlaceBidLowerThanCurrentPrice() {
        Bidder bidder = new Bidder("bidder1", "123456", "bidder@gmail.com", 1000.0);
        Auction auction = createAuction();

        auction.startAuction();

        assertThrows(InvalidBidException.class, () ->
                bidder.placeBid(auction, 50.0)
        );
    }

    @Test
    void bidderCannotPlaceBidGreaterThanBalance() {
        Bidder bidder = new Bidder("bidder1", "123456", "bidder@gmail.com", 150.0);
        Auction auction = createAuction();

        auction.startAuction();

        assertThrows(InsufficientBalanceException.class, () ->
                bidder.placeBid(auction, 200.0)
        );
    }

    @Test
    void bidderCanSetupValidAutoBid() {
        Bidder bidder = new Bidder("bidder1", "123456", "bidder@gmail.com", 1000.0);
        Auction auction = createAuction();

        assertDoesNotThrow(() ->
                bidder.setupAutoBid(auction, 500.0, 50.0)
        );
    }

    @Test
    void bidderCannotSetupAutoBidWithNullAuction() {
        Bidder bidder = new Bidder("bidder1", "123456", "bidder@gmail.com", 1000.0);

        assertThrows(IllegalArgumentException.class, () ->
                bidder.setupAutoBid(null, 500.0, 50.0)
        );
    }

    @Test
    void bidderCannotSetupAutoBidWithMaxBidLowerThanCurrentPrice() {
        Bidder bidder = new Bidder("bidder1", "123456", "bidder@gmail.com", 1000.0);
        Auction auction = createAuction();

        assertThrows(IllegalArgumentException.class, () ->
                bidder.setupAutoBid(auction, 50.0, 10.0)
        );
    }

    @Test
    void bidderCanPrintInfoWithoutException() {
        Bidder bidder = new Bidder("bidder1", "123456", "bidder@gmail.com", 1000.0);

        assertDoesNotThrow(bidder::printInfo);
    }
}
