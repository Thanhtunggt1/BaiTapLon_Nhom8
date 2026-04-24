package com.auction.pattern.observer;

/**
 * Observer Pattern — phía phát sự kiện.
 * Auction implement interface này để quản lý danh sách Observer
 */
public interface Subject {

    /**
     * Đăng ký một Observer vào danh sách theo dõi
     *  observer observer cần thêm
     */
    void attach(Observer observer);

    /**
     * Hủy đăng ký một Observer khỏi danh sách theo dõi.
     *
     * @param observer observer cần xóa
     */
    void detach(Observer observer);

    /**
     * Thông báo đến toàn bộ Observer hiện tại
     */
    void notifyObservers();
}