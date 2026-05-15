package com.auction.model.entity;

import com.auction.model.enums.ItemType;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class SellerExtraTest {

    private Seller createSeller() {
        return new Seller("seller1", "123456", "seller@gmail.com");
    }

    @Test
    void sellerCanCreateElectronicsItem() {
        Seller seller = createSeller();

        Item item = seller.createItem(
                "iPhone",
                "Điện thoại cũ",
                500.0,
                ItemType.ELECTRONICS,
                Map.of(
                        "brand", "Apple",
                        "warrantyMonths", 12
                )
        );

        assertNotNull(item);
        assertInstanceOf(Electronics.class, item);
        assertEquals("iPhone", item.getName());
        assertEquals(1, seller.getItems().size());
    }

    @Test
    void sellerCanCreateArtItem() {
        Seller seller = createSeller();

        Item item = seller.createItem(
                "Tranh cổ",
                "Tranh đẹp",
                1000.0,
                ItemType.ART,
                Map.of(
                        "artistName", "Picasso",
                        "creationYear", 1990
                )
        );

        assertNotNull(item);
        assertInstanceOf(Art.class, item);
        assertEquals("Tranh cổ", item.getName());
        assertEquals(1, seller.getItems().size());
    }

    @Test
    void sellerCanCreateVehicleItem() {
        Seller seller = createSeller();

        Item item = seller.createItem(
                "Toyota",
                "Xe cũ",
                10000.0,
                ItemType.VEHICLE,
                Map.of(
                        "mileage", 50000.0,
                        "licensePlate", "30A-12345"
                )
        );

        assertNotNull(item);
        assertInstanceOf(Vehicle.class, item);
        assertEquals("Toyota", item.getName());
        assertEquals(1, seller.getItems().size());
    }

    @Test
    void sellerCanUpdateOwnItem() {
        Seller seller = createSeller();

        Item item = seller.createItem(
                "iPhone",
                "Điện thoại cũ",
                500.0,
                ItemType.ELECTRONICS,
                Map.of(
                        "brand", "Apple",
                        "warrantyMonths", 12
                )
        );

        boolean result = seller.updateItem(item, "Samsung", "Máy mới", 700.0);

        assertTrue(result);
        assertEquals("Samsung", item.getName());
        assertEquals("Máy mới", item.getDescription());
        assertEquals(700.0, item.getStartingPrice());
    }

    @Test
    void sellerCannotUpdateItemNotOwnedBySeller() {
        Seller seller = createSeller();
        Item foreignItem = new Electronics("Laptop", "Máy tính", 1000.0, "Dell", 24);

        assertThrows(IllegalArgumentException.class, () ->
                seller.updateItem(foreignItem, "New name", "New desc", 1200.0)
        );
    }

    @Test
    void sellerCanDeleteOwnItem() {
        Seller seller = createSeller();

        Item item = seller.createItem(
                "iPhone",
                "Điện thoại cũ",
                500.0,
                ItemType.ELECTRONICS,
                Map.of(
                        "brand", "Apple",
                        "warrantyMonths", 12
                )
        );

        boolean result = seller.deleteItem(item);

        assertTrue(result);
        assertTrue(seller.getItems().isEmpty());
    }

    @Test
    void sellerCannotDeleteItemNotOwnedBySeller() {
        Seller seller = createSeller();
        Item foreignItem = new Electronics("Laptop", "Máy tính", 1000.0, "Dell", 24);

        boolean result = seller.deleteItem(foreignItem);

        assertFalse(result);
    }

    @Test
    void sellerCanCreateAuctionForOwnItem() {
        Seller seller = createSeller();

        Item item = seller.createItem(
                "iPhone",
                "Điện thoại cũ",
                500.0,
                ItemType.ELECTRONICS,
                Map.of(
                        "brand", "Apple",
                        "warrantyMonths", 12
                )
        );

        Auction auction = seller.createAuction(
                item,
                LocalDateTime.now().minusMinutes(1),
                LocalDateTime.now().plusMinutes(10)
        );

        assertNotNull(auction);
        assertEquals(item, auction.getItem());
        assertEquals(seller, auction.getSeller());
    }

    @Test
    void sellerCannotCreateAuctionForItemNotOwnedBySeller() {
        Seller seller = createSeller();
        Item foreignItem = new Electronics("Laptop", "Máy tính", 1000.0, "Dell", 24);

        assertThrows(IllegalArgumentException.class, () ->
                seller.createAuction(
                        foreignItem,
                        LocalDateTime.now().minusMinutes(1),
                        LocalDateTime.now().plusMinutes(10)
                )
        );
    }

    @Test
    void sellerCannotCreateAuctionWithInvalidTime() {
        Seller seller = createSeller();

        Item item = seller.createItem(
                "iPhone",
                "Điện thoại cũ",
                500.0,
                ItemType.ELECTRONICS,
                Map.of(
                        "brand", "Apple",
                        "warrantyMonths", 12
                )
        );

        assertThrows(IllegalArgumentException.class, () ->
                seller.createAuction(
                        item,
                        LocalDateTime.now().plusMinutes(10),
                        LocalDateTime.now()
                )
        );
    }
}