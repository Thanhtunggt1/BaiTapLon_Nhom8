package com.auction.model.entity;

/**
 * kế thừa Item
 * Có thêm thuộc tính: thương hiệu và thời hạn bảo hành (tháng).
 */
public class Electronics extends Item {

    private final String brand;
    private final int warrantyMonths;

    public Electronics(String name, String description, double startingPrice, String brand, int warrantyMonths) {
        super(name, description, startingPrice);
        if (brand == null || brand.isBlank()) {
            throw new IllegalArgumentException("Thương hiệu không được để trống.");
        }
        if (warrantyMonths < 0) {
            throw new IllegalArgumentException("Số tháng bảo hành không được âm.");
        }
        this.brand = brand;
        this.warrantyMonths = warrantyMonths;
    }

    @Override
    public void printInfo() {
        super.printInfo();
        System.out.printf("  └─ Thương hiệu: %s | Bảo hành: %d tháng%n", brand, warrantyMonths);
    }

    public String getBrand() { return brand; }

    public int getWarrantyMonths() { return warrantyMonths; }

}