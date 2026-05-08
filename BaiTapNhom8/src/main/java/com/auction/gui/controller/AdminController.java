package com.auction.gui.controller;

import com.auction.gui.UserStore;
import com.auction.network.NetworkClient;
import com.auction.network.Message;
import com.auction.network.dto.AuctionDto;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.util.Duration;

import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

public class AdminController {

    @FXML private Label totalAuctionsLabel;
    @FXML private Label runningLabel;
    @FXML private Label finishedLabel;
    @FXML private Label totalUsersLabel;

    @FXML private TableView<AuctionDto> allAuctionsTable;
    @FXML private TableColumn<AuctionDto, String> colId;
    @FXML private TableColumn<AuctionDto, String> colAdminItem;
    @FXML private TableColumn<AuctionDto, String> colAdminSeller;
    @FXML private TableColumn<AuctionDto, String> colAdminPrice;
    @FXML private TableColumn<AuctionDto, String> colAdminLeader;
    @FXML private TableColumn<AuctionDto, String> colAdminStatus;
    @FXML private TableColumn<AuctionDto, String> colAdminBids;
    @FXML private TableColumn<AuctionDto, String> colAdminEnd;

    private static final NumberFormat NF  = NumberFormat.getInstance(new Locale("vi", "VN"));

    @FXML
    public void initialize() {
        setupTable();
        loadData();

        NetworkClient.getInstance().setOnBidUpdate(updatedAuction -> {
            Platform.runLater(this::loadData);
        });
    }

    private void setupTable() {
        String leftAlign = "-fx-alignment: CENTER-LEFT; -fx-padding: 0 0 0 10;";
        colId.setStyle(leftAlign);
        colAdminItem.setStyle(leftAlign);
        colAdminSeller.setStyle(leftAlign);
        colAdminPrice.setStyle(leftAlign);
        colAdminLeader.setStyle(leftAlign);
        colAdminStatus.setStyle(leftAlign);
        colAdminBids.setStyle(leftAlign);
        colAdminEnd.setStyle(leftAlign);

        colId.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().id.substring(0, 8) + "…"));
        colAdminItem.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().itemName));
        colAdminSeller.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().sellerUsername));
        colAdminPrice.setCellValueFactory(d -> new SimpleStringProperty(NF.format(d.getValue().currentPrice) + " ₫"));
        colAdminLeader.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().currentLeader != null ? d.getValue().currentLeader : "—"));
        colAdminBids.setCellValueFactory(d -> new SimpleStringProperty(String.valueOf(d.getValue().bidCount)));

        colAdminEnd.setCellValueFactory(d -> {
            try {
                LocalDateTime dt = LocalDateTime.parse(d.getValue().endTime, DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"));
                return new SimpleStringProperty(dt.format(DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy")));
            } catch (Exception e) {
                return new SimpleStringProperty(d.getValue().endTime);
            }
        });

        colAdminStatus.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().status));
        colAdminStatus.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String s, boolean empty) {
                super.updateItem(s, empty);
                if (empty || s == null) { setText(null); setStyle(leftAlign); return; }
                setText(s);
                String style = "";
                switch (s) {
                    case "OPEN" -> style = "-fx-text-fill: #f39c12;";
                    case "RUNNING" -> style = "-fx-text-fill: #27ae60;";
                    case "FINISHED" -> style = "-fx-text-fill: #2980b9;";
                    case "CANCELED" -> style = "-fx-text-fill: #e74c3c;";
                    case "PAID" -> style = "-fx-text-fill: #8e44ad;";
                }
                setStyle(leftAlign + " " + style);
            }
        });
    }

    @FXML
    public void handleRefresh() {
        loadData();
    }

    private void loadData() {
        Message response = NetworkClient.getInstance().getAuctions();
        if (response.isSuccess()) {
            List<AuctionDto> all = response.getPayload(new com.google.gson.reflect.TypeToken<List<AuctionDto>>(){}.getType());
            allAuctionsTable.setItems(FXCollections.observableArrayList(all));

            totalAuctionsLabel.setText(String.valueOf(all.size()));
            runningLabel.setText(String.valueOf(all.stream().filter(a -> "RUNNING".equals(a.status)).count()));
            finishedLabel.setText(String.valueOf(all.stream().filter(a -> "FINISHED".equals(a.status) || "PAID".equals(a.status)).count()));

            totalUsersLabel.setText(String.valueOf(UserStore.getAllUsers().size()));

            allAuctionsTable.refresh();
        }
    }

    @FXML
    private void handleCancelAuction() {
        AuctionDto sel = allAuctionsTable.getSelectionModel().getSelectedItem();
        if (sel == null) { alert("Chưa chọn", "Hãy chọn phiên đấu giá cần hủy."); return; }

        TextInputDialog td = new TextInputDialog("Vi phạm quy định");
        td.setTitle("Hủy Phiên Đấu Giá");
        td.setHeaderText("Nhập lý do hủy phiên: " + sel.itemName);
        td.setContentText("Lý do:");
        td.showAndWait().ifPresent(reason -> {
            Message res = NetworkClient.getInstance().adminCancelAuction(sel.id, reason);
            if (res.isSuccess()) {
                loadData();
                alert("Thành công", "Đã hủy phiên đấu giá.");
            } else {
                alert("Lỗi Server", res.getErrorMessage());
            }
        });
    }

    @FXML
    private void handleEndAuction() {
        AuctionDto sel = allAuctionsTable.getSelectionModel().getSelectedItem();
        if (sel == null) { alert("Chưa chọn", "Hãy chọn phiên đấu giá cần kết thúc."); return; }

        Message res = NetworkClient.getInstance().endAuction(sel.id);
        if (res.isSuccess()) {
            loadData();
            alert("Thành công", "Đã ép kết thúc phiên đấu giá.");
        } else {
            alert("Lỗi Server", res.getErrorMessage());
        }
    }

    @FXML
    private void handleMarkPaid() {
        AuctionDto sel = allAuctionsTable.getSelectionModel().getSelectedItem();
        if (sel == null) { alert("Chưa chọn", "Hãy chọn phiên đấu giá."); return; }

        Message res = NetworkClient.getInstance().markPaid(sel.id);
        if (res.isSuccess()) {
            loadData();
            alert("Thành công", "Đã đánh dấu thanh toán thành công.");
        } else {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Thao tác thất bại");
            alert.setHeaderText("Không thể đánh dấu thanh toán");

            Runnable updateText = () -> {
                String warningMsg = "Chi tiết lỗi: " + res.getErrorMessage();

                if ("FINISHED".equals(sel.status) && sel.endTime != null) {
                    try {
                        LocalDateTime endTime = LocalDateTime.parse(sel.endTime, DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"));
                        LocalDateTime deadline = endTime.plusHours(12);
                        LocalDateTime now = LocalDateTime.now();

                        if (now.isBefore(deadline)) {
                            long secs = java.time.temporal.ChronoUnit.SECONDS.between(now, deadline);
                            long h = secs / 3600, m = (secs % 3600) / 60, s = secs % 60;
                            warningMsg += String.format("\n\nTHỜI GIAN CHỜ THANH TOÁN CÒN LẠI: %02d:%02d:%02d.", h, m, s);
                            warningMsg += "\n(Phiên sẽ tự động bị HỦY nếu người thắng không nạp đủ tiền khi hết giờ!)";
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

    private void alert(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION, msg, ButtonType.OK);
        a.setTitle(title); a.setHeaderText(null); a.showAndWait();
    }
}