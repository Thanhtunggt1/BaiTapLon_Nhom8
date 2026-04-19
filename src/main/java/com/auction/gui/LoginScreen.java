package com.auction.gui;

import com.auction.model.entity.Bidder;
import com.auction.model.entity.Seller;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

public class LoginScreen {
    private VBox view;

    public LoginScreen() {
        view = new VBox(15);
        view.setPadding(new Insets(20));
        view.setAlignment(Pos.CENTER);

        Label lblTitle = new Label("ĐĂNG NHẬP HỆ THỐNG");
        lblTitle.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");

        TextField txtUsername = new TextField();
        txtUsername.setPromptText("Tên đăng nhập");

        PasswordField txtPassword = new PasswordField();
        txtPassword.setPromptText("Mật khẩu");

        ComboBox<String> roleCombo = new ComboBox<>();
        roleCombo.getItems().addAll("Bidder", "Seller");
        roleCombo.setValue("Bidder");

        Button btnLogin = new Button("Đăng Nhập");
        btnLogin.setOnAction(e -> handleLogin(txtUsername.getText(), roleCombo.getValue()));

        view.getChildren().addAll(lblTitle, txtUsername, txtPassword, roleCombo, btnLogin);
    }

    private void handleLogin(String username, String role) {
        if (username.isBlank()) {
            showAlert(Alert.AlertType.ERROR, "Lỗi", "Vui lòng nhập tên.");
            return;
        }

        if ("Seller".equals(role)) {
            Seller seller = new Seller(username, "123456", username + "@gmail.com");
            MainApp.showSellerDashboard(seller);
        } else {
            Bidder bidder = new Bidder(username, "123456", username + "@gmail.com", 50000);
            MainApp.showBidderDashboard(bidder);
        }
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setContentText(content);
        alert.showAndWait();
    }

    public VBox getView() {
        return view;
    }
}