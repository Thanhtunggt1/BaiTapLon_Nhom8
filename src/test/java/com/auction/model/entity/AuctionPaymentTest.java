ếttpackage com.auction.model.entity;

import com.auction.model.enums.AuctionStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class AuctionPaymentTest {

    private Seller seller;
    private Item item;
    private Auction auction;
    private Bidder bidder;

    @BeforeEach
    void setUp() {
        seller = new Seller("seller", "pass123", "seller@test.com");
        bidder = new Bidder("bidder", "pass123", "bidder@test.com", 10000.0);
        item = new Art("Tranh", "Mô tả", 500.0, "Artist");

        LocalDateTime start = LocalDateTime.now();
        LocalDateTime end = start.plusDays(1);
        auction = new Auction(item, seller, start, end);
    }

    @Test
    void testPaymentFlow() {
        auction.startAuction();
        bidder.placeBid(auction, 1000.0);

        assertEquals(AuctionStatus.RUNNING, auction.getStatus());
        assertEquals(10000.0, bidder.getBalance());

        auction.endAuction();
        assertEquals(AuctionStatus.FINISHED, auction.getStatus());

        auction.markAsPaid();
        assertEquals(AuctionStatus.PAID, auction.getStatus());
        assertEquals(9000.0, bidder.getBalance(), "Balance phải giảm 1000");
    }

    @Test
    void testPaymentDeductsCorrectAmount() {
        auction.startAuction();
        bidder.placeBid(auction, 2500.0);

        auction.endAuction();
        double balanceBeforePayment = bidder.getBalance();

        auction.markAsPaid();
        assertEquals(balanceBeforePayment - 2500.0, bidder.getBalance());
    }

    @Test
    void testPaymentOnlyWhenFinished() {
        auction.startAuction();

        assertThrows(IllegalStateException.class, () ->
            auction.markAsPaid(),
            "Chỉ phiên FINISHED mới có thể thanh toán");
    }

    @Test
    void testPaymentOnCanceledAuction() {
        auction.startAuction();
        auction.endAuction(); // Không có bid -> CANCELED

        assertEquals(AuctionStatus.CANCELED, auction.getStatus());

        assertThrows(IllegalStateException.class, () ->
            auction.markAsPaid(),
            "Phiên CANCELED không thể thanh toán");
    }

    @Test
    void testPaymentWithMultipleBidTransitions() {
        auction.startAuction();

        Bidder bidder1 = new Bidder("b1", "pass", "b1@test.com", 5000.0);
        Bidder bidder2 = new Bidder("b2", "pass", "b2@test.com", 5000.0);

        bidder1.placeBid(auction, 1000.0);
        bidder2.placeBid(auction, 1500.0);
        bidder1.placeBid(auction, 2000.0);

        assertEquals(bidder1, auction.getCurrentLeader());
        assertEquals(2000.0, auction.getCurrentHighestPrice());

        auction.endAuction();
        auction.markAsPaid();

        assertEquals(AuctionStatus.PAID, auction.getStatus());
        assertEquals(3000.0, bidder1.getBalance(), "Final bid was 2000");
    }

    @Test
    void testPaymentWithInsufficientBalance() {
        Bidder poorBidder = new Bidder("poor", "pass", "poor@test.com", 500.0);
        auction.startAuction();

        poorBidder.placeBid(auction, 400.0); // OK
        auction.endAuction();

        poorBidder.deduct(200.0); // Now has 300

        assertThrows(IllegalStateException.class, () ->
            auction.markAsPaid(),
            "Không đủ tiền để thanh toán");
    }

    @Test
    void testPaymentMultipleTimes() {
        auction.startAuction();
        bidder.placeBid(auction, 1000.0);
        auction.endAuction();

        auction.markAsPaid();
        assertEquals(AuctionStatus.PAID, auction.getStatus());

        // Cố gắng thanh toán lần 2
        assertThrows(IllegalStateException.class, () ->
            auction.markAsPaid(),
            "Không thể thanh toán 2 lần");
    }

    @Test
    void testNoPaymentWhenNoBidders() {
        auction.startAuction();
        auction.endAuction(); // No bid, CANCELED

        assertEquals(AuctionStatus.CANCELED, auction.getStatus());
        assertNull(auction.getCurrentLeader());
    }

    @Test
    void testPaymentBalanceAfterMultipleBids() {
        auction.startAuction();

        double initialBalance = bidder.getBalance();
        bidder.placeBid(auction, 1000.0);

        assertEquals(initialBalance, bidder.getBalance(), "Balance không giảm khi bid");

        auction.endAuction();
        auction.markAsPaid();

        assertEquals(initialBalance - 1000.0, bidder.getBalance(), "Balance giảm khi thanh toán");
    }

    @Test
    void testPaymentNotificationObservers() {
        auction.startAuction();
        bidder.placeBid(auction, 1000.0);

        auction.endAuction();
        assertDoesNotThrow(() -> auction.markAsPaid());
    }

    @Test
    void testPaymentWithDecimalBidAmount() {
        auction.startAuction();
        bidder.placeBid(auction, 1234.56);

        auction.endAuction();
        auction.markAsPaid();

        assertEquals(10000.0 - 1234.56, bidder.getBalance(), 0.01);
    }
}

