package com.auction.model.entity;

/**
 * Lớp trừu tượng đại diện cho người dùng trong hệ thống
 * Bidder, Seller, Admin kế thừa lớp này
 */
public abstract class User extends Entity {

    private String username;
    private String password;   // Lưu dạng hash trong thực tế
    private String email;
    private boolean loggedIn; // Để xem hiện có đang trong trạng thái đã đăng nhập hay chưa


    // Bản chất cái contructor này chẳng khác gì việc tạo tài khoản nên cũng chẳng cần phương thức register
    protected User(String username, String password, String email) {
        super();
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("Username không được để trống.");
        }
        if (password == null || password.length() < 6) {
            throw new IllegalArgumentException("Password phải có ít nhất 6 ký tự.");
        }

        // Nếu không có dấu @ thì không là email
        if (email == null || !email.contains("@")) {
            throw new IllegalArgumentException("Email không hợp lệ.");
        }
        this.username = username;
        this.password = password;
        this.email = email;
        this.loggedIn = false;
    }

    // ── Business methods ─────────────────────────────────────────────────────

    /**
     * Đăng nhập với username và password.
     *
     * @param username tên đăng nhập
     * @param password mật khẩu
     * @return true nếu thành công
     */
    public boolean login(String username, String password) {
        if (this.username.equals(username) && this.password.equals(password)) {
            this.loggedIn = true;
            System.out.println("[Auth] " + username + " đã đăng nhập.");
            return true;
        }
        System.out.println("[Auth] Sai username hoặc password.");
        return false;
    }

    /**
     * Đăng xuất khỏi hệ thống.
     */
    public void logout() {
        if (loggedIn) {
            loggedIn = false;
            System.out.println("[Auth] " + username + " đã đăng xuất.");
        }
    }

    /**
     * In thông tin người dùng — subclass có thể override để thêm chi tiết.
     */
    public void printInfo() {
        System.out.printf("[%s] id=%s | username=%s | email=%s | loggedIn=%b%n",
                getClass().getSimpleName(), getId(), username, email, loggedIn);
    }

    // ── Getters / Setters ────────────────────────────────────────────────────

    public String getUsername() { return username; }

    public void setUsername(String username) {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("Username không được để trống.");
        }
        this.username = username;
    }

    public String getPassword() { return password; }

    public void setPassword(String password) {
        if (password == null || password.length() < 6) {
            throw new IllegalArgumentException("Password phải có ít nhất 6 ký tự.");
        }
        this.password = password;
    }

    public String getEmail() { return email; }

    public void setEmail(String email) {
        if (email == null || !email.contains("@")) {
            throw new IllegalArgumentException("Email không hợp lệ.");
        }
        this.email = email;
    }

    public boolean isLoggedIn() { return loggedIn; }
}