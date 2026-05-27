package com.auction.model.entity;

import com.auction.model.enums.ItemType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ItemTest {

    private Art art;
    private Electronics electronics;
    private Vehicle vehicle;

    @BeforeEach
    void setUp() {
        art = new Art("Tranh Van Gogh", "Bức Đêm Đầy Sao", 100000.0, "Vincent van Gogh");
        electronics = new Electronics("Laptop MacBook M3", "Máy tính xách tay", 30000.0, "Apple", 24);
        vehicle = new Vehicle("Toyota Camry 2023", "Xe ô tô", 500000.0, "Toyota", 2023);
    }

    @Test
    void testItemInitialization() {
        assertEquals("Tranh Van Gogh", art.getName());
        assertEquals("Bức Đêm Đầy Sao", art.getDescription());
        assertEquals(100000.0, art.getStartingPrice());
        assertNotNull(art.getId());
    }

    @Test
    void testItemNameValidation() {
        assertThrows(IllegalArgumentException.class, () ->
            new Art("", "Description", 1000.0, "Artist"),
            "Tên không được trống");

        assertThrows(IllegalArgumentException.class, () ->
            new Art(null, "Description", 1000.0, "Artist"),
            "Tên không được null");
    }

    @Test
    void testItemPriceValidation() {
        assertThrows(IllegalArgumentException.class, () ->
            new Art("Tranh", "Mô tả", -100.0, "Artist"),
            "Giá không được âm");

        assertThrows(IllegalArgumentException.class, () ->
            new Art("Tranh", "Mô tả", -0.01, "Artist"),
            "Giá không được âm");
    }

    @Test
    void testItemSettersValidation() {
        assertThrows(IllegalArgumentException.class, () ->
            art.setName(""),
            "Tên không được trống");

        assertThrows(IllegalArgumentException.class, () ->
            art.setStartingPrice(-50.0),
            "Giá không được âm");
    }

    @Test
    void testItemDescription() {
        art.setDescription("Mô tả mới");
        assertEquals("Mô tả mới", art.getDescription());

        art.setDescription(null);
        assertEquals("", art.getDescription());
    }

    @Test
    void testArtItem() {
        assertEquals("Vincent van Gogh", art.getArtistName());
        assertEquals("Tranh Van Gogh", art.getName());
    }

    @Test
    void testElectronicsItem() {
        assertEquals("Apple", electronics.getBrand());
        assertEquals(24, electronics.getWarrantyMonths());
        assertEquals("Laptop MacBook M3", electronics.getName());
    }

    @Test
    void testVehicleItem() {
        assertEquals("Toyota", vehicle.getBrand());
        assertEquals(2023, vehicle.getYearOfManufacture());
        assertEquals("Toyota Camry 2023", vehicle.getName());
    }

    @Test
    void testItemPriceZero() {
        // Giá 0 vẫn hợp lệ (không âm)
        Item freeItem = new Art("Quà tặng", "Miễn phí", 0.0, "Unknown");
        assertEquals(0.0, freeItem.getStartingPrice());
    }

    @Test
    void testItemPriceLarge() {
        // Test với giá rất lớn
        Item expensiveItem = new Art("Tranh", "Đắt", 999999999.99, "Artist");
        assertEquals(999999999.99, expensiveItem.getStartingPrice());
    }

    @Test
    void testItemImages() {
        assertTrue(art.getImagesBase64().isEmpty());

        art.getImagesBase64().add("image1_base64");
        assertEquals(1, art.getImagesBase64().size());

        art.setImagesBase64(null);
        assertTrue(art.getImagesBase64().isEmpty());
    }

    @Test
    void testItemPrintInfo() {
        // Chỉ đảm bảo không throw exception
        assertDoesNotThrow(() -> art.printInfo());
        assertDoesNotThrow(() -> electronics.printInfo());
        assertDoesNotThrow(() -> vehicle.printInfo());
    }

    @Test
    void testItemEquality() {
        Art art1 = new Art("Tranh", "Mô tả", 1000.0, "Artist");
        Art art2 = new Art("Tranh", "Mô tả", 1000.0, "Artist");

        // Hai item khác nhau (UUID khác)
        assertNotEquals(art1, art2);

        // Cùng item
        assertEquals(art1, art1);
    }
}

