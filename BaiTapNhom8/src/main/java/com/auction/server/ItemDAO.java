package com.auction.server;

import com.auction.model.entity.Item;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Map;

public class ItemDAO {
    public static boolean insertItem(Item item, String sellerId, String itemType, Map<String, Object> params) {
        // Đã xóa cột image_data khỏi bảng items
        String sqlItem = "INSERT INTO items (id, seller_id, name, description, starting_price, item_type, brand, warranty_months, artist_name, creation_year, mileage, license_plate) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false); // Bắt đầu transaction

            try (PreparedStatement stmt = conn.prepareStatement(sqlItem)) {
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
                stmt.executeUpdate();
            }

            // Lưu danh sách ảnh vào bảng item_images
            if (item.getImagesBase64() != null && !item.getImagesBase64().isEmpty()) {
                String sqlImg = "INSERT INTO item_images (item_id, image_data) VALUES (?, ?)";
                try (PreparedStatement stmtImg = conn.prepareStatement(sqlImg)) {
                    for (String base64 : item.getImagesBase64()) {
                        stmtImg.setString(1, item.getId());
                        stmtImg.setString(2, base64);
                        stmtImg.addBatch();
                    }
                    stmtImg.executeBatch();
                }
            }

            conn.commit(); // Hoàn tất transaction
            return true;
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

    public static boolean deleteItem(String itemId) {
        String sqlImages = "DELETE FROM item_images WHERE item_id = ?";
        String sqlItem = "DELETE FROM items WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement stmtImg = conn.prepareStatement(sqlImages);
                 PreparedStatement stmtItem = conn.prepareStatement(sqlItem)) {
                stmtImg.setString(1, itemId);
                stmtImg.executeUpdate();
                stmtItem.setString(1, itemId);
                int affected = stmtItem.executeUpdate();
                conn.commit();
                return affected > 0;
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        } catch (SQLException e) {
            System.err.println(e.getMessage());
            return false;
        }
    }
}