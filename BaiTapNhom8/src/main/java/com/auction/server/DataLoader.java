package com.auction.server;

import com.auction.manager.AuctionManager;
import com.auction.model.entity.*;
import com.auction.model.enums.AuctionStatus;
import com.auction.model.enums.ItemType;
import com.auction.pattern.factory.ItemFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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
                    int warnings = rs.getInt("unpaid_warnings");
                    boolean isBanned = rs.getBoolean("is_banned");

                    User u;
                    if ("ADMIN".equals(role)) u = new Admin(un, pw, em);
                    else if ("SELLER".equals(role)) u = new Seller(un, pw, em);
                    else {
                        Bidder b = new Bidder(un, pw, em, bal);
                        b.setUnpaidWarnings(warnings);
                        b.setBanned(isBanned);
                        u = b;
                    }
                    setEntityId(u, id);
                    UserDAO.userCache.put(un, u);
                    UserDAO.userByIdCache.put(id, u);
                }
            }

            String sqlItems = "SELECT * FROM items";
            try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sqlItems)) {
                while (rs.next()) {
                    String id = rs.getString("id");
                    String sellerId = rs.getString("seller_id");
                    String name = rs.getString("name");
                    String desc = rs.getString("description");
                    double price = rs.getDouble("starting_price");
                    String typeStr = rs.getString("item_type");

                    ItemType type = ItemType.valueOf(typeStr);
                    Map<String, Object> params = new HashMap<>();
                    if (type == ItemType.ELECTRONICS) {
                        params.put("brand", rs.getString("brand"));
                        params.put("warrantyMonths", rs.getInt("warranty_months"));
                    } else if (type == ItemType.ART) {
                        params.put("artistName", rs.getString("artist_name"));
                        params.put("creationYear", rs.getInt("creation_year"));
                    } else if (type == ItemType.VEHICLE) {
                        params.put("mileage", rs.getDouble("mileage"));
                        params.put("licensePlate", rs.getString("license_plate"));
                    }

                    User seller = UserDAO.userByIdCache.get(sellerId);
                    if (seller instanceof Seller s) {
                        Item item = ItemFactory.getInstance().createItem(type, name, desc, price, params);
                        setEntityId(item, id);

                        String sqlImages = "SELECT image_data FROM item_images WHERE item_id = ?";
                        try (PreparedStatement stmtImg = conn.prepareStatement(sqlImages)) {
                            stmtImg.setString(1, id);
                            try (ResultSet rsImg = stmtImg.executeQuery()) {
                                List<String> images = new ArrayList<>();
                                while (rsImg.next()) {
                                    images.add(rsImg.getString("image_data"));
                                }
                                item.setImagesBase64(images);
                            }
                        }
                        getSellerItems(s).add(item);
                    }
                }
            }

            String sqlAuctions = "SELECT * FROM auctions";
            try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sqlAuctions)) {
                while (rs.next()) {
                    String id = rs.getString("id");
                    String itemId = rs.getString("item_id");
                    String sellerId = rs.getString("seller_id");
                    Timestamp start = rs.getTimestamp("start_time");
                    Timestamp end = rs.getTimestamp("end_time");
                    String status = rs.getString("status");
                    double highestPrice = rs.getDouble("current_highest_price");

                    User sellerUser = UserDAO.userByIdCache.get(sellerId);
                    if (sellerUser instanceof Seller seller) {
                        Item item = null;
                        for (Item i : seller.getItems()) {
                            if (i.getId().equals(itemId)) { item = i; break; }
                        }
                        if (item != null) {
                            Auction auction = new Auction(item, seller, start.toLocalDateTime(), end.toLocalDateTime());
                            setEntityId(auction, id);
                            setAuctionStatus(auction, AuctionStatus.valueOf(status), highestPrice);
                            getSellerAuctions(seller).add(auction);
                            AuctionManager.getInstance().registerAuction(auction);
                        }
                    }
                }
            }

            String sqlBids = "SELECT * FROM bid_transactions ORDER BY created_at ASC";
            try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sqlBids)) {
                while (rs.next()) {
                    String id = rs.getString("id");
                    String auctionId = rs.getString("auction_id");
                    String bidderId = rs.getString("bidder_id");
                    double amount = rs.getDouble("amount");

                    User bidderUser = UserDAO.userByIdCache.get(bidderId);
                    if (bidderUser instanceof Bidder bidder) {
                        Auction auction = null;
                        for (Auction a : AuctionManager.getInstance().getAllAuctions()) {
                            if (a.getId().equals(auctionId)) { auction = a; break; }
                        }
                        if (auction != null) {
                            BidTransaction tx = new BidTransaction(bidder, auction, amount);
                            setEntityId(tx, id);
                            getAuctionBidHistory(auction).add(tx);
                            setAuctionLeaderAndPrice(auction, amount, bidder);
                        }
                    }
                }
            }

            System.out.println("[DataLoader] Tải dữ liệu thành công!");

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