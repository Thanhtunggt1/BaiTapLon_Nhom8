package com.auction.model.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EntityExtraTest {

    @Test
    void everyEntityShouldHaveIdAfterCreated() {
        Admin admin = new Admin("admin1", "123456", "admin@gmail.com");
        Seller seller = new Seller("seller1", "123456", "seller@gmail.com");
        Bidder bidder = new Bidder("bidder1", "123456", "bidder@gmail.com", 1000.0);

        assertNotNull(admin.getId());
        assertNotNull(seller.getId());
        assertNotNull(bidder.getId());
    }

    @Test
    void differentEntitiesShouldHaveDifferentIds() {
        Admin admin = new Admin("admin1", "123456", "admin@gmail.com");
        Seller seller = new Seller("seller1", "123456", "seller@gmail.com");

        assertNotEquals(admin.getId(), seller.getId());
    }

    @Test
    void setIdShouldChangeEntityId() {
        Admin admin = new Admin("admin1", "123456", "admin@gmail.com");

        admin.setId("ADMIN-001");

        assertEquals("ADMIN-001", admin.getId());
    }

    @Test
    void twoEntitiesWithSameIdShouldBeEqual() {
        Admin admin1 = new Admin("admin1", "123456", "admin1@gmail.com");
        Admin admin2 = new Admin("admin2", "123456", "admin2@gmail.com");

        admin1.setId("SAME-ID");
        admin2.setId("SAME-ID");

        assertEquals(admin1, admin2);
    }

    @Test
    void twoEntitiesWithDifferentIdsShouldNotBeEqual() {
        Admin admin1 = new Admin("admin1", "123456", "admin1@gmail.com");
        Admin admin2 = new Admin("admin2", "123456", "admin2@gmail.com");

        admin1.setId("ID-1");
        admin2.setId("ID-2");

        assertNotEquals(admin1, admin2);
    }

    @Test
    void sameEntityShouldBeEqualToItself() {
        Admin admin = new Admin("admin1", "123456", "admin@gmail.com");

        assertEquals(admin, admin);
    }

    @Test
    void entityShouldNotEqualNull() {
        Admin admin = new Admin("admin1", "123456", "admin@gmail.com");

        assertNotEquals(null, admin);
    }

    @Test
    void entityShouldNotEqualNormalObject() {
        Admin admin = new Admin("admin1", "123456", "admin@gmail.com");

        assertNotEquals("ADMIN-001", admin);
    }

    @Test
    void sameIdEntitiesShouldHaveSameHashCode() {
        Admin admin1 = new Admin("admin1", "123456", "admin1@gmail.com");
        Admin admin2 = new Admin("admin2", "123456", "admin2@gmail.com");

        admin1.setId("SAME-ID");
        admin2.setId("SAME-ID");

        assertEquals(admin1.hashCode(), admin2.hashCode());
    }

    @Test
    void differentIdEntitiesShouldHaveDifferentHashCodeUsually() {
        Admin admin1 = new Admin("admin1", "123456", "admin1@gmail.com");
        Admin admin2 = new Admin("admin2", "123456", "admin2@gmail.com");

        admin1.setId("ID-1");
        admin2.setId("ID-2");

        assertNotEquals(admin1.hashCode(), admin2.hashCode());
    }
}
