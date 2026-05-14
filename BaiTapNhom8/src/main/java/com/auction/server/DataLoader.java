package com.auction.server;

import com.auction.manager.AuctionManager;
import com.auction.model.entity.*;
import com.auction.model.enums.AuctionStatus;
import com.auction.model.enums.ItemType;
import com.auction.pattern.factory.ItemFactory;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Map;

public class DataLoader {

    public static void loadAll() {
        try (Connection conn = DatabaseConnection.getConnection()) {
            System.out.println("[DataLoader] Bắt đầu đồng bộ dữ liệu từ MySQL lên RAM...");

            String sqlUsers = "SELECT * FROM users";
            try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sqlUsers)) {
                while (rs.next()) {
                    String id = rs.getString("id");
                    String un = rs.getString("username");
                    String pw = rs.getString("password");
                    String em = rs.getString("email");
                    String role = rs.getString("role");
                    double bal = rs.getDouble("balance");

                    User u;
                    if ("ADMIN".equals(role)) u = new Admin(un, pw, em);
                    else if ("SELLER".equals(role)) u = new Seller(un, pw, em);
                    else u = new Bidder(un, pw, em, bal);

                    setEntityId(u, id);
                    UserDAO.userCache.put(un, u);
                    UserDAO.userByIdCache.put(id, u);
                }
            }

            Map<String, Item> itemMap = new HashMap<>();
            String sqlItems = "SELECT * FROM items";
            try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sqlItems)) {
                while (rs.next()) {
                    String id = rs.getString("id");
                    String sellerId = rs.getString("seller_id");
                    String name = rs.getString("name");
                    String desc = rs.getString("description");
                    double startPrice = rs.getDouble("starting_price");
                    String typeStr = rs.getString("item_type");
                    String imageBase64 = rs.getString("image_data"); // Tải ảnh

                    Map<String, Object> params = new HashMap<>();
                    params.put("brand", rs.getString("brand") != null ? rs.getString("brand") : "");
                    params.put("warrantyMonths", rs.getObject("warranty_months") != null ? rs.getInt("warranty_months") : 0);
                    params.put("artistName", rs.getString("artist_name") != null ? rs.getString("artist_name") : "");
                    params.put("creationYear", rs.getObject("creation_year") != null ? rs.getInt("creation_year") : 0);
                    params.put("mileage", rs.getObject("mileage") != null ? rs.getDouble("mileage") : 0.0);
                    params.put("licensePlate", rs.getString("license_plate") != null ? rs.getString("license_plate") : "");

                    ItemType type = ItemType.valueOf(typeStr);
                    Item item = ItemFactory.getInstance().createItem(type, name, desc, startPrice, params);
                    item.setImageBase64(imageBase64); // Gắn ảnh vào đối tượng
                    setEntityId(item, id);
                    itemMap.put(id, item);

                    User sellerUser = UserDAO.userByIdCache.get(sellerId);
                    if (sellerUser instanceof Seller seller) {
                        getSellerItems(seller).add(item);
                    }
                }
            }

            Map<String, Auction> auctionMap = new HashMap<>();
            String sqlAuctions = "SELECT * FROM auctions";
            try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sqlAuctions)) {
                while (rs.next()) {
                    String id = rs.getString("id");
                    String itemId = rs.getString("item_id");
                    String sellerId = rs.getString("seller_id");
                    Timestamp start = rs.getTimestamp("start_time");
                    Timestamp end = rs.getTimestamp("end_time");
                    String statusStr = rs.getString("status");
                    double currentPrice = rs.getDouble("current_highest_price");

                    Item item = itemMap.get(itemId);
                    User sellerUser = UserDAO.userByIdCache.get(sellerId);

                    if (item != null && sellerUser instanceof Seller seller) {
                        Auction auction = new Auction(item, seller, start.toLocalDateTime(), end.toLocalDateTime());
                        setEntityId(auction, id);
                        setAuctionStatus(auction, AuctionStatus.valueOf(statusStr), currentPrice);

                        getSellerAuctions(seller).add(auction);
                        AuctionManager.getInstance().registerAuction(auction);
                        auctionMap.put(id, auction);
                    }
                }
            }

            String sqlBids = "SELECT * FROM bid_transactions ORDER BY id ASC";
            try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sqlBids)) {
                while (rs.next()) {
                    String id = rs.getString("id");
                    String auctionId = rs.getString("auction_id");
                    String bidderId = rs.getString("bidder_id");
                    double amount = rs.getDouble("amount");

                    Auction auction = auctionMap.get(auctionId);
                    User bidderUser = UserDAO.userByIdCache.get(bidderId);

                    if (auction != null && bidderUser instanceof Bidder bidder) {
                        BidTransaction tx = new BidTransaction(bidder, auction, amount);
                        setEntityId(tx, id);
                        getAuctionBidHistory(auction).add(tx);
                        setAuctionLeaderAndPrice(auction, amount, bidder);
                    }
                }
            }
            System.out.println("[DataLoader] Đã tải xong dữ liệu từ MySQL vào RAM!");
        } catch (Exception e) {
            System.err.println("[DataLoader] Lỗi: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void setEntityId(Entity entity, String id) throws Exception {
        java.lang.reflect.Field f = Entity.class.getDeclaredField("id");
        f.setAccessible(true); f.set(entity, id);
    }
    private static java.util.List<Item> getSellerItems(Seller seller) throws Exception {
        java.lang.reflect.Field f = Seller.class.getDeclaredField("items");
        f.setAccessible(true); return (java.util.List<Item>) f.get(seller);
    }
    private static java.util.List<Auction> getSellerAuctions(Seller seller) throws Exception {
        java.lang.reflect.Field f = Seller.class.getDeclaredField("auctions");
        f.setAccessible(true); return (java.util.List<Auction>) f.get(seller);
    }
    private static java.util.List<BidTransaction> getAuctionBidHistory(Auction auction) throws Exception {
        java.lang.reflect.Field f = Auction.class.getDeclaredField("bidHistory");
        f.setAccessible(true); return (java.util.List<BidTransaction>) f.get(auction);
    }
    private static void setAuctionStatus(Auction auction, AuctionStatus status, double highestPrice) throws Exception {
        java.lang.reflect.Field fStatus = Auction.class.getDeclaredField("status");
        fStatus.setAccessible(true); fStatus.set(auction, status);

        java.lang.reflect.Field fPrice = Auction.class.getDeclaredField("currentHighestPrice");
        fPrice.setAccessible(true); fPrice.setDouble(auction, highestPrice);
    }
    private static void setAuctionLeaderAndPrice(Auction auction, double highestPrice, Bidder leader) throws Exception {
        java.lang.reflect.Field fPrice = Auction.class.getDeclaredField("currentHighestPrice");
        fPrice.setAccessible(true); fPrice.setDouble(auction, highestPrice);

        java.lang.reflect.Field fLeader = Auction.class.getDeclaredField("currentLeader");
        fLeader.setAccessible(true); fLeader.set(auction, leader);
    }
}