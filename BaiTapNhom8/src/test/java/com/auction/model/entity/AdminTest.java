package com.auction.model.entity;

import com.auction.model.enums.AuctionStatus;
import com.auction.model.enums.ItemType;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class AdminTest {

    @Test
    public void testResolveDisputeCancelsAuction() {
        Admin admin = new Admin("superadmin", "pass123", "admin@system.com");
        Seller seller = new Seller("bad_seller", "pass123", "bad@test.com");

        Map<String, Object> params = new HashMap<>();
        params.put("brand", "FakeBrand");
        params.put("warrantyMonths", 0);
        Item item = seller.createItem("Fake Item", "Fake Desc", 100.0, ItemType.ELECTRONICS, params);

        Auction auction = new Auction(item, seller, LocalDateTime.now(), LocalDateTime.now().plusDays(1));

        admin.resolveDispute(auction, "Hàng giả mạo");

        assertEquals(AuctionStatus.CANCELED, auction.getStatus());
    }

    @Test
    public void testResolveDisputeFailsIfPaid() {
        Admin admin = new Admin("superadmin", "pass123", "admin@system.com");
        Seller seller = new Seller("seller1", "pass123", "s@test.com");

        Map<String, Object> params = new HashMap<>();
        params.put("artistName", "Artist");
        params.put("creationYear", 2020);
        Item item = seller.createItem("Art", "Desc", 100.0, ItemType.ART, params);

        Auction auction = new Auction(item, seller, LocalDateTime.now(), LocalDateTime.now().plusDays(1));

        try {
            java.lang.reflect.Field statusField = Auction.class.getDeclaredField("status");
            statusField.setAccessible(true);
            statusField.set(auction, AuctionStatus.PAID);
        } catch (Exception ignored) {}

        admin.resolveDispute(auction, "Test");

        assertEquals(AuctionStatus.PAID, auction.getStatus());
    }
}