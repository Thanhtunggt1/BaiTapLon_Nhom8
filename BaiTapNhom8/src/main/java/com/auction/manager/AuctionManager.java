package com.auction.manager;

import com.auction.model.entity.Auction;
import com.auction.model.enums.AuctionStatus;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class AuctionManager {

    private static volatile AuctionManager instance;

    private final List<Auction> activeAuctions;

    private AuctionManager() {
        this.activeAuctions = new ArrayList<>();
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "AuctionManager-Scheduler");
            t.setDaemon(true);
            return t;
        });
        scheduler.scheduleAtFixedRate(this::checkAndCloseExpiredAuctions, 5, 5, TimeUnit.SECONDS);
    }

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

    public synchronized void registerAuction(Auction auction) {
        if (auction == null) throw new IllegalArgumentException("Auction không được null.");
        if (!activeAuctions.contains(auction)) {
            activeAuctions.add(auction);
            System.out.printf("[AuctionManager] Đã đăng ký phiên [%s] cho sản phẩm '%s'%n",
                    auction.getId(), auction.getItem().getName());
        }
    }

    public void startAuction(Auction auction) {
        if (!activeAuctions.contains(auction)) {
            throw new IllegalArgumentException("Phiên chưa được đăng ký vào AuctionManager.");
        }
        auction.startAuction();
    }

    public synchronized void checkAndCloseExpiredAuctions() {
        LocalDateTime now = LocalDateTime.now();
        for (Auction auction : activeAuctions) {
            if (auction.getStatus() == AuctionStatus.RUNNING && auction.isExpired()) {
                System.out.printf("[AuctionManager] Phiên [%s] hết hạn → tự động đóng.%n",
                        auction.getId());
                auction.endAuction();
            }
            if (auction.getStatus() == AuctionStatus.OPEN
                    && !auction.getStartTime().isAfter(now)) {
                auction.startAuction();
            }
            if (auction.getStatus() == AuctionStatus.FINISHED && auction.getFinishedTime() != null) {
                if (now.isAfter(auction.getFinishedTime().plusHours(12))) {
                    System.out.printf("[AuctionManager] Phiên [%s] quá hạn thanh toán 12h → tự động HỦY và Phạt.%n",
                            auction.getId());
                    auction.cancelDueToUnpaid();
                    com.auction.server.AuctionDAO.updateAuctionStatus(auction.getId(), "CANCELED");
                }
            }
        }
    }

    public synchronized List<Auction> getAllAuctions() {
        return List.copyOf(activeAuctions);
    }
}