package com.auction.gui;

import com.auction.model.entity.User;
import com.auction.network.dto.UserDto; // Cần import UserDto

/**
 * Quản lý phiên đăng nhập hiện tại.
 */
public class SessionManager {
    private static User currentUser;         // Dùng cho logic cũ (có thể xóa sau khi refactor xong)
    private static UserDto currentUserDto;   // Dùng cho kiến trúc mạng mới (Client-Server)

    // --- CÁC PHƯƠNG THỨC CŨ ---
    public static User getCurrentUser() { return currentUser; }
    public static void setCurrentUser(User user) { currentUser = user; }

    // --- THÊM CÁC PHƯƠNG THỨC MỚI DÀNH CHO DTO ĐỂ HẾT BÁO ĐỎ ---
    public static UserDto getCurrentUserDto() { return currentUserDto; }
    public static void setCurrentUserDto(UserDto userDto) { currentUserDto = userDto; }

    public static void logout() {
        currentUser = null;
        currentUserDto = null; // Xóa cả DTO khi đăng xuất
    }

    public static boolean isLoggedIn() {
        return currentUser != null || currentUserDto != null;
    }
}