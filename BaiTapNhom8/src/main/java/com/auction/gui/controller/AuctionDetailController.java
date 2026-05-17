package com.auction.gui.controller;

import com.auction.gui.SessionManager;
import com.auction.network.NetworkClient;
import com.auction.network.Message;
import com.auction.network.dto.AuctionDto;
import com.auction.network.dto.UserDto;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.geometry.Pos;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.util.Duration;
import javafx.scene.layout.StackPane;

import java.io.ByteArrayInputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Base64;

public class AuctionDetailController {

    private static final java.util.List<AuctionDetailController> activeInstances = new java.util.ArrayList<>();

    @FXML private Button btnViewImage;
    @FXML private Label itemNameLabel;
    @FXML private Label itemDescLabel;
    @FXML private Label itemDetailLabel;
    @FXML private Label sellerLabel;
    @FXML private Label startPriceLabel;
    @FXML private Label totalBidsLabel;
    @FXML private LineChart<String, Number> priceChart;

    @FXML private TableView<AuctionDto.BidEntryDto> bidHistoryTable;
    @FXML private TableColumn<AuctionDto.BidEntryDto, String> colBidder;
    @FXML private TableColumn<AuctionDto.BidEntryDto, String> colAmount;
    @FXML private TableColumn<AuctionDto.BidEntryDto, String> colTime;

    @FXML private Label timeRemainingLabel;
    @FXML private Label statusLabel;
    @FXML private Label currentPriceLabel;
    @FXML private Label leaderLabel;
    @FXML private Label balanceLabel;
    @FXML private Button depositButton;
    @FXML private TextField bidAmountField;
    @FXML private Button placeBidButton;
    @FXML private Label bidMessage;
    @FXML private TextField maxBidField;
    @FXML private TextField incrementField;
    @FXML private Label autoBidMessage;

    private AuctionDto auctionDto;
    private Timeline timeline;

