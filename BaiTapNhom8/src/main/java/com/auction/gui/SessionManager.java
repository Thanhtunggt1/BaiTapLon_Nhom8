package com.auction.gui;

import com.auction.model.entity.User;

/**
 * Quản lý phiên đăng nhập hiện tại.
 */
public class SessionManager {
    private static User currentUser;

    public static User getCurrentUser() { return currentUser; }
    public static void setCurrentUser(User user) { currentUser = user; }
    public static void logout() { currentUser = null; }
    public static boolean isLoggedIn() { return currentUser != null; }
}