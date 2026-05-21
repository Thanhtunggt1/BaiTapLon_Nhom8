package com.auction.model.entity;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class EntityTest {

    // Tạo một class nặc danh kế thừa Entity để test vì Entity là abstract class
    private static class TestEntity extends Entity {}

    @Test
    void testUUIDGeneration_IsUnique() {
        TestEntity entity1 = new TestEntity();
        TestEntity entity2 = new TestEntity();

        assertNotNull(entity1.getId(), "ID không được null");
        assertNotEquals(entity1.getId(), entity2.getId(), "Hai đối tượng sinh ra phải có UUID hoàn toàn khác nhau");
    }

    @Test
    void testEqualsAndHashCode() {
        TestEntity entity1 = new TestEntity();
        TestEntity entity2 = new TestEntity();

        // 1. So sánh với chính nó
        assertEquals(entity1, entity1, "So sánh với chính nó phải là true");
        assertEquals(entity1.hashCode(), entity1.hashCode());

        // 2. So sánh 2 đối tượng khác nhau
        assertNotEquals(entity1, entity2, "Hai đối tượng khác UUID phải trả về false");
        assertNotEquals(entity1.hashCode(), entity2.hashCode(), "Mã băm (hashCode) của 2 UUID khác nhau phải khác nhau");

        // 3. So sánh với null và kiểu dữ liệu khác
        assertNotEquals(entity1, null);
        assertNotEquals(entity1, "Một chuỗi String nào đó");
    }
}