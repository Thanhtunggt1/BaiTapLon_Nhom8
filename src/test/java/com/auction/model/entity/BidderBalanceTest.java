package com.auction.model.entity;

import com.auction.exception.InsufficientBalanceException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BidderBalanceTest {

    private Bidder bidder;

    @BeforeEach
    void setUp() {
        bidder = new Bidder("testBidder", "password123", "bidder@test.com", 5000.0);
    }

    @Test
    void testBidderInitialBalance() {
        assertEquals(5000.0, bidder.getBalance());
    }

    @Test
    void testBidderDeposit() {
        bidder.deposit(1000.0);
        assertEquals(6000.0, bidder.getBalance());

        bidder.deposit(500.0);
        assertEquals(6500.0, bidder.getBalance());
    }

    @Test
    void testBidderDepositInvalid() {
        assertThrows(IllegalArgumentException.class, () ->
            bidder.deposit(0),
            "Không được nạp 0 đồng");

        assertThrows(IllegalArgumentException.class, () ->
            bidder.deposit(-100.0),
            "Không được nạp số âm");
    }

    @Test
    void testBidderDeduct() {
        bidder.deduct(1000.0);
        assertEquals(4000.0, bidder.getBalance());

        bidder.deduct(500.0);
        assertEquals(3500.0, bidder.getBalance());
    }

    @Test
    void testBidderDeductExactAmount() {
        bidder.deduct(5000.0);
        assertEquals(0.0, bidder.getBalance());
    }

    @Test
    void testBidderDeductInsufficientBalance() {
        assertThrows(InsufficientBalanceException.class, () ->
            bidder.deduct(6000.0),
            "Không thể trừ nhiều hơn số dư");
    }

    @Test
    void testBidderDeductAlmostExceedsBalance() {
        assertThrows(InsufficientBalanceException.class, () ->
            bidder.deduct(5000.01),
            "Không thể trừ hơn số dư");
    }

    @Test
    void testBidderNegativeInitialBalance() {
        assertThrows(IllegalArgumentException.class, () ->
            new Bidder("user", "pass", "email@test.com", -100.0),
            "Không được có số dư âm");
    }

    @Test
    void testBidderZeroInitialBalance() {
        Bidder zeroBidder = new Bidder("user", "pass", "email@test.com", 0.0);
        assertEquals(0.0, zeroBidder.getBalance());
    }

    @Test
    void testBidderDepositMultipleTimes() {
        bidder.deposit(100.0);
        bidder.deposit(200.0);
        bidder.deposit(300.0);
        assertEquals(5600.0, bidder.getBalance());
    }

    @Test
    void testBidderDeductMultipleTimes() {
        bidder.deduct(100.0);
        bidder.deduct(200.0);
        bidder.deduct(300.0);
        assertEquals(4400.0, bidder.getBalance());
    }

    @Test
    void testBidderComplexTransactions() {
        bidder.deposit(1000.0);  // 6000
        bidder.deduct(500.0);     // 5500
        bidder.deposit(2000.0);   // 7500
        bidder.deduct(1500.0);    // 6000

        assertEquals(6000.0, bidder.getBalance());
    }

    @Test
    void testBidderLargeBalance() {
        Bidder richBidder = new Bidder("rich", "pass", "email@test.com", 1000000.0);
        assertEquals(1000000.0, richBidder.getBalance());

        richBidder.deduct(500000.0);
        assertEquals(500000.0, richBidder.getBalance());
    }

    @Test
    void testBidderDecimalBalance() {
        Bidder decimalBidder = new Bidder("decimal", "pass", "email@test.com", 123.45);
        assertEquals(123.45, decimalBidder.getBalance());

        decimalBidder.deposit(67.89);
        assertEquals(191.34, decimalBidder.getBalance(), 0.01);
    }

    @Test
    void testBidderPrintInfo() {
        assertDoesNotThrow(() -> bidder.printInfo(),
            "Hàm printInfo không được throw exception");
    }
}

