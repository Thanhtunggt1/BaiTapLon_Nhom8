package com.auction.gui;

import com.auction.model.entity.*;

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

    Bidder bidder1 = new Bidder("bidder1", "bidder123", "bidder1@auction.com", 0);
    Bidder bidder2 = new Bidder("bidder2", "bidder123", "bidder2@auction.com", 0);
    UserStore.addUser(bidder1);
    UserStore.addUser(bidder2);

    System.out.println("[DataInitializer] Tài khoản mẫu đã được khởi tạo thành công.");
    System.out.println("  admin/admin123 | seller1/seller123 | seller2/seller123 | bidder1/bidder123 | bidder2/bidder123");
  }
}