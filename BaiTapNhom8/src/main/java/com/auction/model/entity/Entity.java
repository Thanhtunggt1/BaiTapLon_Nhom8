package com.auction.model.entity;

import java.time.LocalDateTime;
import java.util.UUID;

public abstract class Entity {
    private String id; // XÓA CHỮ FINAL Ở ĐÂY
    private final LocalDateTime createdAt;

    protected Entity() {
        this.id = UUID.randomUUID().toString();
        this.createdAt = LocalDateTime.now();
    }

    public String getId() { return id; }

    // THÊM HÀM NÀY VÀO
    public void setId(String id) { this.id = id; }

    public LocalDateTime getCreatedAt() { return createdAt; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Entity)) return false;
        Entity other = (Entity) o;
        return id.equals(other.id);
    }

    @Override
    public int hashCode() { return id.hashCode(); }
}