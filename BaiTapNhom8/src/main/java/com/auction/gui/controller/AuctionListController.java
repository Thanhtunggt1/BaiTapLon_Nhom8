package com.auction.gui.controller;

import com.auction.network.NetworkClient;
import com.auction.network.Message;
import com.auction.network.dto.AuctionDto;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.util.Duration;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

public class AuctionListController {
    @FXML private ComboBox<String> statusFilter;
    @FXML private TableView<AuctionDto> auctionTable;
    @FXML private TableColumn<AuctionDto, String> colItem;
    @FXML private TableColumn<AuctionDto, String> colType;
    @FXML private TableColumn<AuctionDto, String> colStartPrice;
    @FXML private TableColumn<AuctionDto, String> colCurrentPrice;
    @FXML private TableColumn<AuctionDto, String> colLeader;
    @FXML private TableColumn<AuctionDto, String> colStatus;
    @FXML private TableColumn<AuctionDto, String> colEndTime;
    @FXML private TableColumn<AuctionDto, Void> colAction;
    @FXML private TableColumn<AuctionDto, Void> colPay;

    private List<AuctionDto> allAuctions;

    @FXML
    public void initialize() {
        statusFilter.getItems().addAll("Tất cả", "OPEN", "RUNNING", "FINISHED", "PAID", "CANCELED");
        statusFilter.setValue("Tất cả");

        setupColumns();
        loadAuctions();

        NetworkClient.getInstance().setOnBidUpdate(updatedAuction -> {
            Platform.runLater(this::loadAuctions);
        });
    }

