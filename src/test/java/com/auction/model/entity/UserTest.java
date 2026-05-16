package com.auction.model.entity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UserTest {

    private Bidder bidder;
    private Seller seller;
    private Admin admin;

    @BeforeEach
    void setUp() {
        bidder = new Bidder("bidder", "pass123", "bidder@test.com", 5000.0);
        seller = new Seller("seller", "pass123", "seller@test.com");
        admin = new Admin("admin", "pass123", "admin@test.com");
    }

    @Test
    void testBidderCreation() {
        assertEquals("bidder", bidder.getUsername());
        assertEquals("bidder@test.com", bidder.getEmail());
        assertEquals(5000.0, bidder.getBalance());
    }

    @Test
    void testSellerCreation() {
        assertEquals("seller", seller.getUsername());
        assertEquals("seller@test.com", seller.getEmail());
    }

    @Test
    void testAdminCreation() {
        assertEquals("admin", admin.getUsername());
        assertEquals("admin@test.com", admin.getEmail());
    }

    @Test
    void testUserIdIsUnique() {
        Bidder bidder2 = new Bidder("bidder2", "pass", "b2@test.com", 1000.0);
        assertNotEquals(bidder.getId(), bidder2.getId());
    }

    @Test
    void testUserEqualsById() {
        Bidder sameBidder = new Bidder("different", "pass", "diff@test.com", 1000.0);
        assertNotEquals(bidder, sameBidder);
    }

    @Test
    void testUserNullValidation() {
        assertThrows(IllegalArgumentException.class, () ->
            new Bidder(null, "pass", "email@test.com", 1000.0),
            "Username không được null");

        assertThrows(IllegalArgumentException.class, () ->
            new Bidder("user", null, "email@test.com", 1000.0),
            "Password không được null");

        assertThrows(IllegalArgumentException.class, () ->
            new Bidder("user", "pass", null, 1000.0),
            "Email không được null");
    }

    @Test
    void testUserBlankValidation() {
        assertThrows(IllegalArgumentException.class, () ->
            new Bidder("", "pass", "email@test.com", 1000.0),
            "Username không được trống");

        assertThrows(IllegalArgumentException.class, () ->
            new Bidder("user", "", "email@test.com", 1000.0),
            "Password không được trống");

        assertThrows(IllegalArgumentException.class, () ->
            new Bidder("user", "pass", "", 1000.0),
            "Email không được trống");
    }

    @Test
    void testUserHashCodeConsistency() {
        int hash1 = bidder.hashCode();
        int hash2 = bidder.hashCode();
        assertEquals(hash1, hash2, "HashCode phải consistent");
    }

    @Test
    void testMultipleBiddersWithDifferentBalances() {
        Bidder bidder1 = new Bidder("b1", "pass", "b1@test.com", 1000.0);
        Bidder bidder2 = new Bidder("b2", "pass", "b2@test.com", 5000.0);
        Bidder bidder3 = new Bidder("b3", "pass", "b3@test.com", 10000.0);

        assertEquals(1000.0, bidder1.getBalance());
        assertEquals(5000.0, bidder2.getBalance());
        assertEquals(10000.0, bidder3.getBalance());
    }

    @Test
    void testSellerPrintInfo() {
        assertDoesNotThrow(() -> seller.printInfo());
    }

    @Test
    void testAdminPrintInfo() {
        assertDoesNotThrow(() -> admin.printInfo());
    }

    @Test
    void testBidderPrintInfo() {
        assertDoesNotThrow(() -> bidder.printInfo());
    }

    @Test
    void testUserEmailValidation() {
        // Note: Email validation depends on actual implementation
        // These tests assume basic null/blank checks
        Bidder validEmailUser = new Bidder("user", "pass", "user@example.com", 1000.0);
        assertEquals("user@example.com", validEmailUser.getEmail());
    }

    @Test
    void testBidderZeroBalance() {
        Bidder zeroBidder = new Bidder("zero", "pass", "zero@test.com", 0.0);
        assertEquals(0.0, zeroBidder.getBalance());
    }

    @Test
    void testBidderLargeBalance() {
        Bidder richBidder = new Bidder("rich", "pass", "rich@test.com", 1000000.0);
        assertEquals(1000000.0, richBidder.getBalance());
    }

    @Test
    void testUserUsernameLength() {
        String longUsername = "a".repeat(50);
        Bidder longUser = new Bidder(longUsername, "pass", "email@test.com", 1000.0);
        assertEquals(longUsername, longUser.getUsername());
    }

    @Test
    void testUserPasswordLength() {
        String longPassword = "a".repeat(100);
        Bidder userLongPass = new Bidder("user", longPassword, "email@test.com", 1000.0);
        assertEquals("user", userLongPass.getUsername()); // Just verify it accepts long password
    }

    @Test
    void testAllUserTypesCreation() {
        Bidder bidder = new Bidder("b", "p", "b@test.com", 100.0);
        Seller seller = new Seller("s", "p", "s@test.com");
        Admin admin = new Admin("a", "p", "a@test.com");

        assertNotNull(bidder.getId());
        assertNotNull(seller.getId());
        assertNotNull(admin.getId());
    }
}

