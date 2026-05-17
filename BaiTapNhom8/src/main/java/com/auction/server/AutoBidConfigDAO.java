package com.auction.server;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class AutoBidConfigDAO {
    public static boolean insertAutoBidConfig(String id, String auctionId, String bidderId, double maxBid, double incrementAmount) {
        String sql = "INSERT INTO auto_bid_configs (id, auction_id, bidder_id, max_bid, increment_amount) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, id);
            stmt.setString(2, auctionId);
            stmt.setString(3, bidderId);
            stmt.setDouble(4, maxBid);
            stmt.setDouble(5, incrementAmount);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[AutoBidConfigDAO] Lỗi: " + e.getMessage());
            return false;
        }
    }
}