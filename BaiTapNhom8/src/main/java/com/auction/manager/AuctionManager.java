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
 * Lưu danh sách tất cả Auction (active + closed)
 * Định kỳ kiểm tra và tự động đóng các phiên hết hạn
 * Hỗ trợ xử lý concurrent bids thông qua locking bên trong Auction
 */
public class AuctionManager {

    // ── Singleton ─────────────────────────────────────────────────────────────

    private static volatile AuctionManager instance;
    //Tác dụng của volatile ở đây giải quyết 2 bài toán sống còn khi có nhiều luồng cùng chạy một lúc


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
     * In tổng quan hệ thống (dùng cho Admin)
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
    /*
     * Hàm này chỉ là in dữ liệu ra màn hình (Read) thôi mà, có thay đổi hay thêm bớt (Write) cái gì đâu
     * Tại sao lại phải cất công gắn thêm cái ổ khóa synchronized làm gì cho hệ thống bị chậm đi?
     *Có phải làm một việc là: chạy vòng lặp qua toàn bộ danh sách activeAuctions để đếm số lượng và in thông tin từng phiên ra đk?
     * activeAuctions được khởi tạo bằng ArrayList
     * Trong Java, ArrayList :Nó cực kỳ ghét việc bị ai đó thêm/bớt dữ liệu trong lúc nó đang được người khác duyệt qua (vòng lặp)
     * Giả sử không có synchronized ở hàm này
     * Thằng Admin gõ lệnh xem báo cáo. Hệ thống bắt đầu chạy vòng lặp in đến phiên đấu giá thứ 10
     * Cùng đúng cái tích tắc đó mili-giây đó, ông Seller bấm nút tạo thêm một phiên đấu giá thứ 100
     * Danh sách activeAuctions bị thay đổi kích thước đột ngột. Vòng lặp đang chạy của Admin bị hẫng nhịp
     * => Java văng ra lỗi java.util.ConcurrentModificationException
     * */

    /*
     * Nếu muốn xem có bao nhiêu phiên đấu giá đang RUNNING/FINISHED/CANCELD/OPEN/PAID
     * Cách cũ trước Java8 thì phải dùng for đúng không
     *
     * long runningCount = 0;
     * for (Auction a : activeAuctions) {
     *     if (a.getStatus() == AuctionStatus.RUNNING) {runningCount++;}
     * }
     *
     *  => Mất nhiều dòng cho một việc đơn giản
     *
     * Từ Java 8 trở đi thì thêm phương thức stream(), filter(...), count()
     * Cứ tưởng tượng như là 1 cái băng truyền đi:
     * stream() - Lôi tất cả các phiên đấu giá trong danh sách activeAuctions ra và xếp chúng lên băng chuyền
     * filter(...) - Đây là cái phễu lọc. Tại đây, bạn đặt ra một quy tắc (dùng Lambda) VD: a.getStatus() == AuctionStatus.RUNNING
     * Dây chuyền sẽ soi từng phiên đấu giá, phiên nào đúng RUNNING thì cho đi tiếp, phiên nào là FINISHED hay CANCELED thì bị vứt đi
     * count() - Ở cuối băng truyền, đếm xem có phieen đấu giá đã được lọc
     * */

    /**
     * Dừng scheduler khi shutdown ứng dụng.
     */
    public void shutdown() {
        scheduler.shutdown();
        System.out.println("[AuctionManager] Scheduler đã dừng.");
    }
}