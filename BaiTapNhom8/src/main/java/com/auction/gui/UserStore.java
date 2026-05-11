package com.auction.gui;

import com.auction.model.entity.User;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class UserStore {
    private static final List<User> users = new ArrayList<>();

    public static void addUser(User user) { users.add(user); }

    public static User findByUsername(String username) {
        return users.stream()
            .filter(u -> u.getUsername().equals(username))
            .findFirst().orElse(null);
    }

    public static List<User> getAllUsers() {
        return Collections.unmodifiableList(users);
    }

}