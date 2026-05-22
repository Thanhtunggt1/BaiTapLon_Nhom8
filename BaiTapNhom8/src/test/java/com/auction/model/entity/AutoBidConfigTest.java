package com.auction.model.entity;

import com.auction.model.enums.ItemType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class AutoBidConfigTest {

    private Bidder bidder;
    private Auction auction;

    @BeforeEach
    public void setUp() {
        bidder = new Bidder("auto_bidder", "pass123", "auto@test.com", 100000.0);
        Seller seller = new Seller("auto_seller", "pass123", "s@test.com");

        Map<String, Object> params = new HashMap<>();
        params.put("brand", "Asus");
        params.put("warrantyMonths", 24);
        Item item = seller.createItem("PC", "Gaming", 20000.0, ItemType.ELECTRONICS, params);

        auction = new Auction(item, seller, LocalDateTime.now(), LocalDateTime.now().plusHours(1));
    }

    @Test
    public void testComputeNextBidWithinLimit() {
        AutoBidConfig config = new AutoBidConfig(bidder, auction, 30000.0, 1000.0);
        double nextBid = config.computeNextBid(25000.0);
        assertEquals(26000.0, nextBid);
    }

    @Test
    public void testComputeNextBidExceedsLimit() {
        AutoBidConfig config = new AutoBidConfig(bidder, auction, 30000.0, 5000.0);
        double nextBid = config.computeNextBid(28000.0);
        assertEquals(-1, nextBid);
    }

    @Test
    public void testInvalidConfigParameters() {
        assertThrows(IllegalArgumentException.class, () -> new AutoBidConfig(bidder, auction, -100.0, 10.0));
        assertThrows(IllegalArgumentException.class, () -> new AutoBidConfig(bidder, auction, 100.0, -10.0));
    }
}