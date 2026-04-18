package com.auction.model.entity;

import com.auction.exception.AuctionClosedException;
import com.auction.exception.InsufficientBalanceException;
import com.auction.exception.InvalidBidException;
import com.auction.model.enums.AuctionStatus;
import com.auction.pattern.observer.Observer;

/**
 * Người tham gia đấu giá (Bidder).
 * <ul>
 *   <li>Implement {@link Observer} để nhận cập nhật realtime khi có bid mới.</li>
 *   <li>Có thể đặt bid thủ công hoặc cài đặt auto-bid.</li>
 * </ul>
 */
public class Bidder extends User implements Observer {

    private double balance;

    public Bidder(String username, String password, String email, double initialBalance) {
        super(username, password, email);
        if (initialBalance < 0) {
            throw new IllegalArgumentException("Số dư ban đầu không được âm.");
        }
        this.balance = initialBalance;
    }

    // ── Business methods ─────────────────────────────────────────────────────

    /**
     * Đặt giá thủ công cho một phiên đấu giá.
     *
     * @param auction phiên đấu giá muốn tham gia
     * @param amount  số tiền muốn đặt
     * @return true nếu bid hợp lệ và được chấp nhận
     * @throws AuctionClosedException     nếu phiên đã đóng
     * @throws InvalidBidException        nếu giá không cao hơn giá hiện tại
     * @throws InsufficientBalanceException nếu số dư không đủ
     */
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
            throw new InsufficientBalanceException(
                    "Số dư (" + balance + ") không đủ để đặt giá " + amount + ".");
        }

        BidTransaction tx = new BidTransaction(this, auction, amount);
        return auction.placeBid(tx);
    }

    /**
     * Cài đặt auto-bid cho một phiên đấu giá.
     * Hệ thống sẽ tự động trả giá thay người dùng khi có bid từ đối thủ.
     *
     * @param auction   phiên đấu giá
     * @param maxBid    giá tối đa sẵn sàng trả
     * @param increment bước giá mỗi lần auto-bid
     */
    public void setupAutoBid(Auction auction, double maxBid, double increment) {
        if (auction == null) throw new IllegalArgumentException("Auction không được null.");
        if (maxBid <= auction.getCurrentHighestPrice()) {
            throw new IllegalArgumentException(
                    "maxBid phải lớn hơn giá hiện tại (" + auction.getCurrentHighestPrice() + ").");
        }
        if (increment <= 0) {
            throw new IllegalArgumentException("Bước giá phải dương.");
        }

        AutoBidConfig config = new AutoBidConfig(this, auction, maxBid, increment);
        auction.registerAutoBid(config);
        System.out.printf("[AutoBid] %s đã cài auto-bid cho phiên [%s]: max=%.2f, step=%.2f%n",
                getUsername(), auction.getId(), maxBid, increment);
    }

    /**
     * Callback từ Observer — được gọi khi phiên đấu giá có thay đổi.
     */
    @Override
    public void update(Auction auction) {
        System.out.printf("[Observer] %s nhận cập nhật: Phiên [%s] — Giá cao nhất: %.2f | Người dẫn đầu: %s%n",
                getUsername(),
                auction.getId(),
                auction.getCurrentHighestPrice(),
                auction.getCurrentLeader() != null ? auction.getCurrentLeader().getUsername() : "Chưa có");
    }

    // ── Getters / Setters ────────────────────────────────────────────────────

    public double getBalance() { return balance; }

    /**
     * Nạp thêm tiền vào tài khoản.
     */
    public void deposit(double amount) {
        if (amount <= 0) throw new IllegalArgumentException("Số tiền nạp phải dương.");
        this.balance += amount;
    }

    /**
     * Trừ tiền khi thanh toán thắng đấu giá.
     */
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