package com.auction.model.entity;

/**
 * kế thừa Item
 * Có thêm thuộc tính: tên nghệ sĩ và năm sáng tác
 */
public class Art extends Item {

    private String artistName;
    private int creationYear;

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

    // ── override printInfo ─────────────────────────────────────

    @Override
    public void printInfo() {
        super.printInfo();
        System.out.printf("  └─ Nghệ sĩ: %s | Năm sáng tác: %d%n", artistName, creationYear);
    }

    // ── Getters / Setters ────────────────────────────────────────────────────

    public String getArtistName() { return artistName; }

    public void setArtistName(String artistName) {
        if (artistName == null || artistName.isBlank()) {
            throw new IllegalArgumentException("Tên nghệ sĩ không được để trống.");
        }
        this.artistName = artistName;
    }

    public int getCreationYear() { return creationYear; }

    public void setCreationYear(int creationYear) {
        if (creationYear < 0) {
            throw new IllegalArgumentException("Năm sáng tác không hợp lệ.");
        }
        this.creationYear = creationYear;
    }
}