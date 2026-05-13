package com.auction.server;

import com.auction.model.entity.Item;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Map;

public class ItemDAO {
    public static boolean insertItem(Item item, String sellerId, String itemType, Map<String, Object> params) {
        String sql = "INSERT INTO items (id, seller_id, name, description, starting_price, item_type, brand, warranty_months, artist_name, creation_year, mileage, license_plate) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, item.getId());
            stmt.setString(2, sellerId);
            stmt.setString(3, item.getName());
            stmt.setString(4, item.getDescription());
            stmt.setDouble(5, item.getStartingPrice());
            stmt.setString(6, itemType);

            stmt.setObject(7, params.get("brand"));
            stmt.setObject(8, params.get("warrantyMonths"));
            stmt.setObject(9, params.get("artistName"));
            stmt.setObject(10, params.get("creationYear"));
            stmt.setObject(11, params.get("mileage"));
            stmt.setObject(12, params.get("licensePlate"));

            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[ItemDAO] Lỗi: " + e.getMessage());
            return false;
        }
    }

    public static boolean updateItem(Item item) {
        String sql = "UPDATE items SET name = ?, description = ?, starting_price = ? WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, item.getName());
            stmt.setString(2, item.getDescription());
            stmt.setDouble(3, item.getStartingPrice());
            stmt.setString(4, item.getId());
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[ItemDAO] Lỗi cập nhật: " + e.getMessage());
            return false;
        }
    }
}