package com.auction.manager;

import com.auction.model.entity.Auction;
import com.auction.model.entity.DepositRequest;
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
    private final List<DepositRequest> depositRequests;

    private AuctionManager() {
        this.activeAuctions = new ArrayList<>();
        this.depositRequests = new ArrayList<>();

        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread t = new Thread(runnable, "AuctionManager-Scheduler");
            t.setDaemon(true);
            return t;
        });

        scheduler.scheduleAtFixedRate(
                this::checkAndCloseExpiredAuctions,
                5,
                5,
                TimeUnit.SECONDS
        );
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
        if (auction == null) {
            throw new IllegalArgumentException("Auction không được null.");
        }

        if (!activeAuctions.contains(auction)) {
            activeAuctions.add(auction);
            System.out.printf(
                    "[AuctionManager] Đã đăng ký phiên [%s] cho sản phẩm '%s'%n",
                    auction.getId(),
                    auction.getItem().getName()
            );
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
                System.out.printf("[AuctionManager] Phiên [%s] hết hạn -> tự động đóng.%n",
                        auction.getId());
                auction.endAuction();
            }

            if (auction.getStatus() == AuctionStatus.OPEN
                    && !auction.getStartTime().isAfter(now)) {
                auction.startAuction();
            }

            if (auction.getStatus() == AuctionStatus.FINISHED
                    && auction.getFinishedTime() != null) {

                if (now.isAfter(auction.getFinishedTime().plusHours(12))) {
                    System.out.printf(
                            "[AuctionManager] Phiên [%s] quá hạn thanh toán 12h -> tự động HỦY.%n",
                            auction.getId()
                    );
                    auction.cancelAuction();
                }
            }
        }
    }

    public synchronized List<Auction> getAllAuctions() {
        return List.copyOf(activeAuctions);
    }

    public synchronized void addDepositRequest(DepositRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("DepositRequest không được null.");
        }

        depositRequests.add(request);

        System.out.printf(
                "[AuctionManager] Đã thêm yêu cầu nạp %.2f của bidder %s%n",
                request.getAmount(),
                request.getBidder().getUsername()
        );
    }

    public synchronized List<DepositRequest> getDepositRequests() {
        return List.copyOf(depositRequests);
    }
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

