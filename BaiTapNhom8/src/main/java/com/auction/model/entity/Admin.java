package com.auction.model.entity;

import com.auction.model.enums.AuctionStatus;

public class Admin extends User {

    public Admin(String username, String password, String email) {
        super(username, password, email);
    }

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

    @Override
    public void printInfo() {
        super.printInfo();
        System.out.println("  └─ Vai trò: Quản trị viên hệ thống");
    }
}