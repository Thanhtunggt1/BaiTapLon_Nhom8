package com.auction.model.entity;

import com.auction.exception.AuctionClosedException;
import com.auction.exception.InsufficientBalanceException;
import com.auction.exception.InvalidBidException;
import com.auction.model.enums.AuctionStatus;
import com.auction.pattern.observer.Observer;


public class Bidder extends User implements Observer {
    private double balance;

    public Bidder(String username, String password, String email, double initialBalance) {
        super(username, password, email);
        if (initialBalance < 0) {
            throw new IllegalArgumentException("Số dư ban đầu không được âm.");
        }
        this.balance = initialBalance;
    }

    public boolean placeBid(Auction auction, double amount)

            throws AuctionClosedException, InvalidBidException, InsufficientBalanceException {

        if (auction == null) throw new IllegalArgumentException("Auction không được null.");

        if (auction.getStatus() != AuctionStatus.RUNNING) {
            throw new AuctionClosedException(
                    "Phiên đấu giá [" + auction.getId() + "] không ở trạng thái RUNNING.");
        }
        if (amount <= auction.getCurrentHighestPrice()) {
            throw new InvalidBidException(
                    "Giá đặt (" + amount + ") phải cao hơn giá hiện tại ("
                            + auction.getCurrentHighestPrice() + ").");
        }
        if (amount > balance) {
            throw new InsufficientBalanceException("Số dư (" + balance + ") không đủ!");
        }

        BidTransaction tx = new BidTransaction(this, auction, amount);
        return auction.placeBid(tx);
    }


    public void setupAutoBid(Auction auction, double maxBid, double increment) {
        if (auction == null) throw new IllegalArgumentException("Auction không được null.");

        if (maxBid <= auction.getCurrentHighestPrice()) {
            throw new IllegalArgumentException(
                    "maxBid phải lớn hơn giá hiện tại (" + auction.getCurrentHighestPrice() + ").");
        }

        if (increment <= 0) {
            throw new IllegalArgumentException("Bước giá phải dương.");
        }

        if (maxBid > this.balance) {
            throw new IllegalArgumentException("Giá tối đa (MaxBid) không được vượt quá số dư hiện tại. Vui lòng nạp thêm tiền!");
        }

        if (increment > this.balance) {
            throw new IllegalArgumentException("Bước giá không được lớn hơn số dư hiện tại!");
        }

        if (increment > maxBid) {
            throw new IllegalArgumentException("Bước giá vô lý! Không được lớn hơn Giá tối đa (MaxBid).");
        }

        AutoBidConfig config = new AutoBidConfig(this, auction, maxBid, increment);
        auction.registerAutoBid(config);
        System.out.printf("[AutoBid] %s đã cài auto-bid cho phiên [%s]: max=%.2f, step=%.2f%n",
                getUsername(), auction.getId(), maxBid, increment);
    }

    @Override
    public void update(Auction auction) {
        System.out.printf("[Observer] %s nhận cập nhật: Phiên [%s] — Giá cao nhất: %.2f | Người dẫn đầu: %s%n",
                getUsername(),
                auction.getId(),
                auction.getCurrentHighestPrice(),
                auction.getCurrentLeader() != null ? auction.getCurrentLeader().getUsername() : "Chưa có");
    }

    public double getBalance() { return balance; }

    public void deposit(double amount) {
        if (amount <= 0) throw new IllegalArgumentException("Số tiền nạp phải dương.");
        this.balance += amount;
    }


    public void deduct(double amount) {
        if (amount > balance) throw new InsufficientBalanceException("Số dư không đủ.");
        this.balance -= amount;
    }

    @Override
    public void printInfo() {
        super.printInfo();
        System.out.printf("  └─ Số dư: %.2f%n", balance);
    }

}