package com.auction.model.entity;

public class Art extends Item {

    private final String artistName;
    private final int creationYear;

    public Art(String name, String description, double startingPrice,
               String artistName, int creationYear) {
        super(name, description, startingPrice);
        if (artistName == null || artistName.isBlank()) {
            throw new IllegalArgumentException("Tên nghệ sĩ không được để trống.");
        }
        if (creationYear < 0) {
            throw new IllegalArgumentException("Năm sáng tác không hợp lệ.");
        }
        this.artistName = artistName;
        this.creationYear = creationYear;
    }

    @Override
    public void printInfo() {
        super.printInfo();
        System.out.printf("  └─ Nghệ sĩ: %s | Năm sáng tác: %d%n", artistName, creationYear);
    }

}