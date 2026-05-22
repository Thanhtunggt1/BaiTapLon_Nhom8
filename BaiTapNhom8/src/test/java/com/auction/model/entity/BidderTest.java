package com.auction.model.entity;

import com.auction.exception.InsufficientBalanceException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import static org.junit.jupiter.api.Assertions.*;

public class BidderTest {

    private Bidder bidder;
    private Auction dummyAuction;
    private Seller dummySeller;
    private Electronics dummyItem;

    @BeforeEach
    public void setUp() {
        bidder = new Bidder("tung_bidder", "password123", "tung@test.com", 10000.0);
        dummySeller = new Seller("seller_test", "pass123", "seller@test.com");
        dummyItem = new Electronics("Laptop", "Core i7", 5000.0, "Dell", 12);

        try {
            java.lang.reflect.Field itemsField = Seller.class.getDeclaredField("items");
            itemsField.setAccessible(true);
            ((java.util.List<Item>) itemsField.get(dummySeller)).add(dummyItem);
        } catch (Exception ignored) {}

        LocalDateTime now = LocalDateTime.now();
        dummyAuction = new Auction(dummyItem, dummySeller, now, now.plusHours(1));
    }

    @Test
    public void testDepositSuccess() {
        bidder.deposit(5000.0);
        assertEquals(15000.0, bidder.getBalance());
    }

    @Test
    public void testDepositNegativeAmountThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> bidder.deposit(-1000.0));
    }

    @Test
    public void testDeductSuccess() {
        bidder.deduct(4000.0);
        assertEquals(6000.0, bidder.getBalance());
    }

    @Test
    public void testDeductInsufficientBalanceThrowsException() {
        assertThrows(InsufficientBalanceException.class, () -> bidder.deduct(20000.0));
    }

    @Test
    public void testAddUnpaidWarningBansUser() {
        bidder.addUnpaidWarning();
        bidder.addUnpaidWarning();
        assertFalse(bidder.isBanned());

        bidder.addUnpaidWarning();
        assertTrue(bidder.isBanned());
    }

    @Test
    public void testDepositLockoutMechanism() {
        bidder.recordFailedDeposit();
        bidder.recordFailedDeposit();
        assertFalse(bidder.isDepositLocked());

        bidder.recordFailedDeposit();
        assertTrue(bidder.isDepositLocked());
        assertTrue(bidder.getDepositLockRemainingSeconds() > 0);

        bidder.resetFailedDeposit();
        assertFalse(bidder.isDepositLocked());
    }

    @Test
    public void testSetupAutoBidInvalidMaxBid() {
        dummyAuction.startAuction();
        assertThrows(IllegalArgumentException.class, () -> {
            bidder.setupAutoBid(dummyAuction, 4000.0, 500.0);
        });
    }

    @Test
    public void testSetupAutoBidExceedsBalance() {
        dummyAuction.startAuction();
        assertThrows(IllegalArgumentException.class, () -> {
            bidder.setupAutoBid(dummyAuction, 15000.0, 1000.0);
        });
    }
}