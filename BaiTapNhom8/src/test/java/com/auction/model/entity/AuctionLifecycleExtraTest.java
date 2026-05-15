package com.auction.model.entity;

import com.auction.exception.AuctionClosedException;
import com.auction.exception.InsufficientBalanceException;
import com.auction.exception.InvalidBidException;
import com.auction.model.enums.AuctionStatus;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class AuctionLifecycleExtraTest {

    private Seller createSeller() {
        return new Seller("seller1", "123456", "seller@gmail.com");
    }

    private Bidder createBidder(String username, double balance) {
        return new Bidder(username, "123456", username + "@gmail.com", balance);
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
    void newAuctionShouldBeOpen() {
        Auction auction = createAuction();

        assertEquals(AuctionStatus.OPEN, auction.getStatus());
    }

    @Test
    void openAuctionCanBeStarted() {
        Auction auction = createAuction();

        auction.startAuction();

        assertEquals(AuctionStatus.RUNNING, auction.getStatus());
    }

    @Test
    void runningAuctionCanReceiveMultipleBids()
            throws AuctionClosedException, InvalidBidException, InsufficientBalanceException {

        Auction auction = createAuction();
        Bidder bidder1 = createBidder("bidder1", 1000.0);
        Bidder bidder2 = createBidder("bidder2", 1000.0);

        auction.startAuction();

        bidder1.placeBid(auction, 150.0);
        bidder2.placeBid(auction, 250.0);

        assertEquals(250.0, auction.getCurrentHighestPrice());
        assertEquals(bidder2, auction.getCurrentLeader());
        assertEquals(2, auction.getBidHistory().size());
    }

    @Test
    void lowerBidAfterHigherBidShouldFail()
            throws AuctionClosedException, InvalidBidException, InsufficientBalanceException {

        Auction auction = createAuction();
        Bidder bidder1 = createBidder("bidder1", 1000.0);
        Bidder bidder2 = createBidder("bidder2", 1000.0);

        auction.startAuction();
        bidder1.placeBid(auction, 300.0);

        assertThrows(InvalidBidException.class, () ->
                bidder2.placeBid(auction, 200.0)
        );
    }

    @Test
    void highestBidderShouldBeWinnerAfterAuctionFinished()
            throws AuctionClosedException, InvalidBidException, InsufficientBalanceException {

        Auction auction = createAuction();
        Bidder bidder1 = createBidder("bidder1", 1000.0);
        Bidder bidder2 = createBidder("bidder2", 1000.0);

        auction.startAuction();
        bidder1.placeBid(auction, 200.0);
        bidder2.placeBid(auction, 400.0);
        auction.endAuction();

        assertEquals(AuctionStatus.FINISHED, auction.getStatus());
        assertEquals(bidder2, auction.getCurrentLeader());
        assertEquals(400.0, auction.getCurrentHighestPrice());
    }

    @Test
    void finishedAuctionCannotReceiveNewBid()
            throws AuctionClosedException, InvalidBidException, InsufficientBalanceException {

        Auction auction = createAuction();
        Bidder bidder1 = createBidder("bidder1", 1000.0);
        Bidder bidder2 = createBidder("bidder2", 1000.0);

        auction.startAuction();
        bidder1.placeBid(auction, 200.0);
        auction.endAuction();

        assertThrows(AuctionClosedException.class, () ->
                bidder2.placeBid(auction, 300.0)
        );
    }

    @Test
    void canceledAuctionCannotReceiveBid() {
        Auction auction = createAuction();
        Bidder bidder = createBidder("bidder1", 1000.0);

        auction.cancelAuction();

        assertThrows(AuctionClosedException.class, () ->
                bidder.placeBid(auction, 200.0)
        );
    }

    @Test
    void auctionWithNoBidShouldBeCanceledWhenEnded() {
        Auction auction = createAuction();

        auction.startAuction();
        auction.endAuction();

        assertEquals(AuctionStatus.CANCELED, auction.getStatus());
    }

    @Test
    void finishedAuctionCanBeMarkedAsPaid()
            throws AuctionClosedException, InvalidBidException, InsufficientBalanceException {

        Auction auction = createAuction();
        Bidder bidder = createBidder("bidder1", 1000.0);

        auction.startAuction();
        bidder.placeBid(auction, 200.0);
        auction.endAuction();
        auction.markAsPaid();

        assertEquals(AuctionStatus.PAID, auction.getStatus());
    }

    @Test
    void openAuctionCannotBeMarkedAsPaid() {
        Auction auction = createAuction();

        assertThrows(IllegalStateException.class, auction::markAsPaid);
    }
}
