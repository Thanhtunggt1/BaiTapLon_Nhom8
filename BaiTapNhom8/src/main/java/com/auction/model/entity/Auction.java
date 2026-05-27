package com.auction.model.entity;

import com.auction.exception.AuctionClosedException;
import com.auction.exception.InvalidBidException;
import com.auction.model.enums.AuctionStatus;
import com.auction.pattern.observer.Observer;
import com.auction.pattern.observer.Subject;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.PriorityQueue;
import java.util.concurrent.locks.ReentrantLock;

public class Auction extends Entity implements Subject {

    public static final int SNIPE_WINDOW_SECONDS = 30;
    public static final int EXTENSION_SECONDS = 60;

    private final Item item;
    private final Seller seller;
    private double currentHighestPrice;
    private Bidder currentLeader;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private LocalDateTime finishedTime;
    private AuctionStatus status;

    private final List<BidTransaction> bidHistory;
    private final List<Observer> observers;
    private final PriorityQueue<AutoBidConfig> autoBidQueue;

    private final ReentrantLock bidLock = new ReentrantLock();

    public Auction(Item item, Seller seller, LocalDateTime startTime, LocalDateTime endTime) {
        super();
        if (item == null) throw new IllegalArgumentException("Item không được null.");
        if (seller == null) throw new IllegalArgumentException("Seller không được null.");
        if (startTime == null || endTime == null || !endTime.isAfter(startTime)) {
            throw new IllegalArgumentException("Thời gian không hợp lệ.");
        }
        this.item = item;
        this.seller = seller;
        this.startTime = startTime;
        this.endTime = endTime;
        this.currentHighestPrice = item.getStartingPrice();
        this.currentLeader = null;
        this.status = AuctionStatus.OPEN;
        this.bidHistory = new ArrayList<>();
        this.observers = new ArrayList<>();
        this.autoBidQueue = new PriorityQueue<>();
    }

    public synchronized void startAuction() {
        if (status != AuctionStatus.OPEN) {
            throw new IllegalStateException("Chỉ có thể bắt đầu phiên ở trạng thái OPEN.");
        }

        LocalDateTime now = LocalDateTime.now();
        long offsetSeconds = java.time.temporal.ChronoUnit.SECONDS.between(this.startTime, now);
        if (offsetSeconds > 0) {
            this.startTime = now;
            this.endTime = this.endTime.plusSeconds(offsetSeconds);
        }

        status = AuctionStatus.RUNNING;
        System.out.printf("[Auction:%s] Phiên đấu giá '%s' đã BẮT ĐẦU. Giá khởi điểm: %.2f%n",
                getId(), item.getName(), currentHighestPrice);
        notifyObservers();
    }

    public synchronized void endAuction() {
        if (status != AuctionStatus.RUNNING) {
            System.out.println("[Auction] Phiên không ở trạng thái RUNNING, bỏ qua endAuction.");
            return;
        }
        if (currentLeader == null) {
            status = AuctionStatus.CANCELED;
            System.out.printf("[Auction:%s] Không có bid nào → KẾT THÚC với trạng thái CANCELED.%n", getId());
        } else {
            status = AuctionStatus.FINISHED;
            this.finishedTime = LocalDateTime.now();
            System.out.printf("[Auction:%s] KẾT THÚC — Người thắng: %s với giá %.2f%n",
                    getId(), currentLeader.getUsername(), currentHighestPrice);
        }
        notifyObservers();
    }

    public synchronized void cancelAuction() {
        if (status == AuctionStatus.PAID) {
            throw new IllegalStateException("Phiên đã thanh toán, không thể hủy.");
        }
        status = AuctionStatus.CANCELED;
        System.out.printf("[Auction:%s] Phiên bị HỦY.%n", getId());
        notifyObservers();
    }

    public synchronized void cancelDueToUnpaid() {
        if (status == AuctionStatus.PAID) {
            return;
        }
        status = AuctionStatus.CANCELED;
        if (currentLeader != null) {
            currentLeader.addUnpaidWarning();
            System.out.printf("[Penalty] Tài khoản %s bị cảnh báo %d/3 lần do không thanh toán phiên [%s]%n",
                    currentLeader.getUsername(), currentLeader.getUnpaidWarnings(), getId());
        }
        System.out.printf("[Auction:%s] Phiên bị HỦY do quá hạn thanh toán 12h. Sản phẩm hoàn trả cho %s.%n",
                getId(), seller.getUsername());
        notifyObservers();
    }

