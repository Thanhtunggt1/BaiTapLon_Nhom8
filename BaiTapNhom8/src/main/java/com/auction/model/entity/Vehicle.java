package com.auction.model.entity;

public class Vehicle extends Item {

    private final double mileage;
    private final String licensePlate;

    public Vehicle(String name, String description, double startingPrice,
                   double mileage, String licensePlate) {
        super(name, description, startingPrice);
        if (mileage < 0) {
            throw new IllegalArgumentException("Số km không được âm.");
        }
        if (licensePlate == null || licensePlate.isBlank()) {
            throw new IllegalArgumentException("Biển số xe không được để trống.");
        }


        this.mileage = mileage;
        this.licensePlate = licensePlate;
    }


    @Override
    public void printInfo() {
        super.printInfo();
        System.out.printf("  └─ Biển số: %s | Km đã đi: %.1f km%n", licensePlate, mileage);
    }

}