package com.auction.pattern.factory;

import com.auction.model.entity.Art;
import com.auction.model.entity.Electronics;
import com.auction.model.entity.Item;
import com.auction.model.entity.Vehicle;
import com.auction.model.enums.ItemType;

import java.util.Map;

/*
* Factory Method Pattern — tạo các loại Item theo ItemType
* Là Singleton: chỉ tồn tại một instance duy nhất trong toàn hệ thống
* */


public class ItemFactory {
/*
* Việc sử dụng Singleton lý do là:
* Giả dụ có 2 người seller A và B đều tạo ra sản phẩm
* Chúng sử dụng 2 cái ItemFactory khác nhau
* Sẽ có khả năng tạo ra một sản phẩm có cùng một mã ID
*
* */
    // ── Singleton ─────────────────────────────────────────────────────────────

    private static volatile ItemFactory instance;

    private ItemFactory() {}

    /**
     * Trả về instance duy nhất của ItemFactory
     */
    public static ItemFactory getInstance() {
        if (instance == null) {
            synchronized (ItemFactory.class) {
                if (instance == null) {
                    instance = new ItemFactory();
                }
            }
        }
        return instance;
    }

    // ── Factory Method ────────────────────────────────────────────────────────

    /**
     * Tạo Item theo loại
     *
     * @param type          loại sản phẩm
     * @param name          tên sản phẩm
     * @param description   mô tả
     * @param startingPrice giá khởi điểm
     * @param params        thuộc tính đặc thù (xem bảng bên dưới)
     * @return Item vừa tạo
     *
     */
    public Item createItem(ItemType type, String name, String description,
                           double startingPrice, Map<String, Object> params) {
        if (type == null) throw new IllegalArgumentException("ItemType không được null.");
        if (params == null) throw new IllegalArgumentException("params không được null.");

        return switch (type) {
            case ELECTRONICS -> createElectronics(name, description, startingPrice, params);
            case ART -> createArt(name, description, startingPrice, params);
            case VEHICLE -> createVehicle(name, description, startingPrice, params);
        };
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private Electronics createElectronics(String name, String description,
                                          double startingPrice, Map<String, Object> params) {
        String brand = getRequired(params, "brand", String.class);
        int warrantyMonths = getRequired(params, "warrantyMonths", Integer.class);
        return new Electronics(name, description, startingPrice, brand, warrantyMonths);
    }

    private Art createArt(String name, String description,
                          double startingPrice, Map<String, Object> params) {
        String artistName = getRequired(params, "artistName", String.class);
        int creationYear = getRequired(params, "creationYear", Integer.class);
        return new Art(name, description, startingPrice, artistName, creationYear);
    }

    private Vehicle createVehicle(String name, String description,
                                  double startingPrice, Map<String, Object> params) {
        double mileage = getRequired(params, "mileage", Double.class);
        String licensePlate = getRequired(params, "licensePlate", String.class);
        return new Vehicle(name, description, startingPrice, mileage, licensePlate);
    }

    /**
     * Lấy tham số bắt buộc từ map và ép kiểu an toàn.
     */
    @SuppressWarnings("unchecked")
    private <T> T getRequired(Map<String, Object> params, String key, Class<T> type) {
        Object value = params.get(key);
        if (value == null) {
            throw new IllegalArgumentException("Thiếu tham số bắt buộc: '" + key + "'");
        }
        if (!type.isInstance(value)) {
            throw new IllegalArgumentException(
                    "Tham số '" + key + "' phải là " + type.getSimpleName()
                            + " nhưng nhận được " + value.getClass().getSimpleName());
        }
        return (T) value;
    }
}