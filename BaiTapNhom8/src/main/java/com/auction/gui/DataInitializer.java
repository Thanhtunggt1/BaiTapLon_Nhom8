package com.auction.gui;

import com.auction.manager.AuctionManager;
import com.auction.model.entity.*;
import com.auction.model.enums.ItemType;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Khởi tạo dữ liệu mẫu khi ứng dụng khởi động.
 */
public class DataInitializer {

  public static void init() {
    // ── Users ─────────────────────────────────────────────────────────────
    Admin admin = new Admin("admin", "admin123", "admin@auction.com");
    UserStore.addUser(admin);

    Seller seller1 = new Seller("seller1", "seller123", "seller1@auction.com");
    Seller seller2 = new Seller("seller2", "seller123", "seller2@auction.com");
    UserStore.addUser(seller1);
    UserStore.addUser(seller2);

    Bidder bidder1 = new Bidder("bidder1", "bidder123", "bidder1@auction.com", 50_000_000);
    Bidder bidder2 = new Bidder("bidder2", "bidder123", "bidder2@auction.com", 100_000_000);
    Bidder bidder3 = new Bidder("bidder3", "bidder123", "bidder3@auction.com", 20_000_000);
    UserStore.addUser(bidder1);
    UserStore.addUser(bidder2);
    UserStore.addUser(bidder3);

    // ── Items for seller1 ─────────────────────────────────────────────────
    Map<String, Object> p1 = new HashMap<>();
    p1.put("brand", "Samsung");
    p1.put("warrantyMonths", 24);
    Item tv = seller1.createItem("TV Samsung 55\" QLED", "Smart TV 4K, HDR, Wi-Fi tích hợp",
        15_000_000, ItemType.ELECTRONICS, p1);

    Map<String, Object> p2 = new HashMap<>();
    p2.put("artistName", "Nguyễn Sáng");
    p2.put("creationYear", 1985);
    Item painting = seller1.createItem("Tranh Kết Nạp Đảng", "Sơn dầu trên bố, 80x120cm",
        50_000_000, ItemType.ART, p2);

    Map<String, Object> p3 = new HashMap<>();
    p3.put("brand", "Apple");
    p3.put("warrantyMonths", 12);
    Item macbook = seller1.createItem("MacBook Pro M3 14\"", "Chip M3 Pro, 18GB RAM, 512GB SSD",
        55_000_000, ItemType.ELECTRONICS, p3);

    // ── Items for seller2 ─────────────────────────────────────────────────
    Map<String, Object> p4 = new HashMap<>();
    p4.put("mileage", 45000.0);
    p4.put("licensePlate", "29A-12345");
    Item car = seller2.createItem("Honda City 2022", "Xe sedan, bản G, màu trắng ngọc trai",
        480_000_000, ItemType.VEHICLE, p4);

    Map<String, Object> p5 = new HashMap<>();
    p5.put("mileage", 0.0);
    p5.put("licensePlate", "51F-99999");
    Item bike = seller2.createItem("Xe máy Honda Wave 2024", "Xe mới 100%, chưa lăn bánh",
        20_000_000, ItemType.VEHICLE, p5);

    // ── Auctions ──────────────────────────────────────────────────────────
    LocalDateTime now = LocalDateTime.now();

    // Auction 1: đang chạy (TV Samsung)
    Auction a1 = seller1.createAuction(tv, now.minusMinutes(10), now.plusMinutes(45));
    AuctionManager.getInstance().registerAuction(a1);
    a1.startAuction();

    // Auction 2: chưa bắt đầu (Tranh)
    Auction a2 = seller1.createAuction(painting, now.plusMinutes(5), now.plusHours(2));
    AuctionManager.getInstance().registerAuction(a2);

    // Auction 3: đang chạy (Honda City)
    Auction a3 = seller2.createAuction(car, now.minusMinutes(20), now.plusHours(3));
    AuctionManager.getInstance().registerAuction(a3);
    a3.startAuction();

    // Auction 4: đang chạy (MacBook)
    Auction a4 = seller1.createAuction(macbook, now.minusMinutes(5), now.plusMinutes(60));
    AuctionManager.getInstance().registerAuction(a4);
    a4.startAuction();

    // Auction 5: chưa bắt đầu (Wave)
    Auction a5 = seller2.createAuction(bike, now.plusMinutes(15), now.plusHours(1));
    AuctionManager.getInstance().registerAuction(a5);

    // ── Seed some bids ─────────────────────────────────────────────────────
    try {
      a1.placeBid(new BidTransaction(bidder1, a1, 15_500_000));
      a1.placeBid(new BidTransaction(bidder2, a1, 16_000_000));
      a1.placeBid(new BidTransaction(bidder3, a1, 16_500_000));
      a1.placeBid(new BidTransaction(bidder1, a1, 17_200_000));

      a3.placeBid(new BidTransaction(bidder2, a3, 485_000_000));
      a3.placeBid(new BidTransaction(bidder1, a3, 490_000_000));

      a4.placeBid(new BidTransaction(bidder2, a4, 56_000_000));
    } catch (Exception e) {
      System.err.println("[DataInitializer] Lỗi seed bids: " + e.getMessage());
    }

    System.out.println("[DataInitializer] Dữ liệu mẫu đã được khởi tạo thành công.");
    System.out.println("  Tài khoản mẫu: admin/admin123 | seller1/seller123 | bidder1/bidder123");
  }
}