package com.auction.model.entity;

import com.auction.manager.AuctionManager;
import com.auction.model.enums.AuctionStatus;

/**
 * Quản trị viên hệ thống (Admin)
 * Có quyền quản lý toàn bộ hệ thống và giải quyết tranh chấp
 */
public class Admin extends User {

    public Admin(String username, String password, String email) {
        super(username, password, email);
    }

    // ── Business methods ─────────────────────────────────────────────────────

    /**
     * Quản lý hệ thống — in tổng quan các phiên đang hoạt động.
     */
    public void manageSystem() {
        System.out.println("=== [Admin] Quản lý hệ thống ===");
        AuctionManager manager = AuctionManager.getInstance();
        manager.printSystemSummary();
    }

    /**
     * Giải quyết tranh chấp: có thể hủy một phiên đấu giá.
     * @param auction phiên bị tranh chấp
     * @param reason  lý do hủy
     */
    public void resolveDispute(Auction auction, String reason) {
        if (auction == null) throw new IllegalArgumentException("Auction không được null.");

        if (auction.getStatus() == AuctionStatus.PAID) {
            System.out.println("[Admin] Phiên đã thanh toán, không thể hủy.");
            return;
        }

        auction.cancelAuction();
        System.out.printf("[Admin:%s] Đã hủy phiên [%s]. Lý do: %s%n",
                getUsername(), auction.getId(), reason);
    }

    /**
     * Xem thông tin chi tiết của bất kỳ phiên đấu giá nào.
     * @param auction phiên cần xem
     */
    public void inspectAuction(Auction auction) {
        if (auction == null) throw new IllegalArgumentException("Auction không được null.");
        auction.printInfo();
    }

    @Override
    public void printInfo() {
        super.printInfo();
        System.out.println("  └─ Vai trò: Quản trị viên hệ thống");
    }
}