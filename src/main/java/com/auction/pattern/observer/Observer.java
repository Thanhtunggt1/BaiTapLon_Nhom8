package com.auction.pattern.observer;

import com.auction.model.entity.Auction;

/**
 * Observer Pattern — phía lắng nghe.
 * Bidder implement interface này để nhận thông báo khi có bid mới
 */
public interface Observer {

    /**
     * Được gọi khi Subject (Auction) có thay đổi trạng thái.
     *
     * @param auction phiên đấu giá vừa cập nhật
     */
    void update(Auction auction);
}