package com.auction.gui;

import com.auction.model.entity.User;
import com.auction.network.dto.UserDto; // Cần import UserDto


public class SessionManager {
    private static User currentUser;
    private static UserDto currentUserDto;

    public static User getCurrentUser() { return currentUser; }
    public static void setCurrentUser(User user) { currentUser = user; }

    public static UserDto getCurrentUserDto() { return currentUserDto; }
    public static void setCurrentUserDto(UserDto userDto) { currentUserDto = userDto; }

    public static void logout() {
        currentUser = null;
        currentUserDto = null;
    }

}