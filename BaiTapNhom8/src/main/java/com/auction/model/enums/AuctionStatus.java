package com.auction.model.enums;


public enum AuctionStatus {
    /** Phiên đã được tạo, chưa bắt đầu */
    OPEN,

    /** Phiên đang diễn ra, chấp nhận bid */
    RUNNING,

    /** Phiên đã kết thúc, chờ thanh toán */
    FINISHED,

    /** Người thắng đã thanh toán thành công */
    PAID,

    /** Phiên bị hủy (không có bid / lỗi) */
    CANCELED
}