    private void setupColumns() {
        String rightAlign = "-fx-alignment: CENTER-RIGHT; -fx-padding: 0 10 0 0;";
        colItem.setStyle(rightAlign);
        colType.setStyle(rightAlign);
        colStartPrice.setStyle(rightAlign);
        colCurrentPrice.setStyle(rightAlign);
        colLeader.setStyle(rightAlign);
        colStatus.setStyle(rightAlign);
        colEndTime.setStyle(rightAlign);
        colAction.setStyle(rightAlign);
        colPay.setStyle(rightAlign);

        colItem.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().itemName));
        colType.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().itemType));
        colStartPrice.setCellValueFactory(d -> new SimpleStringProperty(String.format("%,.0f ₫", d.getValue().startingPrice)));
        colCurrentPrice.setCellValueFactory(d -> new SimpleStringProperty(String.format("%,.0f ₫", d.getValue().currentPrice)));
        colLeader.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().currentLeader != null ? d.getValue().currentLeader : "—"));

        colStatus.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().status));
        colStatus.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String s, boolean empty) {
                super.updateItem(s, empty);
                if (empty || s == null) { setText(null); setStyle(rightAlign); return; }
                setText(s);
                String style = "";
                switch (s) {
                    case "OPEN": style = "-fx-text-fill: #f39c12;"; break;
                    case "RUNNING": style = "-fx-text-fill: #27ae60;"; break;
                    case "FINISHED": style = "-fx-text-fill: #2980b9;"; break;
                    case "CANCELED": style = "-fx-text-fill: #e74c3c;"; break;
                    case "PAID": style = "-fx-text-fill: #8e44ad;"; break;
                }
                setStyle(rightAlign + " " + style);
            }
        });

        colEndTime.setCellValueFactory(d -> {
            try {
                LocalDateTime dt = LocalDateTime.parse(d.getValue().endTime, DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"));
                return new SimpleStringProperty(dt.format(DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy")));
            } catch (Exception e) {
                return new SimpleStringProperty(d.getValue().endTime);
            }
        });

        colAction.setCellFactory(col -> new TableCell<>() {
            private final Button btn = new Button("Xem");
            {
                btn.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-cursor: hand;");
                btn.setOnAction(e -> openDetail(getTableView().getItems().get(getIndex())));
            }
            @Override protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : btn);
            }
        });

        colPay.setCellFactory(col -> new TableCell<>() {
            private final Button btn = new Button("Thanh toán");
            {
                btn.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; -fx-cursor: hand;");
                btn.setOnAction(e -> handlePay(getTableView().getItems().get(getIndex())));
            }
            @Override protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    AuctionDto dto = getTableView().getItems().get(getIndex());
                    String currentLeader = dto.currentLeader;
                    String currentUser = com.auction.gui.SessionManager.getCurrentUserDto().username;

                    if ("FINISHED".equals(dto.status) && currentUser.equals(currentLeader)) {
                        setGraphic(btn);
                    } else {
                        setGraphic(null);
                    }
                }
            }
        });
    }

    @FXML
    public void handleFilter() {
        if (allAuctions == null) return;
        String filter = statusFilter.getValue();
        if ("Tất cả".equals(filter)) {
            auctionTable.setItems(FXCollections.observableArrayList(allAuctions));
        } else {
            List<AuctionDto> filtered = allAuctions.stream()
                    .filter(a -> a.status.equals(filter))
                    .collect(Collectors.toList());
            auctionTable.setItems(FXCollections.observableArrayList(filtered));
        }
    }

    @FXML
    public void handleRefresh() {
        loadAuctions();
    }

    public void loadAuctions() {
        Message response = NetworkClient.getInstance().getAuctions();
        if (response.isSuccess()) {
            allAuctions = response.getPayload(new com.google.gson.reflect.TypeToken<List<AuctionDto>>(){}.getType());
            handleFilter();
        }
    }

    private void handlePay(AuctionDto dto) {
        Message response = NetworkClient.getInstance().markPaid(dto.id);
        if (response.isSuccess()) {
            com.auction.network.dto.UserDto userDto = com.auction.gui.SessionManager.getCurrentUserDto();
            if (userDto != null) {
                userDto.balance -= dto.currentPrice;
            }
            com.auction.model.entity.User localUser = com.auction.gui.SessionManager.getCurrentUser();
            if (localUser instanceof com.auction.model.entity.Bidder bidder) {
                try { bidder.deduct(dto.currentPrice); } catch (Exception ignored) {}
            }

            Alert bill = new Alert(Alert.AlertType.INFORMATION);
            bill.setTitle("Hóa Đơn Thanh Toán");
            bill.setHeaderText("Giao Dịch Thành Công!");

            String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss dd/MM/yyyy"));
            String receipt = String.format("Mã giao dịch:\t%s\nSản phẩm:\t%s\nNgười bán:\t%s\nNgười mua:\t%s\nGiá thanh toán:\t%,.0f ₫\nThời gian:\t%s\n\nCảm ơn bạn đã sử dụng hệ thống!",
                    dto.id.substring(0, 8).toUpperCase(),
                    dto.itemName,
                    dto.sellerUsername,
                    userDto != null ? userDto.username : dto.currentLeader,
                    dto.currentPrice,
                    time
            );
            bill.setContentText(receipt);
            bill.showAndWait();

            loadAuctions();
            if (MainController.getInstance() != null) {
                MainController.getInstance().refreshBalanceView();
            }
            try {
                AuctionDetailController.updateAllBalances();
            } catch (Exception ignored) {}
        } else {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Thanh toán thất bại");
            alert.setHeaderText("Tài khoản không đủ số dư!");

            Runnable updateText = () -> {
                String warningMsg = "Chi tiết lỗi: " + response.getErrorMessage();

                if ("FINISHED".equals(dto.status) && dto.endTime != null) {
                    try {
                        LocalDateTime endTime = LocalDateTime.parse(dto.endTime, DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"));
                        LocalDateTime deadline = endTime.plusHours(12);
                        LocalDateTime now = LocalDateTime.now();

                        if (now.isBefore(deadline)) {
                            long secs = java.time.temporal.ChronoUnit.SECONDS.between(now, deadline);
                            long h = secs / 3600, m = (secs % 3600) / 60, s = secs % 60;
                            warningMsg += String.format("\n\nTHỜI GIAN CHỜ NẠP TIỀN CÒN LẠI: %02d:%02d:%02d.", h, m, s);
                            warningMsg += "\n(Phiên sẽ tự động bị HỦY nếu bạn không nạp đủ tiền khi hết giờ!)";
                        } else {
                            warningMsg += "\n\nĐã quá thời hạn 12h! Phiên này đang chờ hệ thống tự động quét và hủy.";
                        }
                    } catch (Exception e) {}
                }
                alert.setContentText(warningMsg);
            };

            updateText.run();
            Timeline timeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> updateText.run()));
            timeline.setCycleCount(Timeline.INDEFINITE);
            timeline.play();

            alert.setOnHidden(e -> timeline.stop());
            alert.showAndWait();
        }
    }

    private void openDetail(AuctionDto dto) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/auction/gui/auction_detail.fxml"));
            Stage stage = new Stage();
            stage.setScene(new Scene(loader.load()));
            AuctionDetailController ctrl = loader.getController();
            ctrl.setAuctionDto(dto);
            stage.show();
        } catch (Exception e) { e.printStackTrace(); }
    }
}