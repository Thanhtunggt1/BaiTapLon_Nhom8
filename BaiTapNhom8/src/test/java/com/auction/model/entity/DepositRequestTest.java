package com.auction.model.entity;

import com.auction.model.enums.DepositStatus;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DepositRequestTest {

    @Test
    void bidderCanCreateDepositRequest() {
        Bidder bidder = new Bidder("bidder1", "123456", "bidder@gmail.com", 1000.0);

        DepositRequest request = bidder.requestDeposit(500.0);

        assertNotNull(request.getId());
        assertEquals(bidder, request.getBidder());
        assertEquals(500.0, request.getAmount());
        assertEquals(DepositStatus.PENDING, request.getStatus());
        assertNotNull(request.getCreatedAt());
    }

    @Test
    void depositRequestDoesNotIncreaseBalanceImmediately() {
        Bidder bidder = new Bidder("bidder1", "123456", "bidder@gmail.com", 1000.0);

        bidder.requestDeposit(500.0);

        assertEquals(1000.0, bidder.getBalance());
    }

    @Test
    void adminApproveDepositShouldIncreaseBidderBalance() {
        Bidder bidder = new Bidder("bidder1", "123456", "bidder@gmail.com", 1000.0);
        Admin admin = new Admin("admin1", "123456", "admin@gmail.com");

        DepositRequest request = bidder.requestDeposit(500.0);
        admin.approveDeposit(request);

        assertEquals(1500.0, bidder.getBalance());
        assertEquals(DepositStatus.APPROVED, request.getStatus());
    }

    @Test
    void adminRejectDepositShouldNotIncreaseBidderBalance() {
        Bidder bidder = new Bidder("bidder1", "123456", "bidder@gmail.com", 1000.0);
        Admin admin = new Admin("admin1", "123456", "admin@gmail.com");

        DepositRequest request = bidder.requestDeposit(500.0);
        admin.rejectDeposit(request);

        assertEquals(1000.0, bidder.getBalance());
        assertEquals(DepositStatus.REJECTED, request.getStatus());
    }

    @Test
    void cannotCreateDepositRequestWithZeroAmount() {
        Bidder bidder = new Bidder("bidder1", "123456", "bidder@gmail.com", 1000.0);

        assertThrows(IllegalArgumentException.class, () ->
                bidder.requestDeposit(0)
        );
    }

    @Test
    void cannotCreateDepositRequestWithNegativeAmount() {
        Bidder bidder = new Bidder("bidder1", "123456", "bidder@gmail.com", 1000.0);

        assertThrows(IllegalArgumentException.class, () ->
                bidder.requestDeposit(-100.0)
        );
    }

    @Test
    void adminCannotApproveSameRequestTwice() {
        Bidder bidder = new Bidder("bidder1", "123456", "bidder@gmail.com", 1000.0);
        Admin admin = new Admin("admin1", "123456", "admin@gmail.com");

        DepositRequest request = bidder.requestDeposit(500.0);
        admin.approveDeposit(request);

        assertThrows(IllegalStateException.class, () ->
                admin.approveDeposit(request)
        );
    }

    @Test
    void adminCannotRejectApprovedRequest() {
        Bidder bidder = new Bidder("bidder1", "123456", "bidder@gmail.com", 1000.0);
        Admin admin = new Admin("admin1", "123456", "admin@gmail.com");

        DepositRequest request = bidder.requestDeposit(500.0);
        admin.approveDeposit(request);

        assertThrows(IllegalStateException.class, () ->
                admin.rejectDeposit(request)
        );
    }
}