package com.auction.model.entity;

import com.auction.model.enums.AuctionStatus;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class AdminExtraTest {

    private Seller createSeller() {
        return new Seller("seller1", "123456", "seller@gmail.com");
    }

    private Art createItem() {
        return new Art("Tranh cổ", "Tranh đẹp", 100.0, "Picasso", 1990);
    }

    private Auction createAuction() {
        return new Auction(
                createItem(),
                createSeller(),
                LocalDateTime.now().minusMinutes(1),
                LocalDateTime.now().plusMinutes(10)
        );
    }

    @Test
    void adminShouldBeCreatedSuccessfully() {
        Admin admin = new Admin("admin1", "123456", "admin@gmail.com");

        assertNotNull(admin.getId());
        assertEquals("admin1", admin.getUsername());
        assertEquals("admin@gmail.com", admin.getEmail());
    }

    @Test
    void adminUsernameCannotBeBlank() {
        assertThrows(IllegalArgumentException.class, () ->
                new Admin("", "123456", "admin@gmail.com")
        );
    }

    @Test
    void adminPasswordMustHaveAtLeastSixCharacters() {
        assertThrows(IllegalArgumentException.class, () ->
                new Admin("admin1", "123", "admin@gmail.com")
        );
    }

    @Test
    void adminEmailMustBeValid() {
        assertThrows(IllegalArgumentException.class, () ->
                new Admin("admin1", "123456", "wrong-email")
        );
    }

    @Test
    void adminCanCancelOpenAuction() {
        Admin admin = new Admin("admin1", "123456", "admin@gmail.com");
        Auction auction = createAuction();

        admin.resolveDispute(auction, "Sản phẩm không hợp lệ");

        assertEquals(AuctionStatus.CANCELED, auction.getStatus());
    }

    @Test
    void adminCanCancelRunningAuction() {
        Admin admin = new Admin("admin1", "123456", "admin@gmail.com");
        Auction auction = createAuction();

        auction.startAuction();
        admin.resolveDispute(auction, "Có tranh chấp");

        assertEquals(AuctionStatus.CANCELED, auction.getStatus());
    }

    @Test
    void adminCannotResolveNullAuction() {
        Admin admin = new Admin("admin1", "123456", "admin@gmail.com");

        assertThrows(IllegalArgumentException.class, () ->
                admin.resolveDispute(null, "Lỗi phiên đấu giá")
        );
    }

    @Test
    void adminResolvePaidAuctionShouldNotCancelIt() {
        Admin admin = new Admin("admin1", "123456", "admin@gmail.com");
        Auction auction = createAuction();
        Bidder bidder = new Bidder("bidder1", "123456", "bidder@gmail.com", 1000.0);

        auction.startAuction();

        assertDoesNotThrow(() -> bidder.placeBid(auction, 200.0));
        auction.endAuction();
        auction.markAsPaid();

        admin.resolveDispute(auction, "Đã thanh toán rồi");

        assertEquals(AuctionStatus.PAID, auction.getStatus());
    }

    @Test
    void twoAdminsShouldHaveDifferentIds() {
        Admin admin1 = new Admin("admin1", "123456", "admin1@gmail.com");
        Admin admin2 = new Admin("admin2", "123456", "admin2@gmail.com");

        assertNotEquals(admin1.getId(), admin2.getId());
    }

    @Test
    void adminCanPrintInfoWithoutException() {
        Admin admin = new Admin("admin1", "123456", "admin@gmail.com");

        assertDoesNotThrow(admin::printInfo);
    }
}
