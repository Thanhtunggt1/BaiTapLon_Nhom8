package com.auction.model.entity;

import java.time.LocalDateTime;

/**
 * Giao dịch đặt giá (BidTransaction)
 * Mỗi lần một Bidder đặt giá hợp lệ sẽ tạo ra một BidTransaction
 * và lưu vào lịch sử của Auction
 */
public class BidTransaction extends Entity {

    private final Bidder bidder;
    private final Auction auction;
    private final double amount;
    private final LocalDateTime timestamp;

    public BidTransaction(Bidder bidder, Auction auction, double amount) {
        super();
        if (bidder == null) throw new IllegalArgumentException("Bidder không được null.");
        if (auction == null) throw new IllegalArgumentException("Auction không được null.");
        if (amount <= 0) throw new IllegalArgumentException("Số tiền đặt giá phải dương.");

        this.bidder = bidder;
        this.auction = auction;
        this.amount = amount;
        this.timestamp = LocalDateTime.now();
    }

    // ── Business methods ─────────────────────────────────────────────────────

    /**
     * Kiểm tra tính hợp lệ của giao dịch
     * Phiên đang ở trạng thái RUNNING
     * Số tiền cao hơn giá hiện tại của phiên
     *
     * @return true nếu hợp lệ
     */
    public boolean isValid() {
        if (auction.getStatus() != com.auction.model.enums.AuctionStatus.RUNNING) {
            return false;
        }
        return amount > auction.getCurrentHighestPrice();
    }

    // ── Getters ───────────────────────────────────────────────────────────────

    public Bidder getBidder() { return bidder; }

    public Auction getAuction() { return auction; }

    public double getAmount() { return amount; }

    public LocalDateTime getTimestamp() { return timestamp; }

    @Override
    public String toString() {
        return String.format("BidTransaction{id='%s', bidder='%s', amount=%.2f, time=%s}",
                getId(), bidder.getUsername(), amount, timestamp);
    }
}