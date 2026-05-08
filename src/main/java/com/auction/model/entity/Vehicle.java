package com.auction.model.entity;

/**
 * kế thừa Item
 * Có thêm thuộc tính: số km đã đi và biển số xe
 */

public class Vehicle extends Item {

    private double mileage;        // km đã đi
    private String licensePlate;   // Biển số xe

    public Vehicle(String name, String description, double startingPrice,
                   double mileage, String licensePlate) {
        super(name, description, startingPrice); //Gọi đến constr thằng cha Item
        if (mileage < 0) {
            throw new IllegalArgumentException("Số km không được âm.");
        }
        if (licensePlate == null || licensePlate.isBlank()) {
            throw new IllegalArgumentException("Biển số xe không được để trống.");
        }

        // Sau khi thỏa mãn các dòng trên, bắt đầu gán

        this.mileage = mileage;
        this.licensePlate = licensePlate;
    }

    // override printInfo

    @Override
    public void printInfo() {
        super.printInfo();
        // Thêm cái printInfo() của Item cái print ra thêm 2 cái thuộc tính đặc thù licensePlate, mileage
        System.out.printf("  └─ Biển số: %s | Km đã đi: %.1f km%n", licensePlate, mileage);
    }

    // ── Getters / Setters ────────────────────────────────────────────────────

    public double getMileage() { return mileage; }

    public void setMileage(double mileage) {
        if (mileage < 0) throw new IllegalArgumentException("Số km không được âm.");
        this.mileage = mileage;
    }

    public String getLicensePlate() { return licensePlate; }

    public void setLicensePlate(String licensePlate) {
        if (licensePlate == null || licensePlate.isBlank()) {
            throw new IllegalArgumentException("Biển số xe không được để trống.");
        }
        this.licensePlate = licensePlate;
    }
}