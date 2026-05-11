package com.auction.pattern.observer;

import com.auction.model.entity.Auction;

public interface Observer {

    void update(Auction auction);
}