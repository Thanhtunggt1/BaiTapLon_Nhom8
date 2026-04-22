package com.auction.manager;

import com.auction.model.entity.Auction;
import com.auction.model.entity.BidTransaction;
import com.auction.model.enums.AuctionStatus;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Singleton quản lý toàn bộ các phiên đấu giá trong hệ thống.
 * <ul>
 *   <li>Lưu danh sách tất cả Auction (active + closed).</li>
 *   <li>Định kỳ kiểm tra và tự động đóng các phiên hết hạn.</li>
 *   <li>Hỗ trợ xử lý concurrent bids thông qua locking bên trong Auction.</li>
 * </ul>
 */
public class AuctionManager {

    // ── Singleton ─────────────────────────────────────────────────────────────

    private static volatile AuctionManager instance;

    private final List<Auction> activeAuctions;
    private final ScheduledExecutorService scheduler;

    private AuctionManager() {
        this.activeAuctions = new ArrayList<>();
        // Scheduler kiểm tra mỗi 5 giây
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "AuctionManager-Scheduler");
            t.setDaemon(true);
            return t;
        });
        scheduler.scheduleAtFixedRate(this::checkAndCloseExpiredAuctions, 5, 5, TimeUnit.SECONDS);
    }

    /**
     * Trả về instance duy nhất (thread-safe, double-checked locking).
     */
    public static AuctionManager getInstance() {
        if (instance == null) {
            synchronized (AuctionManager.class) {
                if (instance == null) {
                    instance = new AuctionManager();
                }
            }
        }
        return instance;
    }

    // ── Auction management ────────────────────────────────────────────────────

    /**
     * Đăng ký phiên đấu giá mới vào hệ thống.
     *
     * @param auction phiên cần thêm
     */
    public synchronized void registerAuction(Auction auction) {
        if (auction == null) throw new IllegalArgumentException("Auction không được null.");
        if (!activeAuctions.contains(auction)) {
            activeAuctions.add(auction);
            System.out.printf("[AuctionManager] Đã đăng ký phiên [%s] cho sản phẩm '%s'%n",
                    auction.getId(), auction.getItem().getName());
        }
    }

    /**
     * Bắt đầu một phiên đấu giá đã đăng ký.
     *
     * @param auction phiên cần bắt đầu
     */
    public void startAuction(Auction auction) {
        if (!activeAuctions.contains(auction)) {
            throw new IllegalArgumentException("Phiên chưa được đăng ký vào AuctionManager.");
        }
        auction.startAuction();
    }

    /**
     * Tự động kiểm tra và đóng các phiên đã hết thời gian.
     * Được gọi định kỳ bởi scheduler.
     */
    public synchronized void checkAndCloseExpiredAuctions() {
        for (Auction auction : activeAuctions) {
            if (auction.getStatus() == AuctionStatus.RUNNING && auction.isExpired()) {
                System.out.printf("[AuctionManager] Phiên [%s] hết hạn → tự động đóng.%n",
                        auction.getId());
                auction.endAuction();
            }
            // Chuyển OPEN → RUNNING nếu đến giờ bắt đầu
            if (auction.getStatus() == AuctionStatus.OPEN
                    && !auction.getStartTime().isAfter(java.time.LocalDateTime.now())) {
                auction.startAuction();
            }
        }
    }

    /**
     * Xử lý concurrent bid: delegate đến Auction.placeBid() đã có lock.
     * Phương thức này là entry point cho server khi nhận bid từ client.
     *
     * @param bid giao dịch đặt giá từ client
     * @return true nếu bid được chấp nhận
     */
    public boolean processConcurrentBid(BidTransaction bid) {
        if (bid == null) throw new IllegalArgumentException("BidTransaction không được null.");
        return bid.getAuction().placeBid(bid);
    }

    /**
     * Lấy danh sách các phiên đang RUNNING.
     */
    public synchronized List<Auction> getRunningAuctions() {
        List<Auction> running = new ArrayList<>();
        for (Auction a : activeAuctions) {
            if (a.getStatus() == AuctionStatus.RUNNING) running.add(a);
        }
        return Collections.unmodifiableList(running);
    }

    /**
     * Lấy toàn bộ danh sách phiên (bao gồm đã đóng).
     */
    public synchronized List<Auction> getAllAuctions() {
        return Collections.unmodifiableList(new ArrayList<>(activeAuctions));
    }

    /**
     * Tìm phiên theo id.
     *
     * @param id id cần tìm
     * @return Auction nếu tìm thấy, null nếu không
     */
    public synchronized Auction findById(String id) {
        return activeAuctions.stream()
                .filter(a -> a.getId().equals(id))
                .findFirst()
                .orElse(null);
    }

    /**
     * In tổng quan hệ thống (dùng cho Admin).
     */
    public synchronized void printSystemSummary() {
        System.out.println("=== Tổng quan hệ thống ===");
        System.out.printf("  Tổng số phiên: %d%n", activeAuctions.size());
        long running = activeAuctions.stream()
                .filter(a -> a.getStatus() == AuctionStatus.RUNNING).count();
        long finished = activeAuctions.stream()
                .filter(a -> a.getStatus() == AuctionStatus.FINISHED).count();
        long canceled = activeAuctions.stream()
                .filter(a -> a.getStatus() == AuctionStatus.CANCELED).count();
        System.out.printf("  RUNNING: %d | FINISHED: %d | CANCELED: %d%n",
                running, finished, canceled);
        System.out.println("=== Chi tiết từng phiên ===");
        activeAuctions.forEach(Auction::printInfo);
    }

    /**
     * Dừng scheduler khi shutdown ứng dụng.
     */
    public void shutdown() {
        scheduler.shutdown();
        System.out.println("[AuctionManager] Scheduler đã dừng.");
    }
}