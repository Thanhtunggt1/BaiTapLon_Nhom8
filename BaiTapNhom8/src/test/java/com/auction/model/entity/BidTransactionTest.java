package com.auction.model.entity;

import com.auction.model.enums.ItemType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class BidTransactionTest {

    private Bidder bidder;
    private Auction auction;

    @BeforeEach
    public void setUp() {
        bidder = new Bidder("tx_bidder", "pass123", "tx@test.com", 50000.0);
        Seller seller = new Seller("tx_seller", "pass123", "s@test.com");

        Map<String, Object> params = new HashMap<>();
        params.put("mileage", 0.0);
        params.put("licensePlate", "NEW");
        Item item = seller.createItem("Bike", "New", 1000.0, ItemType.VEHICLE, params);

        auction = new Auction(item, seller, LocalDateTime.now(), LocalDateTime.now().plusHours(1));
    }

    @Test
    public void testInvalidTransactionAmountZero() {
        assertThrows(IllegalArgumentException.class, () -> new BidTransaction(bidder, auction, 0.0));
    }

    @Test
    public void testTransactionValidityWhenAuctionNotRunning() {
        BidTransaction tx = new BidTransaction(bidder, auction, 1500.0);
        assertFalse(tx.isValid());
    }

    @Test
    public void testTransactionValidityValidAmount() {
        auction.startAuction();
        BidTransaction tx = new BidTransaction(bidder, auction, 1500.0);
        assertTrue(tx.isValid());
    }

    @Test
    public void testTransactionValidityInvalidAmount() {
        auction.startAuction();
        BidTransaction tx = new BidTransaction(bidder, auction, 500.0);
        assertFalse(tx.isValid());
    }
}