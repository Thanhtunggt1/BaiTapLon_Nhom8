package com.auction.server;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class BidTransactionDAO {
    public static boolean insertBidTransaction(String id, String auctionId, String bidderId, double amount) {
        String sql = "INSERT INTO bid_transactions (id, auction_id, bidder_id, amount) VALUES (?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, id);
            stmt.setString(2, auctionId);
            stmt.setString(3, bidderId);
            stmt.setDouble(4, amount);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[BidTransactionDAO] Lỗi: " + e.getMessage());
            return false;
        }
    }
}