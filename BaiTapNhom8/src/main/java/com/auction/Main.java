package com.auction;

import com.auction.network.NetworkClient;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.stage.Stage;

import java.io.IOException;

public class Main extends Application {

    private static Stage primaryStage;

    @Override
    public void start(Stage stage) throws Exception {
        primaryStage = stage;

        try {
            NetworkClient.getInstance().connect();
            System.out.println("[Main] Kết nối server thành công.");
        } catch (IOException e) {
            Alert alert = new Alert(Alert.AlertType.ERROR,
                    "Không thể kết nối đến máy chủ đấu giá tại localhost:9999.\n" +
                            "Vui lòng đảm bảo AuctionServer đã được khởi động!",
                    ButtonType.OK);
            alert.setTitle("Lỗi Kết Nối");
            alert.setHeaderText("Mất kết nối với Server");
            alert.showAndWait();

            System.err.println("[Main] Lỗi kết nối: " + e.getMessage());
            System.exit(1);
        }

        showLogin();
    }

    public static void showLogin() throws Exception {
        FXMLLoader loader = new FXMLLoader(
                Main.class.getResource("/com/auction/gui/login.fxml"));
        Scene scene = new Scene(loader.load(), 420, 480);

        primaryStage.setTitle("Hệ Thống Đấu Giá Trực Tuyến - Đăng nhập");
        primaryStage.setScene(scene);
        primaryStage.setResizable(false);
        primaryStage.show();
    }

    public static void showMain() throws Exception {
        FXMLLoader loader = new FXMLLoader(
                Main.class.getResource("/com/auction/gui/main.fxml"));
        Scene scene = new Scene(loader.load(), 1150, 720);

        primaryStage.setTitle("Hệ Thống Đấu Giá Trực Tuyến - Dashboard");
        primaryStage.setScene(scene);
        primaryStage.setResizable(true);
        primaryStage.centerOnScreen();
        primaryStage.show();
    }

    public static Stage getPrimaryStage() {
        return primaryStage;
    }

    public static void main(String[] args) {
        launch(args);
    }
}