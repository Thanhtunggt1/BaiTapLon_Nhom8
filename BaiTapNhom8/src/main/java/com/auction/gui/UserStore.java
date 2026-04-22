package com.auction.gui;

import com.auction.model.entity.Bidder;
import com.auction.model.entity.Seller;
import com.auction.model.entity.User;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Kho lưu trữ người dùng trong bộ nhớ (thay cho database).
 */
public class UserStore {
    private static final List<User> users = new ArrayList<>();

    public static void addUser(User user) { users.add(user); }

    public static User findByUsername(String username) {
        return users.stream()
            .filter(u -> u.getUsername().equals(username))
            .findFirst().orElse(null);
    }

    public static boolean usernameExists(String username) {
        return findByUsername(username) != null;
    }

    public static List<User> getAllUsers() {
        return Collections.unmodifiableList(users);
    }

    public static List<Bidder> getAllBidders() {
        return users.stream()
            .filter(u -> u instanceof Bidder)
            .map(u -> (Bidder) u)
            .collect(Collectors.toList());
    }

    public static List<Seller> getAllSellers() {
        return users.stream()
            .filter(u -> u instanceof Seller)
            .map(u -> (Seller) u)
            .collect(Collectors.toList());
    }
}