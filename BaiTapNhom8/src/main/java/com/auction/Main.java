package com.auction;

import com.auction.gui.DataInitializer;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {

    private static Stage primaryStage;

    @Override
    public void start(Stage stage) throws Exception {
        primaryStage = stage;
        DataInitializer.init();
        showLogin();
    }

    public static void showLogin() throws Exception {
        FXMLLoader loader = new FXMLLoader(
            Main.class.getResource("/com/auction/gui/login.fxml"));
        Scene scene = new Scene(loader.load(), 420, 480);
        primaryStage.setTitle("Hệ Thống Đấu Giá Trực Tuyến");
        primaryStage.setScene(scene);
        primaryStage.setResizable(false);
        primaryStage.show();
    }

    public static void showMain() throws Exception {
        FXMLLoader loader = new FXMLLoader(
            Main.class.getResource("/com/auction/gui/main.fxml"));
        Scene scene = new Scene(loader.load(), 1150, 720);
        primaryStage.setTitle("Hệ Thống Đấu Giá Trực Tuyến");
        primaryStage.setScene(scene);
        primaryStage.setResizable(true);
        primaryStage.show();
    }

    public static Stage getPrimaryStage() {
        return primaryStage;
    }

    public static void main(String[] args) {
        launch(args);
    }
}