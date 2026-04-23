package com.auction.model.entity;

/**
 * Lớp trừu tượng đại diện cho sản phẩm đấu giá.
 * Các loại cụ thể (Electronics, Art, Vehicle) kế thừa lớp này.
 */
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
        /**
         * Nếu biến description được truyền vào khác null thì gán giá trị đó cho thuộc tính this.description
         * Ngược lại, nếu description là null thì gán chuỗi rỗng "" cho this.description
         *Toán tử ba ngôi (ternary operator) để tránh lỗi NullPointerException và đảm bảo rằng this.description luôn có một giá trị hợp lệ
         */
        this.description = description != null ? description : "";
        this.startingPrice = startingPrice;
    }

    //Abstract / Polymorphism

    /**
     * In thông tin sản phẩm — subclass override để thêm thuộc tính đặc thù.
     */
    public void printInfo() {
        System.out.printf("[%s] id=%s | Tên: %s | Giá khởi điểm: %.2f%n  Mô tả: %s%n",
                getClass().getSimpleName(), getId(), name, startingPrice, description);
    }

    //Getters / Setters

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