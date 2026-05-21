package com.auction.pattern.factory;

import com.auction.model.entity.Art;
import com.auction.model.entity.Electronics;
import com.auction.model.entity.Item;
import com.auction.model.enums.ItemType;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ItemFactoryTest {

    @Test
    void testSingletonInstance() {
        ItemFactory instance1 = ItemFactory.getInstance();
        ItemFactory instance2 = ItemFactory.getInstance();
        assertSame(instance1, instance2, "Cả hai tham chiếu phải trỏ về cùng 1 vùng nhớ (Singleton)");
    }

    @Test
    void testCreateElectronics_Success() {
        Map<String, Object> params = new HashMap<>();
        params.put("brand", "Samsung");
        params.put("warrantyMonths", 24);

        Item item = ItemFactory.getInstance().createItem(
                ItemType.ELECTRONICS, "S24 Ultra", "Phone", 25000.0, params);

        assertTrue(item instanceof Electronics);
        Electronics electronics = (Electronics) item;
        assertEquals("Samsung", electronics.getBrand());
        assertEquals(24, electronics.getWarrantyMonths());
    }

    @Test
    void testCreateArt_MissingParam_ThrowsException() {
        Map<String, Object> params = new HashMap<>();
        params.put("artistName", "Picasso"); // Cố tình thiếu 'creationYear'

        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            ItemFactory.getInstance().createItem(
                    ItemType.ART, "Bức tranh", "Nghệ thuật", 1000.0, params);
        });

        assertTrue(exception.getMessage().contains("Thiếu tham số bắt buộc"));
    }
}