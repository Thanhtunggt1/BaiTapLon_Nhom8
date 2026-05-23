package com.auction.model.entity;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class ItemSubclassTest {

    @Test
    public void testElectronicsInvalidWarranty() {
        assertThrows(IllegalArgumentException.class, () ->
                new Electronics("Phone", "Desc", 100.0, "Apple", -5)
        );
    }

    @Test
    public void testElectronicsInvalidBrand() {
        assertThrows(IllegalArgumentException.class, () ->
                new Electronics("Phone", "Desc", 100.0, "", 12)
        );
    }

    @Test
    public void testArtInvalidCreationYear() {
        assertThrows(IllegalArgumentException.class, () ->
                new Art("Mona Lisa", "Painting", 1000.0, "Da Vinci", -1500)
        );
    }

    @Test
    public void testVehicleInvalidMileage() {
        assertThrows(IllegalArgumentException.class, () ->
                new Vehicle("Car", "Fast", 500.0, -100.0, "29A")
        );
    }

    @Test
    public void testVehicleInvalidLicensePlate() {
        assertThrows(IllegalArgumentException.class, () ->
                new Vehicle("Car", "Fast", 500.0, 100.0, "   ")
        );
    }
}