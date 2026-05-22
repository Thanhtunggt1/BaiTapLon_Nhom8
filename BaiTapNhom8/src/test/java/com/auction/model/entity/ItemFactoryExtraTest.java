package com.auction.model.entity;

import com.auction.model.enums.ItemType;
import com.auction.pattern.factory.ItemFactory;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ItemFactoryExtraTest {

    private final ItemFactory factory = ItemFactory.getInstance();

    @Test
    void shouldCreateElectronicsItem() {
        Item item = factory.createItem(
                ItemType.ELECTRONICS,
                "iPhone",
                "Điện thoại cũ",
                500.0,
                Map.of("brand", "Apple", "warrantyMonths", 12)
        );

        assertInstanceOf(Electronics.class, item);
        assertEquals("iPhone", item.getName());
        assertEquals("Điện thoại cũ", item.getDescription());
        assertEquals(500.0, item.getStartingPrice());
    }

    @Test
    void shouldCreateArtItem() {
        Item item = factory.createItem(
                ItemType.ART,
                "Tranh cổ",
                "Tranh đẹp",
                1000.0,
                Map.of("artistName", "Picasso", "creationYear", 1990)
        );

        assertInstanceOf(Art.class, item);
        assertEquals("Tranh cổ", item.getName());
        assertEquals("Tranh đẹp", item.getDescription());
        assertEquals(1000.0, item.getStartingPrice());
    }

    @Test
    void shouldCreateVehicleItem() {
        Item item = factory.createItem(
                ItemType.VEHICLE,
                "Toyota",
                "Xe cũ",
                10000.0,
                Map.of("mileage", 50000.0, "licensePlate", "30A-12345")
        );

        assertInstanceOf(Vehicle.class, item);
        assertEquals("Toyota", item.getName());
        assertEquals("Xe cũ", item.getDescription());
        assertEquals(10000.0, item.getStartingPrice());
    }

    @Test
    void electronicsShouldHaveCorrectExtraFields() {
        Item item = factory.createItem(
                ItemType.ELECTRONICS,
                "Laptop",
                "Máy tính",
                1000.0,
                Map.of("brand", "Dell", "warrantyMonths", 24)
        );

        Electronics electronics = (Electronics) item;

        assertEquals("Dell", electronics.getBrand());
        assertEquals(24, electronics.getWarrantyMonths());
    }

    @Test
    void createItemWithNullTypeShouldThrowException() {
        assertThrows(Exception.class, () ->
                factory.createItem(
                        null,
                        "Item",
                        "Desc",
                        100.0,
                        Map.of()
                )
        );
    }

    @Test
    void createElectronicsWithoutBrandShouldThrowException() {
        assertThrows(Exception.class, () ->
                factory.createItem(
                        ItemType.ELECTRONICS,
                        "Phone",
                        "Desc",
                        100.0,
                        Map.of("warrantyMonths", 12)
                )
        );
    }

    @Test
    void createArtWithoutArtistNameShouldThrowException() {
        assertThrows(Exception.class, () ->
                factory.createItem(
                        ItemType.ART,
                        "Painting",
                        "Desc",
                        100.0,
                        Map.of("creationYear", 1990)
                )
        );
    }

    @Test
    void createVehicleWithoutLicensePlateShouldThrowException() {
        assertThrows(Exception.class, () ->
                factory.createItem(
                        ItemType.VEHICLE,
                        "Car",
                        "Desc",
                        100.0,
                        Map.of("mileage", 10000.0)
                )
        );
    }

    @Test
    void createItemWithBlankNameShouldThrowException() {
        assertThrows(Exception.class, () ->
                factory.createItem(
                        ItemType.ELECTRONICS,
                        "",
                        "Desc",
                        100.0,
                        Map.of("brand", "Apple", "warrantyMonths", 12)
                )
        );
    }
}