    @FXML
    public void initialize() {
        activeInstances.add(this);

        itemNameLabel.setAlignment(Pos.CENTER_LEFT);
        itemDescLabel.setAlignment(Pos.CENTER_LEFT);
        itemDetailLabel.setAlignment(Pos.CENTER_LEFT);
        sellerLabel.setAlignment(Pos.CENTER_LEFT);
        startPriceLabel.setAlignment(Pos.CENTER_LEFT);
        totalBidsLabel.setAlignment(Pos.CENTER_LEFT);
        timeRemainingLabel.setAlignment(Pos.CENTER);
        statusLabel.setAlignment(Pos.CENTER);
        currentPriceLabel.setAlignment(Pos.CENTER);
        leaderLabel.setAlignment(Pos.CENTER);
        balanceLabel.setAlignment(Pos.CENTER_LEFT);
        bidMessage.setAlignment(Pos.CENTER_LEFT);
        autoBidMessage.setAlignment(Pos.CENTER_LEFT);

        priceChart.setLegendVisible(false);

        colBidder.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().bidderName));
        colAmount.setCellValueFactory(d -> new SimpleStringProperty(String.format("%,.0f ₫", d.getValue().amount)));
        colTime.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().time));

        NetworkClient.getInstance().setOnBidUpdate(updatedAuction -> {
            Platform.runLater(() -> {
                if (this.auctionDto != null && this.auctionDto.id.equals(updatedAuction.id)) {
                    setAuctionDto(updatedAuction);
                }
            });
        });
    }

    public static void updateAllBalances() {
        Platform.runLater(() -> {
            UserDto user = SessionManager.getCurrentUserDto();
            if (user != null && "BIDDER".equals(user.role)) {
                for (AuctionDetailController ctrl : activeInstances) {
                    if (ctrl.balanceLabel != null) {
                        ctrl.balanceLabel.setText(String.format("Số dư: %,.0f ₫", user.balance));
                    }
                }
            }
        });
    }

    public void setAuctionDto(AuctionDto dto) {
        this.auctionDto = dto;
        refreshUI();
        startTimer();
    }

    private void refreshUI() {
        itemNameLabel.setText(auctionDto.itemName);
        itemDescLabel.setText(auctionDto.description);
        itemDetailLabel.setText("Loại: " + auctionDto.itemType);
        sellerLabel.setText("Người bán: " + auctionDto.sellerUsername);
        startPriceLabel.setText(String.format("Giá khởi điểm: %,.0f ₫", auctionDto.startingPrice));
        totalBidsLabel.setText("Tổng bid: " + auctionDto.bidCount);
        statusLabel.setText(auctionDto.status);
        currentPriceLabel.setText(String.format("%,.0f ₫", auctionDto.currentPrice));
        leaderLabel.setText(auctionDto.currentLeader != null ? auctionDto.currentLeader : "—");

        if (auctionDto.imagesBase64 != null && !auctionDto.imagesBase64.isEmpty()) {
            btnViewImage.setDisable(false);
            btnViewImage.setText("Xem bộ ảnh (" + auctionDto.imagesBase64.size() + " tấm)");
        } else {
            btnViewImage.setDisable(true);
            btnViewImage.setText("Không có ảnh");
        }

        if (auctionDto.history != null) {
            bidHistoryTable.setItems(FXCollections.observableArrayList(auctionDto.history));
        }

        updateChart();

        UserDto user = SessionManager.getCurrentUserDto();
        if (user != null && "BIDDER".equals(user.role)) {
            balanceLabel.setText(String.format("Số dư: %,.0f ₫", user.balance));
        } else {
            balanceLabel.setText("");
            depositButton.setVisible(false);
            placeBidButton.setDisable(true);
        }
    }

    @FXML
    private void handleViewImage() {
        if (auctionDto.imagesBase64 == null || auctionDto.imagesBase64.isEmpty()) return;

        Pagination pagination = new Pagination(auctionDto.imagesBase64.size(), 0);
        pagination.setPageFactory(pageIndex -> {
            try {
                byte[] bytes = Base64.getDecoder().decode(auctionDto.imagesBase64.get(pageIndex));
                Image img = new Image(new ByteArrayInputStream(bytes));
                ImageView iv = new ImageView(img);
                iv.setPreserveRatio(true);

                // Giảm nhẹ chiều cao để nhường chỗ cho thanh chuyển trang
                iv.setFitWidth(600);
                iv.setFitHeight(450);

                // Gói ảnh vào một hộp ảo có kích thước cố định
                StackPane imageContainer = new StackPane(iv);
                imageContainer.setAlignment(Pos.CENTER);
                imageContainer.setPrefHeight(480);

                return imageContainer;
            } catch (Exception e) {
                return new Label("Lỗi hiển thị ảnh này.");
            }
        });

        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Album ảnh sản phẩm: " + auctionDto.itemName);
        dialog.getDialogPane().setContent(pagination);

        // Tăng chiều cao tối thiểu của cửa sổ lên 600 để dải số trang rơi thẳng xuống dưới
        dialog.getDialogPane().setMinWidth(650);
        dialog.getDialogPane().setMinHeight(600);

        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.showAndWait();
    }

    private void updateChart() {
        priceChart.setAnimated(false);

        XYChart.Series<String, Number> series;
        if (priceChart.getData().isEmpty()) {
            series = new XYChart.Series<>();
            series.setName("Biến động giá");
            priceChart.getData().add(series);
        } else {
            series = priceChart.getData().get(0);
        }

        series.getData().clear();
        series.getData().add(new XYChart.Data<>("Khởi điểm", auctionDto.startingPrice));

        int step = 1;
        if (auctionDto.history != null) {
            for (AuctionDto.BidEntryDto b : auctionDto.history) {
                series.getData().add(new XYChart.Data<>("Lượt " + step++, b.amount));
            }
        }
    }

    private void startTimer() {
        if (timeline != null) timeline.stop();

        timeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            if (auctionDto == null || auctionDto.endTime == null) return;
            try {
                DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
                LocalDateTime end = LocalDateTime.parse(auctionDto.endTime, dtf);
                LocalDateTime now = LocalDateTime.now();

                if (now.isAfter(end) || !"RUNNING".equals(auctionDto.status)) {
                    timeRemainingLabel.setText("00:00:00");
                    if (timeline != null) timeline.stop();
                } else {
                    long secs = ChronoUnit.SECONDS.between(now, end);
                    long h = secs / 3600;
                    long m = (secs % 3600) / 60;
                    long s = secs % 60;
                    timeRemainingLabel.setText(String.format("%02d:%02d:%02d", h, m, s));
                }
            } catch (Exception ex) {
                timeRemainingLabel.setText(auctionDto.endTime);
            }
        }));
        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.play();
    }

    @FXML
    private void handlePlaceBid() {
        try {
            double amount = Double.parseDouble(bidAmountField.getText().trim());
            Message response = NetworkClient.getInstance().placeBid(auctionDto.id, amount);

            if (response.isSuccess()) {
                bidMessage.setText("Đặt giá thành công!");
                bidMessage.setStyle("-fx-text-fill: #27ae60;");
                this.auctionDto = response.getPayload(AuctionDto.class);
                refreshUI();
            } else {
                bidMessage.setText(response.getErrorMessage());
                bidMessage.setStyle("-fx-text-fill: #e74c3c;");
            }
        } catch (Exception e) {
            bidMessage.setText("Số tiền không hợp lệ.");
            bidMessage.setStyle("-fx-text-fill: #e74c3c;");
        }
    }

    @FXML
    private void handleDeposit() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Nạp Tiền");
        dialog.setHeaderText("Nạp thêm tiền vào tài khoản");
        dialog.setContentText("Nhập số tiền (VNĐ):");

        dialog.showAndWait().ifPresent(input -> {
            try {
                double amount = Double.parseDouble(input.replace(",", "").replace(".", "").trim());
                Message response = NetworkClient.getInstance().deposit(amount);
                if (response.isSuccess()) {
                    UserDto updatedUser = response.getPayload(UserDto.class);
                    SessionManager.setCurrentUserDto(updatedUser);

                    com.auction.model.entity.User localUser = SessionManager.getCurrentUser();
                    if (localUser instanceof com.auction.model.entity.Bidder bidder) {
                        bidder.deposit(amount);
                    }

                    updateAllBalances();
                    if (MainController.getInstance() != null) {
                        MainController.getInstance().refreshBalanceView();
                    }
                    Alert a = new Alert(Alert.AlertType.INFORMATION, "Nạp thành công!");
                    a.setHeaderText(null);
                    a.showAndWait();
                } else {
                    Alert a = new Alert(Alert.AlertType.ERROR, response.getErrorMessage());
                    a.setHeaderText(null);
                    a.showAndWait();
                }
            } catch (Exception ignored) {}
        });
    }

    @FXML
    private void handleSetupAutoBid() {
        try {
            double maxBid = Double.parseDouble(maxBidField.getText().trim());
            double increment = Double.parseDouble(incrementField.getText().trim());

            Message response = NetworkClient.getInstance().setupAutoBid(auctionDto.id, maxBid, increment);

            if (response.isSuccess()) {
                autoBidMessage.setText("Cài đặt Auto-Bid mạng thành công!");
                autoBidMessage.setStyle("-fx-text-fill: #27ae60;");

                maxBidField.clear();
                incrementField.clear();
            } else {
                autoBidMessage.setText(response.getErrorMessage());
                autoBidMessage.setStyle("-fx-text-fill: #e74c3c;");
            }
        } catch (NumberFormatException e) {
            autoBidMessage.setText("Vui lòng nhập số hợp lệ.");
            autoBidMessage.setStyle("-fx-text-fill: #e74c3c;");
        }
    }
}