    public synchronized void markAsPaid() {
        if (status != AuctionStatus.FINISHED) {
            throw new IllegalStateException("Chỉ phiên FINISHED mới có thể đánh dấu PAID.");
        }
        if (currentLeader != null) {
            currentLeader.deduct(currentHighestPrice);
            System.out.printf("[Payment] Đã trừ %.2f VNĐ từ tài khoản %s%n",
                    currentHighestPrice, currentLeader.getUsername());
        }
        status = AuctionStatus.PAID;
        System.out.printf("[Auction:%s] Đã thanh toán thành công.%n", getId());
        notifyObservers();
    }

    public boolean placeBid(BidTransaction bid) {
        bidLock.lock();
        try {
            if (status != AuctionStatus.RUNNING) {
                throw new AuctionClosedException("Phiên đấu giá không RUNNING.");
            }
            if (!bid.isValid()) {
                throw new InvalidBidException(
                        "Bid không hợp lệ: amount=" + bid.getAmount()
                                + " <= currentHighest=" + currentHighestPrice);
            }
            currentHighestPrice = bid.getAmount();
            currentLeader = bid.getBidder();
            bidHistory.add(bid);
            System.out.printf("[Auction:%s] Bid mới: %s đặt %.2f%n",
                    getId(), bid.getBidder().getUsername(), bid.getAmount());
            checkAndExtend();
            notifyObservers();
            triggerAutoBids(bid.getBidder());
            return true;
        } finally {
            bidLock.unlock();
        }
    }

    private void checkAndExtend() {
        LocalDateTime now = LocalDateTime.now();
        if (now.isAfter(endTime.minusSeconds(SNIPE_WINDOW_SECONDS))) {
            extendTime(EXTENSION_SECONDS);
        }
    }

    public void extendTime(int seconds) {
        if (seconds <= 0) throw new IllegalArgumentException("Số giây gia hạn phải dương.");
        endTime = endTime.plusSeconds(seconds);
        System.out.printf("[Anti-snipe] Phiên [%s] được gia hạn thêm %d giây → Kết thúc lúc %s%n",
                getId(), seconds, endTime);
    }

    private void triggerAutoBids(Bidder lastBidder) {
        boolean triggered;
        do {
            triggered = false;
            List<AutoBidConfig> sorted = new ArrayList<>(autoBidQueue);
            Collections.sort(sorted);
            for (AutoBidConfig config : sorted) {
                if (config.getBidder().equals(currentLeader)) continue;
                double nextBid = config.computeNextBid(currentHighestPrice);
                if (nextBid < 0) {
                    System.out.printf("[AutoBid] %s đã đạt maxBid (%.2f), không thể tiếp tục.%n",
                            config.getBidder().getUsername(), config.getMaxBid());
                    continue;
                }
                if (nextBid > config.getBidder().getBalance()) {
                    System.out.printf("[AutoBid] %s không đủ số dư cho auto-bid %.2f.%n",
                            config.getBidder().getUsername(), nextBid);
                    continue;
                }
                BidTransaction autoBid = new BidTransaction(config.getBidder(), this, nextBid);
                if (autoBid.isValid()) {
                    currentHighestPrice = nextBid;
                    currentLeader = config.getBidder();
                    bidHistory.add(autoBid);
                    System.out.printf("[AutoBid] %s tự động đặt giá %.2f%n",
                            config.getBidder().getUsername(), nextBid);
                    checkAndExtend();
                    notifyObservers();
                    triggered = true;
                    break;
                }
            }
        } while (triggered);
    }

    public void registerAutoBid(AutoBidConfig config) {
        bidLock.lock();
        try {
            autoBidQueue.removeIf(c -> c.getBidder().equals(config.getBidder()));
            autoBidQueue.add(config);
        } finally {
            bidLock.unlock();
        }
    }

    @Override
    public synchronized void notifyObservers() {
        for (Observer o : observers) {
            o.update(this);
        }
    }

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(endTime);
    }

    public Item getItem() { return item; }

    public Seller getSeller() { return seller; }

    public double getCurrentHighestPrice() {
        if (bidHistory == null || bidHistory.isEmpty()) {
            return item.getStartingPrice();
        }
        return currentHighestPrice;
    }

    public Bidder getCurrentLeader() { return currentLeader; }

    public LocalDateTime getStartTime() { return startTime; }

    public LocalDateTime getEndTime() { return endTime; }

    public LocalDateTime getFinishedTime() { return finishedTime; }

    public AuctionStatus getStatus() { return status; }

    public List<BidTransaction> getBidHistory() {
        return Collections.unmodifiableList(bidHistory);
    }
}