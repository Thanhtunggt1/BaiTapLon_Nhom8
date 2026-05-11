package com.auction.model.entity;

import java.time.LocalDateTime;

public class AutoBidConfig extends Entity implements Comparable<AutoBidConfig> {

    private final Bidder bidder;
    private final Auction auction;
    private final double maxBid;
    private final double increment;
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

    public double computeNextBid(double currentPrice) {
        double nextBid = currentPrice + increment;
        return nextBid <= maxBid ? nextBid : -1;
    }


    @Override
    public int compareTo(AutoBidConfig other) {
        return this.registeredTime.compareTo(other.registeredTime);
    }

    public Bidder getBidder() { return bidder; }

    public Auction getAuction() { return auction; }

    public double getMaxBid() { return maxBid; }

    @Override
    public String toString() {
        return String.format("AutoBidConfig{bidder='%s', maxBid=%.2f, increment=%.2f, registered=%s}",
                bidder.getUsername(), maxBid, increment, registeredTime);
    }
}