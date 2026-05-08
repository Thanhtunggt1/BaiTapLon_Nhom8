package com.auction.server;

import com.auction.model.entity.*;
import java.sql.*;

public class UserDAO {

    // 1. Lưu User mới vào Database (Dùng cho Đăng ký)
    public static boolean insertUser(User user, String role) {
        String sql = "INSERT INTO users (id, username, password, email, role, balance) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, user.getId());
            stmt.setString(2, user.getUsername());
            stmt.setString(3, user.getPassword());
            stmt.setString(4, user.getEmail());
            stmt.setString(5, role.toUpperCase());

            if (user instanceof Bidder) {
                stmt.setDouble(6, ((Bidder) user).getBalance());
            } else {
                stmt.setDouble(6, 0.0);
            }

            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[UserDAO] Lỗi Insert: " + e.getMessage());
            return false;
        }
    }

    // 2. Tìm User theo Username (Dùng cho Đăng nhập)
    public static User findByUsername(String username) {
        String sql = "SELECT * FROM users WHERE username = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                String role = rs.getString("role").toUpperCase();
                String pass = rs.getString("password");
                String email = rs.getString("email");
                double bal  = rs.getDouble("balance");

                User user;
                if ("ADMIN".equals(role)) user = new Admin(username, pass, email);
                else if ("SELLER".equals(role)) user = new Seller(username, pass, email);
                else user = new Bidder(username, pass, email, bal);

                user.setId(rs.getString("id"));
                return user;
            }
        } catch (SQLException e) {
            System.err.println("[UserDAO] Lỗi Find: " + e.getMessage());
        }
        return null;
    }
}