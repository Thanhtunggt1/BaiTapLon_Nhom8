package com.auction.model.entity;

public abstract class Item extends Entity {

    private String name;
    private String description;
    private double startingPrice;
    private String imageBase64;

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
        this.imageBase64 = null; // Khởi tạo giá trị mặc định là null
    }

    public void printInfo() {
        System.out.printf("[%s] id=%s | Tên: %s | Giá khởi điểm: %.2f%n  Mô tả: %s%n",
                getClass().getSimpleName(), getId(), name, startingPrice, description);
    }

    // --- CÁC GETTER & SETTER CŨ ---

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

    // --- GETTER & SETTER CHO ẢNH (MỚI THÊM) ---

    public String getImageBase64() {
        return imageBase64;
    }

    public void setImageBase64(String imageBase64) {
        this.imageBase64 = imageBase64;
    }
}