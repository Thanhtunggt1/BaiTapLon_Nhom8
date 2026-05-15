package com.auction.model.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ItemExtraTest {

    @Test
    void artShouldBeCreatedSuccessfully() {
        Art art = new Art("Tranh cổ", "Tranh đẹp", 100.0, "Picasso", 1990);

        assertNotNull(art.getId());
        assertEquals("Tranh cổ", art.getName());
        assertEquals("Tranh đẹp", art.getDescription());
        assertEquals(100.0, art.getStartingPrice());
    }

    @Test
    void electronicsShouldBeCreatedSuccessfully() {
        Electronics phone = new Electronics("iPhone", "Điện thoại", 500.0, "Apple", 12);

        assertNotNull(phone.getId());
        assertEquals("iPhone", phone.getName());
        assertEquals("Điện thoại", phone.getDescription());
        assertEquals(500.0, phone.getStartingPrice());
        assertEquals("Apple", phone.getBrand());
        assertEquals(12, phone.getWarrantyMonths());
    }

    @Test
    void vehicleShouldBeCreatedSuccessfully() {
        Vehicle car = new Vehicle("Toyota Camry", "Xe cũ", 10000.0, 50000.0, "30A-12345");

        assertNotNull(car.getId());
        assertEquals("Toyota Camry", car.getName());
        assertEquals("Xe cũ", car.getDescription());
        assertEquals(10000.0, car.getStartingPrice());
    }

    @Test
    void itemNameCannotBeBlank() {
        assertThrows(IllegalArgumentException.class, () ->
                new Electronics("", "Điện thoại", 500.0, "Apple", 12)
        );
    }

    @Test
    void startingPriceCannotBeNegative() {
        assertThrows(IllegalArgumentException.class, () ->
                new Electronics("iPhone", "Điện thoại", -1.0, "Apple", 12)
        );
    }

    @Test
    void electronicsBrandCannotBeBlank() {
        assertThrows(IllegalArgumentException.class, () ->
                new Electronics("iPhone", "Điện thoại", 500.0, "", 12)
        );
    }

    @Test
    void electronicsWarrantyCannotBeNegative() {
        assertThrows(IllegalArgumentException.class, () ->
                new Electronics("iPhone", "Điện thoại", 500.0, "Apple", -1)
        );
    }

    @Test
    void artArtistNameCannotBeBlank() {
        assertThrows(IllegalArgumentException.class, () ->
                new Art("Tranh cổ", "Tranh đẹp", 100.0, "", 1990)
        );
    }

    @Test
    void artCreationYearCannotBeNegative() {
        assertThrows(IllegalArgumentException.class, () ->
                new Art("Tranh cổ", "Tranh đẹp", 100.0, "Picasso", -1)
        );
    }

    @Test
    void vehicleMileageCannotBeNegative() {
        assertThrows(IllegalArgumentException.class, () ->
                new Vehicle("Toyota", "Xe cũ", 10000.0, -1.0, "30A-12345")
        );
    }

    @Test
    void vehicleLicensePlateCannotBeBlank() {
        assertThrows(IllegalArgumentException.class, () ->
                new Vehicle("Toyota", "Xe cũ", 10000.0, 50000.0, "")
        );
    }

    @Test
    void setNameShouldUpdateItemName() {
        Electronics item = new Electronics("iPhone", "Điện thoại", 500.0, "Apple", 12);

        item.setName("Samsung");

        assertEquals("Samsung", item.getName());
    }

    @Test
    void setDescriptionShouldUpdateDescription() {
        Electronics item = new Electronics("iPhone", "Điện thoại", 500.0, "Apple", 12);

        item.setDescription("Máy còn mới");

        assertEquals("Máy còn mới", item.getDescription());
    }

    @Test
    void setStartingPriceShouldUpdateStartingPrice() {
        Electronics item = new Electronics("iPhone", "Điện thoại", 500.0, "Apple", 12);

        item.setStartingPrice(700.0);

        assertEquals(700.0, item.getStartingPrice());
    }
}