package com.auction.model.entity;

import com.auction.model.enums.AuctionStatus;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class AdminTest {

    @Test
    void testResolveDispute_CancelRunningAuction() {
        Admin admin = new Admin("super_admin", "admin123", "admin@uet.edu.vn");
        Seller seller = new Seller("s", "123456", "s@s.com");
        Item item = new Item("Bình gốm", "Đồ cổ", 500) {};
        Auction auction = new Auction(item, seller, LocalDateTime.now(), LocalDateTime.now().plusDays(1));

        auction.startAuction();

        // Admin nhảy vào hủy phiên đang chạy
        admin.resolveDispute(auction, "Phát hiện hàng giả");

        assertEquals(AuctionStatus.CANCELED, auction.getStatus(), "Phiên phải chuyển sang CANCELED khi Admin can thiệp");
    }

    @Test
    void testResolveDispute_CannotCancelPaidAuction() {
        Admin admin = new Admin("super_admin", "admin123", "admin@uet.edu.vn");
        Seller seller = new Seller("s", "123456", "s@s.com");
        Item item = new Item("Bình gốm", "Đồ cổ", 500) {};
        Auction auction = new Auction(item, seller, LocalDateTime.now(), LocalDateTime.now().plusDays(1));

        Bidder bidder = new Bidder("b", "123456", "b@b.com", 2000.0);

        auction.startAuction();
        bidder.placeBid(auction, 1000.0); // Đặt giá thành công
        auction.endAuction(); // Kết thúc phiên (Chuyển sang FINISHED)
        auction.markAsPaid(); // Trừ tiền (Chuyển sang PAID)

        // Cố gắng nhờ Admin hủy một phiên đã thanh toán xong xuôi
        admin.resolveDispute(auction, "Đổi ý");

        assertEquals(AuctionStatus.PAID, auction.getStatus(), "Admin không thể hủy một phiên đã PAID");
    }
}