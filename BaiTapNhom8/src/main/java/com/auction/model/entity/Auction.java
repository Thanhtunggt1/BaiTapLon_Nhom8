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
 * Phiên đấu giá — trung tâm của hệ thống
 * Implement Subject để hỗ trợ Observer Pattern (realtime update)
 * Sử dụng ReentrantLock để xử lý Concurrent Bidding an toàn
 * Hỗ trợ Anti-sniping: tự động gia hạn khi có bid trong X giây cuối
 * Hỗ trợ Auto-Bidding thông qua PriorityQueue theo thời gian đăng ký
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
    /*
    * Hàng đợi thường (Queue): Giống như đi xếp hàng mua vé xem phim. Ai đến trước thì được mua trước, ai đến sau đứng xếp sau
    * Hàng đợi ưu tiên (PriorityQueue): Dễ hình dung thì cho VD như này nhá:
    * Có 3 đại gia cùng cài đặt Auto-Bid cho 1 sản phẩm
    * Đại gia A: Cài lúc 8h sáng, ngân sách tối đa (maxBid) = 100 triệu
    * Đại gia B: Cài lúc 9h sáng, ngân sách tối đa (maxBid) = 500 triệu
    * Đại gia C: Cài lúc 10h sáng, ngân sách tối đa (maxBid) = 50 triệu
    * Nếu dùng ArrayList hoặc Queue, hệ thống sẽ ưu tiên xử lý ông A (đến sớm nhất)
    * Nhưng như thế là sai logic kinh doanh. Ông B sẵn sàng trả giá cao nhất, ông B phải là khách VIP được hệ thống ưu tiên
    * Đó là lý do có PriorityQueue
    * Java sẽ tự động so sánh nó với các tờ AutoBidConfig (dựa trên một bộ luật tự viết, VD: Ai có maxBid cao hơn thì người đó có mức ưu tiên cao hơn)
    * Để cái PriorityQueue này hoạt động mà không bị báo lỗi đỏ lòm
    * Lớp AutoBidConfig bắt buộc phải dạy cho Java biết thế nào là ưu tiên bằng cách implements Comparable<AutoBidConfig> và override compareTo()
    *
    * */

    // ── Concurrency ───────────────────────────────────────────────────────────
    private final ReentrantLock bidLock = new ReentrantLock();
    /*
     * Từ "Re-entrant" dịch nôm na là "Được phép vào lại"
     * ReentrantLock trong Java là một cơ chế khóa (lock) nâng cao, thay thế cho từ khóa synchronized
     * Cho phép kiểm soát đồng bộ trong môi trường đa luồng với nhiều tính năng linh hoạt hơn
     *
     * Tưởng tượng ta có đúng 1 chiếc chìa khóa và một căn phòng:
     * Dùng chìa khóa đó để mở cửa chính bước vào phòng, sau đó chốt cửa lại từ bên trong (để không ai khác được vào). Chiếc chìa khóa lúc này đang cắm ở ổ khóa cửa chính
     * Đang đứng trong phòng, ta nhìn thấy một cái Két Sắt. Nhưng cái Két Sắt này yêu cầu đúng chiếc chìa khóa cửa chính kia mới mở được
     * Ta không thể ra rút chìa ở cửa chính được (vì rút ra là cửa mở, người khác xông vào mất) -> Chôn chân
     *
     * Với ReentrantLock:
     * Nó sẽ tự hiểu là mình đã đóng cửa rồi -> Tức là có chìa khóa của chính nó
     * Két sắt sẽ tự động ở ra ma không cần rút chìa khóa ở cửa chính
     * */

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
     * Bắt đầu phiên đấu giá (OPEN → RUNNING)
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
    /*
    * Phương thức startAuction() có vẻ rất đơn giản: chỉ là đổi trạng thái và in ra màn hình
    * Vậy tại sao lại phải cất công gắn cái "ổ khóa" synchronized vào làm gì?
    *
    * Nếu không có synchronized:
    * Nhịp 1 (Check): Kiểm tra xem cửa đã mở chưa if (status != AuctionStatus.OPEN)
    * Nhịp 2 (Act): Đổi trạng thái sang đang chạy status = RUNNING; và gọi notifyObservers()
    * Hệ thống của bạn có tính năng cho phép Admin bấm nút "Bắt đầu" (OPEN -> RUNNING)
    * Đồng thời cũng có một bộ đếm giờ tự động (Timer) kích hoạt phiên đấu giá khi đến giờ G
    * Giả dụ Admin cũng ấn Bắt đầu vào cùng thời điểm G
    * Luồng của Admin chạy đến Nhịp 1, thấy status đang là OPEN - Hợp lệ, -> RUNNING
    * Luồng của Timer cũng chạy đến Nhịp 1. Vì Luồng Admin chưa kịp đổi trạng thái, Timer vẫn thấy status đang là OPEN - Cũng hợp lệ, -> RUNNING
    * Cả 2 luồng cùng tràn xuống Nhịp 2
    * Hàm notifyObservers() bị gọi 2 lần liên tiếp
    * Toàn bộ người tham gia (Bidder) sẽ bị hệ thống spam 2 cái email/thông báo báo hiệu phiên đấu giá bắt đầu
    *
    * */

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
    /*
    * Lý dó cũng dùng synchonized cũng giống như lý do startAuction
    * Từ VD không dùng synchonized của startAuction
    * Thì dòng thông báo kết thúc sẽ được in ra 2 lần nếu cùng kết thúc ở cùng thời điểm
    * */

    /**
     * Hủy phiên đấu giá (dùng cho Admin hoặc khi có tranh chấp)
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
     * Tự động trừ tiền của người dẫn đầu.
     */
    public synchronized void markAsPaid() {
        if (status != AuctionStatus.FINISHED) {
            throw new IllegalStateException("Chỉ phiên FINISHED mới có thể đánh dấu PAID.");
        }

        // GỌI HÀM TRỪ TIỀN (DEDUCT) ĐÃ CÓ SẴN TRONG BIDDER
        if (currentLeader != null) {
            try {
                currentLeader.deduct(currentHighestPrice);

                System.out.printf("[Payment] Đã trừ %.2f VNĐ từ tài khoản %s%n",
                        currentHighestPrice, currentLeader.getUsername());
            } catch (Exception e) {
                // Bắt lỗi nếu Bidder không đủ tiền
                /*
                * Lý do có hàm này
                * Giả dụ có 2 phiên đã FINISHED và cùng 1 Bidder dẫn đầu
                * Bidder đó đã trả tiền cho 1 phiên nhưng lại không có đủ tiền cho phiên con lại
                * */
                throw new IllegalStateException("Thanh toán thất bại! Tài khoản [" + currentLeader.getUsername() + "] báo lỗi: " + e.getMessage());
            }
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
    /*
    * Nếu không có khóa, VD:
    * Giá hiện tại: 100$
    * Người A trả 110$, Người B trả 105$ cùng một lúc
    * Cả hai luồng A và B cùng đọc thấy giá là 100$. Cả hai đều thấy giá mình trả (110 và 105) là hợp lệ
    * Luồng A cập nhật giá lên 110$. Ngay sau đó, luồng B đè lên và cập nhật giá thành 105$
    * Một người trả giá thấp hơn 105$ lại chiến thắng người trả giá cao hơn 110$
    * Nó cũng tránh việc lỗi nếu dunùng bidHistory.add(newTransaction)
    * Nếu nhiều luồng cùng gọi hàm add() vào một danh sách ArrayList cùng lúc văng lỗi ConcurrentModificationException
    *
    * Vậy tại sao không dùng synchronized
    * Giả dụ: Ở những giây cuối cùng, có thể có 50 người cùng bấm nút "Đặt giá"
    *
    * Nếu dùng synchronized:
    * synchronized hoạt động như một cuộc bốc thăm trúng thưởng
    * Java sẽ chọn ngẫu nhiên 1 trong 50 người để cho vào, không quan tâm ai đến trước, ai đến sau
    *
    * Với ReentrantLock: có một tính chất là tính Công bằng
    * 50 người sẽ bị ép xếp thành một hàng dọc theo đúng thứ tự mili-giây họ bấm nút
    * Ai bấm trước chắc chắn 100% sẽ được hệ thống xử lý trước
    *
    * */

    /**
     * Anti-sniping: nếu còn ít hơn SNIPE_WINDOW_SECONDS giây → gia hạn thêm.
     */
    private void checkAndExtend() {
        LocalDateTime now = LocalDateTime.now();
        if (now.isAfter(endTime.minusSeconds(SNIPE_WINDOW_SECONDS))) {
            extendTime(EXTENSION_SECONDS);
            // endTime.minusSeconds(SNIPE_WINDOW_SECONDS): endTime - SNIPE_WINDOW_SECONDS
            // isAfter(...) xem thời gian (now) qua thời gian (endTime - SNIPE_WINDOW_SECONDS) chưa
        }
    }

    /**
     * Gia hạn thời gian kết thúc phiên
     * @param seconds số giây gia hạn thêm
     */
    public void extendTime(int seconds) {
        if (seconds <= 0) throw new IllegalArgumentException("Số giây gia hạn phải dương.");
        endTime = endTime.plusSeconds(seconds);
        System.out.printf("[Anti-snipe] Phiên [%s] được gia hạn thêm %d giây → Kết thúc lúc %s%n",
                getId(), seconds, endTime);
    }

    /**
     * Kích hoạt auto-bid từ hàng đợi sau mỗi bid hợp lệ
     * Bỏ qua AutoBidConfig của bidder vừa thắng để tránh tự đấu với chính mình
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
    /*
     * Lý do có phương thức triggerAutoBids()
     * VD: Giả sử hàm chỉ trơ trọi là triggerAutoBids()
     * Bidder cài Auto-Bid với ngân sách 50 triệu bước nhảy là 1 triệu
     * Đang rảnh rỗi, Bidder đó vào bấm nút đặt giá bằng tay là 30 triệu. Hệ thống ghi nhận Bidder đó đang dẫn đầu
     * Hàm triggerAutoBids() được gọi một cách mù quáng
     * Hệ thống lập tức tính toán: Giá mới = 30 + 1 = 31 triệu
     * */

    // ── Auto-bid registration ─────────────────────────────────────────────────

    /**
     * Đăng ký cấu hình auto-bid
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

    /*
    * Khi các hàm cùng chọc vào một tài nguyên chung (ở đây là autoBidQueue)
    * Chúng bắt buộc phải dùng chung một chiếc chìa khóa
    * Đã lỡ dùng ReentrantLock cho placeBid thì registerAutoBid cũng phải dùng đúng cái bidLock đó để đảm bảo chúng chặn được nhau
    *
    *Giả dụ:
    * Khi dùng từ khóa synchronized ở tên hàm, Java sẽ ngầm định dùng chìa khóa mặc định của class (tức là ổ khóa this).
    * Tạm gọi đây là Khóa Đỏ
    *
    * Trong khi đó, ở hàm placeBid (hàm đặt giá), bạn lại đang dùng bidLock.lock()
    * Tạm gọi đây là Khóa Xanh
    *
    * Luồng 1 (Người A): Đang thực hiện placeBid
    * Nó chốt Khóa Xanh lại, bước vào phòng và bắt đầu lật cuốn sổ autoBidQueue ra để đọc do có sử dụng triggerAutoBid() mà nó có dùng đến ds autoBidQueue
    *
    * Luồng 2 (Người B): Cùng lúc đó, bấm nút đăng ký Auto-Bid. Luồng này chạy vào hàm registerAutoBid
    * Hệ thống kiểm tra xem Khóa Đỏ có ai dùng không? Câu trả lời là không (vì Người A đang cầm Khóa Xanh)
    *
    * Thế là hệ thống mở cửa cho Người B
    * Người B chạy ngay dòng lệnh removeIf(...) — tức là xé bỏ dữ liệu trong cuốn sổ
    * Hậu quả: Người A đang lật sổ ra đọc, Người B lao vào xé sổ. Java sẽ lập tức ném ra lỗi ConcurrentModificationException
    * */

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
     * Kiểm tra phiên có hết hạn chưa dựa vào thời gian thực
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

    public double getCurrentHighestPrice() {
        // Nếu danh sách lịch sử đặt giá trống (chưa có ai bid)
        if (bidHistory == null || bidHistory.isEmpty()) {
            // Luôn luôn lấy giá gốc mới nhất từ đối tượng Item
            return item.getStartingPrice();
        }
        // Nếu đã có người bid, thì trả về biến giá cao nhất đang được lưu
        return currentHighestPrice;
    }

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