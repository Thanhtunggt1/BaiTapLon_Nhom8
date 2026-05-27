package com.auction.gui;

import com.auction.model.entity.Bidder;
import com.auction.model.entity.Seller;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class UserStoreTest {

    @AfterEach
    void tearDown() {
        // Xóa tất cả users sau mỗi test để không ảnh hưởng lẫn nhau
        UserStore.clearAllUsers();
    }

    @Test
    void testAddUserAndFindByUsername() {
        Seller seller = new Seller("seller1", "password123", "seller@example.com");
        UserStore.addUser(seller);

        com.auction.model.entity.User found = UserStore.findByUsername("seller1");
        assertNotNull(found);
        assertEquals("seller1", found.getUsername());
        assertTrue(found instanceof Seller);
    }

    @Test
    void testUsernameExists() {
        Bidder bidder = new Bidder("bidder1", "password123", "bidder@example.com", 1000.0);
        UserStore.addUser(bidder);

        assertTrue(UserStore.usernameExists("bidder1"));
        assertFalse(UserStore.usernameExists("nonexistent"));
    }

    @Test
    void testGetAllUsers() {
        Seller seller = new Seller("seller2", "password123", "seller2@example.com");
        Bidder bidder = new Bidder("bidder2", "password123", "bidder2@example.com", 2000.0);
        UserStore.addUser(seller);
        UserStore.addUser(bidder);

        List<com.auction.model.entity.User> allUsers = UserStore.getAllUsers();
        assertEquals(2, allUsers.size());
        assertTrue(allUsers.contains(seller));
        assertTrue(allUsers.contains(bidder));
    }

    @Test
    void testGetAllBidders() {
        Bidder bidder1 = new Bidder("bidder3", "password123", "bidder3@example.com", 1500.0);
        Bidder bidder2 = new Bidder("bidder4", "password123", "bidder4@example.com", 2500.0);
        Seller seller = new Seller("seller3", "password123", "seller3@example.com");
        UserStore.addUser(bidder1);
        UserStore.addUser(bidder2);
        UserStore.addUser(seller);

        List<Bidder> bidders = UserStore.getAllBidders();
        assertEquals(2, bidders.size());
        assertTrue(bidders.contains(bidder1));
        assertTrue(bidders.contains(bidder2));
    }

    @Test
    void testGetAllSellers() {
        Seller seller1 = new Seller("seller4", "password123", "seller4@example.com");
        Seller seller2 = new Seller("seller5", "password123", "seller5@example.com");
        Bidder bidder = new Bidder("bidder5", "password123", "bidder5@example.com", 3000.0);
        UserStore.addUser(seller1);
        UserStore.addUser(seller2);
        UserStore.addUser(bidder);

        List<Seller> sellers = UserStore.getAllSellers();
        assertEquals(2, sellers.size());
        assertTrue(sellers.contains(seller1));
        assertTrue(sellers.contains(seller2));
    }
}
