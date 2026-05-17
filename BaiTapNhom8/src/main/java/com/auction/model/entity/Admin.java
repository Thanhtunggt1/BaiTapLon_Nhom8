package com.auction.model.entity;

import com.auction.model.enums.AuctionStatus;
import com.auction.model.enums.DepositStatus;

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

    public void approveDeposit(DepositRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Yêu cầu nạp tiền không được null.");
        }

        if (request.getStatus() != DepositStatus.PENDING) {
            throw new IllegalStateException("Yêu cầu nạp tiền đã được xử lý.");
        }

        Bidder bidder = request.getBidder();
        bidder.deposit(request.getAmount());
        request.setStatus(DepositStatus.APPROVED);

        System.out.printf("[Admin:%s] Đã duyệt nạp %.2f cho bidder %s%n",
                getUsername(), request.getAmount(), bidder.getUsername());
    }

    public void rejectDeposit(DepositRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Yêu cầu nạp tiền không được null.");
        }

        if (request.getStatus() != DepositStatus.PENDING) {
            throw new IllegalStateException("Yêu cầu nạp tiền đã được xử lý.");
        }

        request.setStatus(DepositStatus.REJECTED);

        System.out.printf("[Admin:%s] Đã từ chối yêu cầu nạp %.2f của bidder %s%n",
                getUsername(), request.getAmount(), request.getBidder().getUsername());
    }

    @Override
    public void printInfo() {
        super.printInfo();
        System.out.println(" └ Vai trò: Quản trị viên hệ thống");
    }
}