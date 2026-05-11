package com.auction.server;

import com.auction.model.entity.Auction;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;

public class AuctionDAO {

    public static boolean insertAuction(Auction auction) {
        String sql = "INSERT INTO auctions (id, item_id, seller_id, start_time, end_time, status, current_highest_price) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, auction.getId());
            stmt.setString(2, auction.getItem().getId());
            stmt.setString(3, auction.getSeller().getId());
            stmt.setTimestamp(4, Timestamp.valueOf(auction.getStartTime()));
            stmt.setTimestamp(5, Timestamp.valueOf(auction.getEndTime()));
            stmt.setString(6, auction.getStatus().name());
            stmt.setDouble(7, auction.getCurrentHighestPrice());
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[AuctionDAO] Lỗi: " + e.getMessage());
            return false;
        }
    }

    public static boolean updateAuctionStatus(String auctionId, String newStatus) {
        String sql = "UPDATE auctions SET status = ? WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, newStatus);
            stmt.setString(2, auctionId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[AuctionDAO] Lỗi cập nhật status: " + e.getMessage());
            return false;
        }
    }
}