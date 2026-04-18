package com.auction.model.entity;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Lớp cơ sở trừu tượng cho mọi đối tượng trong hệ thống.
 * Cung cấp id duy nhất (UUID) và thời điểm tạo.
 */
public abstract class Entity {

    private final String id;
    private final LocalDateTime createdAt;

    protected Entity() {
        this.id = UUID.randomUUID().toString();
        this.createdAt = LocalDateTime.now();
    }

    // ── Getters ──────────────────────────────────────────────────────────────

    public String getId() {
        return id;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    // ── Object overrides ─────────────────────────────────────────────────────

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Entity)) return false;
        Entity other = (Entity) o;
        return id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{id='" + id + "'}";
    }
}