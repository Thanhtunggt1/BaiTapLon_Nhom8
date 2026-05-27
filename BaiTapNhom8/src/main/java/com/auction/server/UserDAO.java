package com.auction.server;

import com.auction.model.entity.*;
import java.sql.*;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class UserDAO {

    public static final Map<String, User> userCache = new ConcurrentHashMap<>();
    public static final Map<String, User> userByIdCache = new ConcurrentHashMap<>();

    public static boolean insertUser(User user, String role) {
        String sql = "INSERT INTO users (id, username, password, email, role, balance) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, user.getId());
            stmt.setString(2, user.getUsername());
            stmt.setString(3, user.getPassword());
            stmt.setString(4, user.getEmail());
            stmt.setString(5, role.toUpperCase());
            stmt.setDouble(6, (user instanceof Bidder bidder) ? bidder.getBalance() : 0.0);

            boolean success = stmt.executeUpdate() > 0;
            if (success) {
                userCache.put(user.getUsername(), user);
                userByIdCache.put(user.getId(), user);
            }
            return success;
        } catch (SQLException e) {
            System.err.println(e.getMessage());
            return false;
        }
    }

    public static boolean updateUserBalance(Bidder bidder) {
        String sql = "UPDATE users SET balance = ? WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setDouble(1, bidder.getBalance());
            stmt.setString(2, bidder.getId());
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println(e.getMessage());
            return false;
        }
    }

    public static boolean updateBidderPenalty(Bidder bidder) {
        String sql = "UPDATE users SET unpaid_warnings = ?, is_banned = ? WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, bidder.getUnpaidWarnings());
            stmt.setBoolean(2, bidder.isBanned());
            stmt.setString(3, bidder.getId());
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println(e.getMessage());
            return false;
        }
    }

    public static boolean updateUserRole(String username, String newRole) {
        String sql = "UPDATE users SET role = ? WHERE username = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, newRole.toUpperCase());
            stmt.setString(2, username);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println(e.getMessage());
            return false;
        }
    }

    public static User findByUsername(String username) {
        return userCache.get(username);
    }

    // --- CÁC HÀM CHO GIỚI HẠN NẠP TIỀN ---

    public static double getTodayDepositTotal(String bidderId) {
        String sql = "SELECT SUM(amount) AS total FROM deposit_history WHERE bidder_id = ? AND DATE(created_at) = CURDATE()";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, bidderId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getDouble("total");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0.0;
    }

    public static void insertDepositHistory(String bidderId, double amount) {
        String sql = "INSERT INTO deposit_history (bidder_id, amount) VALUES (?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, bidderId);
            stmt.setDouble(2, amount);
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}