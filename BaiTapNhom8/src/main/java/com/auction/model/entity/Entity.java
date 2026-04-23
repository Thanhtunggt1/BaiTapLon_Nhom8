package com.auction.model.entity;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Lớp cơ sở trừu tượng cho mọi đối tượng trong hệ thống.
 * Cung cấp id duy nhất (UUID) và thời điểm tạo.
 */

public abstract class Entity {

    private final String id;                // Mã định danh
    private final LocalDateTime createdAt;  // Thời gian chính xác mà một đối tượng được sinh ra

    protected Entity() {
        this.id = UUID.randomUUID().toString();

        /*
        UUID.randomUUID() kết quả VD: 5fc03087-d265-41e7-b8c6-83e29cd24f4c
        * Luôn gồm 36 ký tự theo định dạng 8-4-4-4-12, ký tự chỉ bao gồm các số từ 0-9 và các chữ cái từ a-f
        * Tỷ lệ sinh ra 2 mã UUID ngẫu nhiên giống hệt nhau là cực kỳ thấp (gần như bằng 0)
        * .toString ép ra kiểu String
        * */

        this.createdAt = LocalDateTime.now(); // Gán bằng thời gian thực
    }

    //Getters

    public String getId() {
        return id;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    //Object overrides

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Entity)) return false;
        Entity other = (Entity) o;
        return id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode(); // .hasCode chuyển String id -> int id
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{id='" + id + "'}";
        /*
        * getClass() mò ra được đối tợng thuộc kiểu gì thực sự
        * VD: Item myItem = new Electronics(.....);
        * myItem.getClass -> Class Electronics
        * getClass() trả về một kiểu dữ liệu rất đặc biệt trong Java, nó có tên chính là Class (viết hoa chữ C, thuộc gói java.lang)
        * getSimpleName()
        * */

    }
}