package com.auction.pattern.factory;

import com.auction.model.entity.Art;
import com.auction.model.entity.Electronics;
import com.auction.model.entity.Item;
import com.auction.model.entity.Vehicle;
import com.auction.model.enums.ItemType;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class ItemFactoryTest {

    @Test
    public void testCreateElectronicsSuccess() {
        Map<String, Object> params = new HashMap<>();
        params.put("brand", "Samsung");
        params.put("warrantyMonths", 24);

        Item item = ItemFactory.getInstance().createItem(ItemType.ELECTRONICS, "Smart TV", "4K UHD", 15000.0, params);

        assertTrue(item instanceof Electronics);
        assertEquals("Samsung", ((Electronics) item).getBrand());
        assertEquals(24, ((Electronics) item).getWarrantyMonths());
    }

    @Test
    public void testCreateArtSuccess() {
        Map<String, Object> params = new HashMap<>();
        params.put("artistName", "Picasso");
        params.put("creationYear", 1937);

        Item item = ItemFactory.getInstance().createItem(ItemType.ART, "Guernica", "Oil painting", 500000.0, params);

        assertTrue(item instanceof Art);
        assertEquals("Guernica", item.getName());
    }

    @Test
    public void testCreateVehicleSuccess() {
        Map<String, Object> params = new HashMap<>();
        params.put("mileage", 15000.5);
        params.put("licensePlate", "30A-99999");

        Item item = ItemFactory.getInstance().createItem(ItemType.VEHICLE, "Honda Civic", "Sedan", 700000.0, params);

        assertTrue(item instanceof Vehicle);
    }

    @Test
    public void testCreateItemMissingParamsThrowsException() {
        Map<String, Object> params = new HashMap<>();
        params.put("brand", "Apple");

        assertThrows(IllegalArgumentException.class, () ->
                ItemFactory.getInstance().createItem(ItemType.ELECTRONICS, "MacBook", "M2", 20000.0, params)
        );
    }

    @Test
    public void testCreateItemInvalidParamTypeThrowsException() {
        Map<String, Object> params = new HashMap<>();
        params.put("brand", "Apple");
        params.put("warrantyMonths", "Twelve");

        assertThrows(IllegalArgumentException.class, () ->
                ItemFactory.getInstance().createItem(ItemType.ELECTRONICS, "MacBook", "M2", 20000.0, params)
        );
    }
}