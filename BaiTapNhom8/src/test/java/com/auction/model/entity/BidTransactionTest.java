package com.auction.model.entity;

import com.auction.model.enums.AuctionStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class BidTransactionTest {

    private Bidder bidder;
    private Auction dummyAuction;

    @BeforeEach
    void setUp() {
        bidder = new Bidder("bidder", "123456", "b@test.com", 10000.0);
        Seller seller = new Seller("seller", "123456", "s@test.com");
        Item item = new Item("Xe máy", "Cũ", 2000.0) {};
        dummyAuction = new Auction(item, seller, LocalDateTime.now(), LocalDateTime.now().plusDays(1));
    }

    @Test
    void testIsValid_FalseWhenAuctionNotRunning() {
        // Auction vừa tạo, status đang là OPEN (chưa start)
        BidTransaction tx = new BidTransaction(bidder, dummyAuction, 2500.0);

        assertFalse(tx.isValid(), "Giao dịch không hợp lệ nếu phiên chưa RUNNING");
    }

    @Test
    void testIsValid_FalseWhenAmountNotHigherThanCurrent() {
        dummyAuction.startAuction(); // Giá gốc đang là 2000.0

        // Bidder cố tình đặt 2000.0 (Bằng với giá gốc)
        BidTransaction tx = new BidTransaction(bidder, dummyAuction, 2000.0);

        assertFalse(tx.isValid(), "Giao dịch không hợp lệ nếu số tiền không cao hơn giá hiện tại");
    }

    @Test
    void testIsValid_TrueWhenValid() {
        dummyAuction.startAuction();

        // Đặt giá 2100.0 (Cao hơn 2000.0)
        BidTransaction tx = new BidTransaction(bidder, dummyAuction, 2100.0);

        assertTrue(tx.isValid(), "Giao dịch hợp lệ");
    }
}