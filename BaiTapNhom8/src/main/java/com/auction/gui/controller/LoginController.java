package com.auction.gui.controller;

import com.auction.Main;
import com.auction.gui.SessionManager;
import com.auction.gui.UserStore;
import com.auction.model.entity.Bidder;
import com.auction.model.entity.Seller;
import com.auction.model.entity.User;
import javafx.fxml.FXML;
import javafx.scene.control.*;

public class LoginController {

    // Login fields
    @FXML private TextField loginUsername;
    @FXML private PasswordField loginPassword;
    @FXML private Label loginError;

    // Register fields
    @FXML private TextField regUsername;
    @FXML private PasswordField regPassword;
    @FXML private TextField regEmail;
    @FXML private ComboBox<String> regRole;
    @FXML private Label regError;

    @FXML
    public void initialize() {
        regRole.getItems().addAll("Bidder", "Seller");
        regRole.setValue("Bidder");
    }

    @FXML
    private void handleLogin() {
        String username = loginUsername.getText().trim();
        String password = loginPassword.getText();

        if (username.isEmpty() || password.isEmpty()) {
            setError(loginError, "Vui lòng nhập đầy đủ thông tin.");
            return;
        }

        User user = UserStore.findByUsername(username);
        if (user == null || !user.login(username, password)) {
            setError(loginError, "Sai tên đăng nhập hoặc mật khẩu.");
            return;
        }

        SessionManager.setCurrentUser(user);
        try {
            Main.showMain();
        } catch (Exception e) {
            setError(loginError, "Lỗi khi mở màn hình chính: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void handleRegister() {
        String username = regUsername.getText().trim();
        String password = regPassword.getText();
        String email    = regEmail.getText().trim();
        String role     = regRole.getValue();

        if (username.isEmpty() || password.isEmpty() || email.isEmpty()) {
            setError(regError, "Vui lòng nhập đầy đủ thông tin.");
            return;
        }
        if (UserStore.usernameExists(username)) {
            setError(regError, "Tên đăng nhập đã tồn tại.");
            return;
        }

        try {
            User newUser;
            if ("Bidder".equals(role)) {
                // Tự động gán số dư = 0 cho Bidder mới
                newUser = new Bidder(username, password, email, 0);
            } else {
                newUser = new Seller(username, password, email);
            }
            UserStore.addUser(newUser);
            regError.setStyle("-fx-text-fill: #27ae60;");
            regError.setText("Đăng ký thành công! Hãy chuyển sang tab Đăng nhập.");
            regUsername.clear(); regPassword.clear(); regEmail.clear();
        } catch (IllegalArgumentException e) {
            setError(regError, e.getMessage());
        }
    }

    private void setError(Label label, String msg) {
        label.setStyle("-fx-text-fill: #e74c3c;");
        label.setText(msg);
    }
}