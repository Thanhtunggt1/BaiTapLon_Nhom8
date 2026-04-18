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

/**
 * Phiên đấu giá — trung tâm của hệ thống.
 * <ul>
 *   <li>Implement {@link Subject} để hỗ trợ Observer Pattern (realtime update).</li>
 *   <li>Sử dụng {@link ReentrantLock} để xử lý Concurrent Bidding an toàn.</li>
 *   <li>Hỗ trợ Anti-sniping: tự động gia hạn khi có bid trong X giây cuối.</li>
 *   <li>Hỗ trợ Auto-Bidding thông qua PriorityQueue theo thời gian đăng ký.</li>
 * </ul>
 */
public class Auction extends Entity implements Subject {

    // ── Anti-sniping config ───────────────────────────────────────────────────
    /** Khoảng thời gian cuối (giây) để kích hoạt anti-sniping */
    public static final int SNIPE_WINDOW_SECONDS = 30;
    /** Thời gian gia hạn thêm (giây) khi anti-sniping kích hoạt */
    public static final int EXTENSION_SECONDS = 60;

    // ── Core fields ───────────────────────────────────────────────────────────
    private final Item item;
    private final Seller seller;
    private double currentHighestPrice;
    private Bidder currentLeader;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private AuctionStatus status;

    // ── Collections ────────────────────────────────────────────────────────────
    private final List<BidTransaction> bidHistory;
    private final List<Observer> observers;
    private final PriorityQueue<AutoBidConfig> autoBidQueue;  // ưu tiên theo thời gian đăng ký

    // ── Concurrency ───────────────────────────────────────────────────────────
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

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    /**
     * Bắt đầu phiên đấu giá (OPEN → RUNNING).
     */
    public synchronized void startAuction() {
        if (status != AuctionStatus.OPEN) {
            throw new IllegalStateException("Chỉ có thể bắt đầu phiên ở trạng thái OPEN.");
        }
        status = AuctionStatus.RUNNING;
        System.out.printf("[Auction:%s] Phiên đấu giá '%s' đã BẮT ĐẦU. Giá khởi điểm: %.2f%n",
                getId(), item.getName(), currentHighestPrice);
        notifyObservers();
    }

