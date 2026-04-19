package com.auction.gui;

import com.auction.manager.AuctionManager;
import com.auction.model.entity.User;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

public class MainApp extends Application {
    private static Stage primaryStage;
    private static User currentUser; // Lưu thông tin người dùng đang đăng nhập

    @Override
    public void start(Stage stage) {
        primaryStage = stage;
        primaryStage.setTitle("Hệ Thống Đấu Giá Trực Tuyến");
        showLoginScreen();
        primaryStage.show();
    }

    // Điều hướng: Màn hình Login
    public static void showLoginScreen() {
        currentUser = null; // Reset user
        Scene scene = new Scene(new LoginScreen().getView(), 400, 300);
        primaryStage.setScene(scene);
    }

    public static void showSellerDashboard(User seller) {
        currentUser = seller;
        Scene scene = new Scene(new SellerDashboard().getView(), 800, 600);
        primaryStage.setScene(scene);
    }

    public static void showBidderDashboard(User bidder) {
        currentUser = bidder;
        Scene scene = new Scene(new BidderDashboard().getView(), 800, 600);
        primaryStage.setScene(scene);
    }

    public static User getCurrentUser() {
        return currentUser;
    }

    public static void main(String[] args) {
        launch(args);
    }
}