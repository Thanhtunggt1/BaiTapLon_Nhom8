package com.auction.model.entity;

public abstract class Item extends Entity {

    private String name;
    private String description;
    private double startingPrice;

    protected Item(String name, String description, double startingPrice) {
        super();
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Tên sản phẩm không được để trống.");
        }
        if (startingPrice < 0) {
            throw new IllegalArgumentException("Giá khởi điểm không được âm.");
        }
        this.name = name;


        this.description = description != null ? description : "";
        this.startingPrice = startingPrice;
    }

    public void printInfo() {
        System.out.printf("[%s] id=%s | Tên: %s | Giá khởi điểm: %.2f%n  Mô tả: %s%n",
                getClass().getSimpleName(), getId(), name, startingPrice, description);
    }


    public String getName() { return name; }

    public void setName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Tên sản phẩm không được để trống.");
        }
        this.name = name;
    }

    public String getDescription() { return description; }

    public void setDescription(String description) {
        this.description = description != null ? description : "";
    }

    public double getStartingPrice() { return startingPrice; }

    public void setStartingPrice(double startingPrice) {
        if (startingPrice < 0) {
            throw new IllegalArgumentException("Giá khởi điểm không được âm.");
        }
        this.startingPrice = startingPrice;
    }
}