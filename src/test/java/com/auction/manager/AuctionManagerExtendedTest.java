package com.auction.manager;

import com.auction.model.entity.*;
import com.auction.model.enums.AuctionStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AuctionManagerExtendedTest {

    private AuctionManager manager;
    private Seller seller;
    private Item item;
    private Bidder bidder1;
    private Bidder bidder2;

    @BeforeEach
    void setUp() {
        manager = AuctionManager.getInstance();
        seller = new Seller("seller1", "pass123", "seller@test.com");
        item = new Art("Tranh", "Mô tả", 100.0, "Artist");
        bidder1 = new Bidder("bidder1", "pass123", "bidder1@test.com", 1000.0);
        bidder2 = new Bidder("bidder2", "pass123", "bidder2@test.com", 1000.0);
    }

    @Test
    void testRegisterAuction() {
        LocalDateTime start = LocalDateTime.now();
        LocalDateTime end = start.plusDays(1);
        Auction auction = new Auction(item, seller, start, end);

        assertDoesNotThrow(() -> manager.registerAuction(auction));
        assertTrue(manager.getAllAuctions().stream()
                .anyMatch(a -> a.getId().equals(auction.getId())));
    }

    @Test
    void testRegisterNullAuction() {
        assertThrows(IllegalArgumentException.class, () ->
            manager.registerAuction(null),
            "Không được đăng ký phiên null");
    }

    @Test
    void testRegisterDuplicateAuction() {
        LocalDateTime start = LocalDateTime.now();
        LocalDateTime end = start.plusDays(1);
        Auction auction = new Auction(item, seller, start, end);

        manager.registerAuction(auction);
        manager.registerAuction(auction); // Đăng ký lần 2

        // Phiên chỉ được đăng ký 1 lần
        long count = manager.getAllAuctions().stream()
                .filter(a -> a.getId().equals(auction.getId()))
                .count();
        assertEquals(1, count);
    }

    @Test
    void testStartAuction() {
        LocalDateTime start = LocalDateTime.now();
        LocalDateTime end = start.plusDays(1);
        Auction auction = new Auction(item, seller, start, end);

        manager.registerAuction(auction);
        assertDoesNotThrow(() -> manager.startAuction(auction));
        assertEquals(AuctionStatus.RUNNING, auction.getStatus());
    }

    @Test
    void testStartUnregisteredAuction() {
        LocalDateTime start = LocalDateTime.now();
        LocalDateTime end = start.plusDays(1);
        Auction auction = new Auction(item, seller, start, end);

        assertThrows(IllegalArgumentException.class, () ->
            manager.startAuction(auction),
            "Không thể start phiên chưa đăng ký");
    }

    @Test
    void testGetAllAuctions() {
        LocalDateTime start = LocalDateTime.now();
        LocalDateTime end = start.plusDays(1);

        Auction auction1 = new Auction(item, seller, start, end);
        Auction auction2 = new Auction(item, seller, start, end);

        manager.registerAuction(auction1);
        manager.registerAuction(auction2);

        List<Auction> auctions = manager.getAllAuctions();
        assertTrue(auctions.size() >= 2);
    }

    @Test
    void testGetAllAuctionsIsImmutable() {
        LocalDateTime start = LocalDateTime.now();
        LocalDateTime end = start.plusDays(1);
        Auction auction = new Auction(item, seller, start, end);

        manager.registerAuction(auction);
        List<Auction> auctions = manager.getAllAuctions();

        assertThrows(UnsupportedOperationException.class, () ->
            auctions.clear(),
            "Danh sách phiên phải immutable");
    }

    @Test
    void testCheckAndCloseExpiredAuctions_NoAutoClose() {
        LocalDateTime start = LocalDateTime.now();
        LocalDateTime end = start.plusDays(1); // Chưa hết hạn

        Auction auction = new Auction(item, seller, start, end);
        manager.registerAuction(auction);
        manager.startAuction(auction);

        assertEquals(AuctionStatus.RUNNING, auction.getStatus());
        manager.checkAndCloseExpiredAuctions();

        // Phiên vẫn RUNNING vì chưa hết hạn
        assertEquals(AuctionStatus.RUNNING, auction.getStatus());
    }

    @Test
    void testCheckAndCloseExpiredAuctions_AutoStartScheduled() {
        LocalDateTime start = LocalDateTime.now().minusSeconds(10); // Đã qua thời điểm bắt đầu
        LocalDateTime end = start.plusDays(1);

        Auction auctionAutoStart = new Auction(item, seller, start, end);
        manager.registerAuction(auctionAutoStart);

        manager.checkAndCloseExpiredAuctions();

        // Phiên sẽ tự động bắt đầu
        assertEquals(AuctionStatus.RUNNING, auctionAutoStart.getStatus());
    }

    @Test
    void testCheckAndCloseExpiredAuctions_CancelFinishedExpired() {
        LocalDateTime start = LocalDateTime.now().minusDays(2);
        LocalDateTime end = start.plusDays(1);

        Auction finishedAuction = new Auction(item, seller, start, end);
        manager.registerAuction(finishedAuction);
        manager.startAuction(finishedAuction);

        // Giả lập có bid
        BidTransaction bid = new BidTransaction(bidder1, finishedAuction, 150.0);
        finishedAuction.placeBid(bid);

        finishedAuction.endAuction();
        assertEquals(AuctionStatus.FINISHED, finishedAuction.getStatus());

        finishedAuction.markAsPaid();
        assertEquals(AuctionStatus.PAID, finishedAuction.getStatus());

        finishedAuction.setFinishedTime(LocalDateTime.now().minusHours(13)); // 13 giờ trước

        manager.checkAndCloseExpiredAuctions();

        // Phiên đã quá hạn thanh toán (12h) sẽ bị hủy
        assertEquals(AuctionStatus.CANCELED, finishedAuction.getStatus());
    }

    @Test
    void testMultipleAuctionsConcurrent() {
        for (int i = 0; i < 5; i++) {
            Item tempItem = new Art("Item " + i, "Description", 100.0, "Artist");
            LocalDateTime start = LocalDateTime.now();
            LocalDateTime end = start.plusDays(1);
            Auction auction = new Auction(tempItem, seller, start, end);
            manager.registerAuction(auction);
        }

        assertTrue(manager.getAllAuctions().size() >= 5);
    }
}