    /**
     * Kết thúc phiên đấu giá (RUNNING → FINISHED hoặc CANCELED nếu không có bid).
     */
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
            System.out.printf("[Auction:%s] KẾT THÚC — Người thắng: %s với giá %.2f%n",
                    getId(), currentLeader.getUsername(), currentHighestPrice);
        }
        notifyObservers();
    }

    /**
     * Hủy phiên đấu giá (dùng cho Admin hoặc khi có tranh chấp).
     */
    public synchronized void cancelAuction() {
        if (status == AuctionStatus.PAID) {
            throw new IllegalStateException("Phiên đã thanh toán, không thể hủy.");
        }
        status = AuctionStatus.CANCELED;
        System.out.printf("[Auction:%s] Phiên bị HỦY.%n", getId());
        notifyObservers();
    }

    /**
     * Xác nhận thanh toán — chuyển trạng thái FINISHED → PAID.
     */
    public synchronized void markAsPaid() {
        if (status != AuctionStatus.FINISHED) {
            throw new IllegalStateException("Chỉ phiên FINISHED mới có thể đánh dấu PAID.");
        }
        status = AuctionStatus.PAID;
        System.out.printf("[Auction:%s] Đã thanh toán thành công.%n", getId());
        notifyObservers();
    }

    // ── Bidding ────────────────────────────────────────────────────────────────

    /**
     * Xử lý một BidTransaction — thread-safe với ReentrantLock.
     * Sau khi bid hợp lệ, kích hoạt auto-bid từ các Bidder khác (nếu có).
     *
     * @param bid giao dịch đặt giá
     * @return true nếu bid được chấp nhận
     */
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

            // Cập nhật trạng thái phiên
            currentHighestPrice = bid.getAmount();
            currentLeader = bid.getBidder();
            bidHistory.add(bid);

            System.out.printf("[Auction:%s] Bid mới: %s đặt %.2f%n",
                    getId(), bid.getBidder().getUsername(), bid.getAmount());

            // Anti-sniping: gia hạn nếu bid trong X giây cuối
            checkAndExtend();

            // Thông báo đến tất cả Observer
            notifyObservers();

            // Xử lý auto-bid từ các Bidder khác
            triggerAutoBids(bid.getBidder());

            return true;
        } finally {
            bidLock.unlock();
        }
    }

    /**
     * Anti-sniping: nếu còn ít hơn {@value #SNIPE_WINDOW_SECONDS} giây → gia hạn thêm.
     */
    private void checkAndExtend() {
        LocalDateTime now = LocalDateTime.now();
        if (now.isAfter(endTime.minusSeconds(SNIPE_WINDOW_SECONDS))) {
            extendTime(EXTENSION_SECONDS);
        }
    }

    /**
     * Gia hạn thời gian kết thúc phiên.
     *
     * @param seconds số giây gia hạn thêm
     */
    public void extendTime(int seconds) {
        if (seconds <= 0) throw new IllegalArgumentException("Số giây gia hạn phải dương.");
        endTime = endTime.plusSeconds(seconds);
        System.out.printf("[Anti-snipe] Phiên [%s] được gia hạn thêm %d giây → Kết thúc lúc %s%n",
                getId(), seconds, endTime);
    }

    /**
     * Kích hoạt auto-bid từ hàng đợi sau mỗi bid hợp lệ.
     * Bỏ qua AutoBidConfig của bidder vừa thắng để tránh tự đấu với chính mình.
     *
     * @param lastBidder bidder vừa đặt giá (bỏ qua auto-bid của người này)
     */
    private void triggerAutoBids(Bidder lastBidder) {
        // Duyệt theo thứ tự ưu tiên (thời gian đăng ký)
        List<AutoBidConfig> sorted = new ArrayList<>(autoBidQueue);
        Collections.sort(sorted);

        for (AutoBidConfig config : sorted) {
            if (config.getBidder().equals(lastBidder)) continue;   // bỏ qua người vừa bid

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
                notifyObservers();
                // Chỉ kích hoạt auto-bid đầu tiên hợp lệ; vòng lặp tiếp theo
                // sẽ được kích hoạt khi có bid mới từ đối thủ
                break;
            }
        }
    }

    // ── Auto-bid registration ─────────────────────────────────────────────────

    /**
     * Đăng ký cấu hình auto-bid.
     * Nếu Bidder đã có config trước đó, config mới thay thế config cũ.
     *
     * @param config cấu hình auto-bid
     */
    public void registerAutoBid(AutoBidConfig config) {
        bidLock.lock();
        try {
            // Xóa config cũ của cùng Bidder (nếu có)
            autoBidQueue.removeIf(c -> c.getBidder().equals(config.getBidder()));
            autoBidQueue.add(config);
        } finally {
            bidLock.unlock();
        }
    }

    // ── Observer (Subject interface) ──────────────────────────────────────────

    @Override
    public synchronized void attach(Observer observer) {
        if (!observers.contains(observer)) {
            observers.add(observer);
        }
    }

    @Override
    public synchronized void detach(Observer observer) {
        observers.remove(observer);
    }

    @Override
    public synchronized void notifyObservers() {
        for (Observer o : observers) {
            o.update(this);
        }
    }

    // ── Utility ───────────────────────────────────────────────────────────────

    /**
     * Kiểm tra phiên có hết hạn chưa dựa vào thời gian thực.
     */
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(endTime);
    }

    /**
     * In thông tin chi tiết phiên đấu giá.
     */
    public void printInfo() {
        System.out.printf("=== Auction [%s] ===%n", getId());
        System.out.printf("  Sản phẩm   : %s%n", item.getName());
        System.out.printf("  Người bán  : %s%n", seller.getUsername());
        System.out.printf("  Trạng thái : %s%n", status);
        System.out.printf("  Giá cao nhất: %.2f%n", currentHighestPrice);
        System.out.printf("  Dẫn đầu    : %s%n",
                currentLeader != null ? currentLeader.getUsername() : "Chưa có");
        System.out.printf("  Bắt đầu    : %s%n", startTime);
        System.out.printf("  Kết thúc   : %s%n", endTime);
        System.out.printf("  Số lượt bid: %d%n", bidHistory.size());
    }

    // ── Getters ───────────────────────────────────────────────────────────────

    public Item getItem() { return item; }

    public Seller getSeller() { return seller; }

    public double getCurrentHighestPrice() { return currentHighestPrice; }

    public Bidder getCurrentLeader() { return currentLeader; }

    public LocalDateTime getStartTime() { return startTime; }

    public LocalDateTime getEndTime() { return endTime; }

    public AuctionStatus getStatus() { return status; }

    public List<BidTransaction> getBidHistory() {
        return Collections.unmodifiableList(bidHistory);
    }

    public List<Observer> getObservers() {
        return Collections.unmodifiableList(observers);
    }
}