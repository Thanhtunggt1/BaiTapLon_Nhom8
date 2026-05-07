package com.auction.gui.controller;

import com.auction.Main;
import com.auction.gui.SessionManager;
import com.auction.network.NetworkClient;
import com.auction.network.Message;
import com.auction.network.dto.UserDto;
import javafx.fxml.FXML;
import javafx.scene.control.*;

public class LoginController {
    @FXML private TextField loginUsername;
    @FXML private PasswordField loginPassword;
    @FXML private Label loginError;

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

        // Gửi yêu cầu đăng nhập qua mạng
        Message response = NetworkClient.getInstance().login(username, password);

        if (!response.isSuccess()) {
            setError(loginError, response.getErrorMessage());
            return;
        }

        // Lưu thông tin người dùng từ DTO trả về
        UserDto userDto = response.getPayload(UserDto.class);
        NetworkClient.getInstance().setCurrentUser(userDto);
        SessionManager.setCurrentUserDto(userDto); // Cần thêm phương thức này vào SessionManager

        try {
            Main.showMain();
        } catch (Exception e) {
            setError(loginError, "Lỗi khi mở màn hình chính: " + e.getMessage());
        }
    }

    @FXML
    private void handleRegister() {
        String username = regUsername.getText().trim();
        String password = regPassword.getText();
        String email    = regEmail.getText().trim();
        String role     = regRole.getValue().toUpperCase();

        if (username.isEmpty() || password.isEmpty() || email.isEmpty()) {
            setError(regError, "Vui lòng nhập đầy đủ thông tin.");
            return;
        }

        Message response = NetworkClient.getInstance().register(username, password, email, role);
        if (response.isSuccess()) {
            regError.setStyle("-fx-text-fill: #27ae60;");
            regError.setText("Đăng ký thành công! Hãy chuyển sang Đăng nhập.");
            regUsername.clear(); regPassword.clear(); regEmail.clear();
        } else {
            setError(regError, response.getErrorMessage());
        }
    }

    private void setError(Label label, String msg) {
        label.setStyle("-fx-text-fill: #e74c3c;");
        label.setText(msg);
    }
}