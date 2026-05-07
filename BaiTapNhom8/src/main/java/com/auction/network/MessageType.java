package com.auction.network;

public enum MessageType {
    // ── Auth ──────────────────────────────────────────
    LOGIN,              // Client → Server: đăng nhập
    LOGIN_RESPONSE,     // Server → Client: kết quả đăng nhập

    REGISTER,           // Client → Server: đăng ký
    REGISTER_RESPONSE,  // Server → Client: kết quả đăng ký

    // ── Auction ───────────────────────────────────────
    GET_AUCTIONS,       // Client → Server: lấy danh sách phiên
    AUCTIONS_RESPONSE,  // Server → Client: trả về danh sách

    PLACE_BID,          // Client → Server: đặt giá
    BID_RESPONSE,       // Server → Client: kết quả đặt giá

    BID_UPDATE,         // Server → ALL clients: có bid mới (push)

    // ── Seller ────────────────────────────────────────
    CREATE_ITEM,        // Client → Server
    CREATE_ITEM_RESPONSE,

    CREATE_AUCTION,     // Client → Server
    CREATE_AUCTION_RESPONSE,

    END_AUCTION,        // Client → Server: kết thúc phiên sớm
    CANCEL_AUCTION,

    // ── Admin ─────────────────────────────────────────
    ADMIN_CANCEL_AUCTION,

    // ── Payment ───────────────────────────────────────
    MARK_PAID,
    MARK_PAID_RESPONSE,

    // ── Deposit ───────────────────────────────────────
    DEPOSIT,
    DEPOSIT_RESPONSE,

    // ── Error ─────────────────────────────────────────
    ERROR               // Server → Client: thông báo lỗi chung
}