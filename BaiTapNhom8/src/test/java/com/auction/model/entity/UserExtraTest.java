package com.auction.model.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UserExtraTest {

    @Test
    void bidderShouldBeCreatedSuccessfully() {
        Bidder bidder = new Bidder("bidder1", "123456", "bidder@gmail.com", 1000.0);

        assertNotNull(bidder.getId());
        assertEquals("bidder1", bidder.getUsername());
        assertEquals("bidder@gmail.com", bidder.getEmail());
        assertEquals(1000.0, bidder.getBalance());
    }

    @Test
    void sellerShouldBeCreatedSuccessfully() {
        Seller seller = new Seller("seller1", "123456", "seller@gmail.com");

        assertNotNull(seller.getId());
        assertEquals("seller1", seller.getUsername());
        assertEquals("seller@gmail.com", seller.getEmail());
        assertTrue(seller.getItems().isEmpty());
    }

    @Test
    void adminShouldBeCreatedSuccessfully() {
        Admin admin = new Admin("admin1", "123456", "admin@gmail.com");

        assertNotNull(admin.getId());
        assertEquals("admin1", admin.getUsername());
        assertEquals("admin@gmail.com", admin.getEmail());
    }

    @Test
    void usernameCannotBeBlank() {
        assertThrows(IllegalArgumentException.class, () ->
                new Bidder("", "123456", "bidder@gmail.com", 1000.0)
        );
    }

    @Test
    void passwordMustHaveAtLeastSixCharacters() {
        assertThrows(IllegalArgumentException.class, () ->
                new Bidder("bidder1", "123", "bidder@gmail.com", 1000.0)
        );
    }

    @Test
    void emailMustBeValid() {
        assertThrows(IllegalArgumentException.class, () ->
                new Bidder("bidder1", "123456", "wrong-email", 1000.0)
        );
    }

    @Test
    void bidderBalanceCannotBeNegative() {
        assertThrows(IllegalArgumentException.class, () ->
                new Bidder("bidder1", "123456", "bidder@gmail.com", -1.0)
        );
    }

    @Test
    void bidderDepositShouldIncreaseBalance() {
        Bidder bidder = new Bidder("bidder1", "123456", "bidder@gmail.com", 1000.0);

        bidder.deposit(500.0);

        assertEquals(1500.0, bidder.getBalance());
    }

    @Test
    void bidderDepositCannotBeZeroOrNegative() {
        Bidder bidder = new Bidder("bidder1", "123456", "bidder@gmail.com", 1000.0);

        assertThrows(IllegalArgumentException.class, () ->
                bidder.deposit(0)
        );

        assertThrows(IllegalArgumentException.class, () ->
                bidder.deposit(-100.0)
        );
    }
}