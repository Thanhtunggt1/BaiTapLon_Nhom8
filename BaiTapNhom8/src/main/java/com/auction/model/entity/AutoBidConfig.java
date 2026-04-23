package com.auction.model.entity;

import java.time.LocalDateTime;

/**
 * Cấu hình Auto-Bid của một Bidder cho một phiên đấu giá cụ thể.
 * Khi có bid mới từ đối thủ, AuctionManager sẽ kiểm tra các AutoBidConfig
 * và tự động đặt giá theo thứ tự ưu tiên (thời điểm đăng ký sớm hơn → ưu tiên hơn).
 */
public class AutoBidConfig extends Entity implements Comparable<AutoBidConfig> {

    private final Bidder bidder;
    private final Auction auction;
    private double maxBid;
    private double increment;
    private final LocalDateTime registeredTime;

    public AutoBidConfig(Bidder bidder, Auction auction, double maxBid, double increment) {
        super();
        if (bidder == null) throw new IllegalArgumentException("Bidder không được null.");
        if (auction == null) throw new IllegalArgumentException("Auction không được null.");
        if (maxBid <= 0) throw new IllegalArgumentException("maxBid phải dương.");
        if (increment <= 0) throw new IllegalArgumentException("increment phải dương.");

        this.bidder = bidder;
        this.auction = auction;
        this.maxBid = maxBid;
        this.increment = increment;
        this.registeredTime = LocalDateTime.now();
    }

    // Business methods

    /**
     * Tính giá auto-bid tiếp theo dựa trên giá hiện tại của phiên.
     * @param currentPrice giá hiện tại cao nhất
     * @return giá auto-bid đề xuất, hoặc -1 nếu vượt quá maxBid
     */
    public double computeNextBid(double currentPrice) {
        double nextBid = currentPrice + increment;
        return nextBid <= maxBid ? nextBid : -1;
    }

    /**
     * So sánh theo thời gian đăng ký: đăng ký sớm hơn có độ ưu tiên cao hơn.
     */
    @Override
    public int compareTo(AutoBidConfig other) {
        return this.registeredTime.compareTo(other.registeredTime);
    }

    //Getters / Setters

    public Bidder getBidder() { return bidder; }

    public Auction getAuction() { return auction; }

    public double getMaxBid() { return maxBid; }

    public void setMaxBid(double maxBid) {
        if (maxBid <= 0) throw new IllegalArgumentException("maxBid phải dương.");
        this.maxBid = maxBid;
    }

    public double getIncrement() { return increment; }

    public void setIncrement(double increment) {
        if (increment <= 0) throw new IllegalArgumentException("increment phải dương.");
        this.increment = increment;
    }

    public LocalDateTime getRegisteredTime() { return registeredTime; }

    @Override
    public String toString() {
        return String.format("AutoBidConfig{bidder='%s', maxBid=%.2f, increment=%.2f, registered=%s}",
                bidder.getUsername(), maxBid, increment, registeredTime);
    }
}