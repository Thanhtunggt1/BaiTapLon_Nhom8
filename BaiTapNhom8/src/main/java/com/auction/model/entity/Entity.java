package com.auction.model.entity;

import java.util.UUID;

public abstract class Entity {
    private String id;

    protected Entity() {
        this.id = UUID.randomUUID().toString();
    }

    public String getId() { return id; }

    public void setId(String id) { this.id = id; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Entity other)) return false;
        return id.equals(other.id);
    }

    @Override
    public int hashCode() { return id.hashCode(); }
}