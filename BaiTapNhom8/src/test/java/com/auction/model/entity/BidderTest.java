package com.auction.model.entity;

import com.auction.exception.AuctionClosedException;
import com.auction.exception.InsufficientBalanceException;
import com.auction.model.enums.AuctionStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class BidderTest {

    private Bidder bidder;
    private Auction dummyAuction;

    @BeforeEach
    void setUp() {
        bidder = new Bidder("test_user", "123456", "test@test.com", 1000.0);
        Seller seller = new Seller("seller", "123456", "s@test.com");
        Item item = new Item("Đồ cổ", "Mô tả", 500.0) {}; // Lớp nặc danh để test Item abstract
        dummyAuction = new Auction(item, seller, LocalDateTime.now(), LocalDateTime.now().plusDays(1));
    }

    @Test
    void testPlaceBid_InsufficientBalance_ThrowsException() {
        dummyAuction.startAuction();

        // Bidder có 1000, cố tình đặt giá 2000
        Exception exception = assertThrows(InsufficientBalanceException.class, () -> {
            bidder.placeBid(dummyAuction, 2000.0);
        });
        assertTrue(exception.getMessage().contains("không đủ"));
    }

    @Test
    void testPlaceBid_AuctionNotRunning_ThrowsException() {
        // Cố tình không gọi dummyAuction.startAuction() để trạng thái vẫn là OPEN
        Exception exception = assertThrows(AuctionClosedException.class, () -> {
            bidder.placeBid(dummyAuction, 600.0);
        });
        assertTrue(exception.getMessage().contains("không ở trạng thái RUNNING"));
    }

    @Test
    void testDepositAndDeduct_Success() {
        bidder.deposit(500.0);
        assertEquals(1500.0, bidder.getBalance());
        bidder.deduct(200.0);
        assertEquals(1300.0, bidder.getBalance());
    }
}