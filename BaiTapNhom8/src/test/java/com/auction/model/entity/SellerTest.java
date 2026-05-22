package com.auction.model.entity;

import com.auction.model.enums.ItemType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

public class SellerTest {

    private Seller seller;

    @BeforeEach
    public void setUp() {
        seller = new Seller("tung_seller", "pass123", "tung_seller@test.com");
    }

    @Test
    public void testCreateItem() {
        Map<String, Object> params = new HashMap<>();
        params.put("artistName", "Van Gogh");
        params.put("creationYear", 1889);

        Item item = seller.createItem("Starry Night", "Painting", 50000.0, ItemType.ART, params);

        assertNotNull(item);
        assertEquals(1, seller.getItems().size());
        assertEquals("Starry Night", item.getName());
    }

    @Test
    public void testUpdateItemSuccess() {
        Map<String, Object> params = new HashMap<>();
        params.put("mileage", 100.0);
        params.put("licensePlate", "29A-12345");

        Item item = seller.createItem("Car", "Old car", 20000.0, ItemType.VEHICLE, params);

        assertTrue(seller.updateItem(item, "New Car", "Updated desc", 25000.0));
        assertEquals("New Car", item.getName());
        assertEquals("Updated desc", item.getDescription());
        assertEquals(25000.0, item.getStartingPrice());
    }

    @Test
    public void testDeleteItemSuccess() {
        Map<String, Object> params = new HashMap<>();
        params.put("brand", "Sony");
        params.put("warrantyMonths", 12);

        Item item = seller.createItem("TV", "4K", 10000.0, ItemType.ELECTRONICS, params);
        assertTrue(seller.deleteItem(item));
        assertEquals(0, seller.getItems().size());
    }

    @Test
    public void testCreateAuctionSuccess() {
        Map<String, Object> params = new HashMap<>();
        params.put("brand", "Sony");
        params.put("warrantyMonths", 12);

        Item item = seller.createItem("TV", "4K", 10000.0, ItemType.ELECTRONICS, params);
        LocalDateTime now = LocalDateTime.now();

        Auction auction = seller.createAuction(item, now, now.plusDays(1));
        assertNotNull(auction);
        assertEquals(item, auction.getItem());
    }
}