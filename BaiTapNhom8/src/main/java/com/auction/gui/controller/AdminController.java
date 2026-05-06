package com.auction.gui.controller;

import com.auction.gui.SessionManager;
import com.auction.gui.UserStore;
import com.auction.manager.AuctionManager;
import com.auction.model.entity.Admin;
import com.auction.model.entity.Auction;
import com.auction.model.enums.AuctionStatus;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.util.Duration;

import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;

public class AdminController {

    // ── Summary cards ──────────────────────────────────────────────────────────
    @FXML private Label totalAuctionsLabel;
    @FXML private Label runningLabel;
    @FXML private Label finishedLabel;
    @FXML private Label totalUsersLabel;

    // ── Table ──────────────────────────────────────────────────────────────────
    @FXML private TableView<Auction>            allAuctionsTable;
    @FXML private TableColumn<Auction, String>  colId;
    @FXML private TableColumn<Auction, String>  colAdminItem;
    @FXML private TableColumn<Auction, String>  colAdminSeller;
    @FXML private TableColumn<Auction, String>  colAdminPrice;
    @FXML private TableColumn<Auction, String>  colAdminLeader;
    @FXML private TableColumn<Auction, String>  colAdminStatus;
    @FXML private TableColumn<Auction, String>  colAdminBids;
    @FXML private TableColumn<Auction, String>  colAdminEnd;

    private static final NumberFormat NF  = NumberFormat.getInstance(new Locale("vi", "VN"));
    private static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @FXML
    public void initialize() {
        setupTable();
        loadData();
    }

    private void setupTable() {
        colId.setCellValueFactory(d ->
                new SimpleStringProperty(d.getValue().getId().substring(0, 8) + "…"));
        colAdminItem.setCellValueFactory(d ->
                new SimpleStringProperty(d.getValue().getItem().getName()));
        colAdminSeller.setCellValueFactory(d ->
                new SimpleStringProperty(d.getValue().getSeller().getUsername()));
        colAdminPrice.setCellValueFactory(d ->
                new SimpleStringProperty(NF.format(d.getValue().getCurrentHighestPrice()) + " ₫"));
        colAdminLeader.setCellValueFactory(d -> {
            var l = d.getValue().getCurrentLeader();
            return new SimpleStringProperty(l != null ? l.getUsername() : "—");
        });
        colAdminStatus.setCellValueFactory(d ->
                new SimpleStringProperty(d.getValue().getStatus().toString()));
        colAdminBids.setCellValueFactory(d ->
                new SimpleStringProperty(String.valueOf(d.getValue().getBidHistory().size())));
        colAdminEnd.setCellValueFactory(d ->
                new SimpleStringProperty(d.getValue().getEndTime().format(DTF)));

        // Color status - ĐÃ SỬA VỀ CÚ PHÁP CHUẨN
        colAdminStatus.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String s, boolean empty) {
                super.updateItem(s, empty);
                if (empty || s == null) { setText(null); setStyle(""); return; }
                setText(s);

                String style = "-fx-text-fill: #f39c12;"; // Màu mặc định
                switch (s) {
                    case "RUNNING":
                        style = "-fx-text-fill: #27ae60;";
                        break;
                    case "FINISHED":
                        style = "-fx-text-fill: #2980b9;";
                        break;
                    case "PAID":
                        style = "-fx-text-fill: #8e44ad;";
                        break;
                    case "CANCELED":
                        style = "-fx-text-fill: #e74c3c;";
                        break;
                }
                setStyle(style);
            }
        });
    }

    @FXML
    public void handleRefresh() {
        AuctionManager.getInstance().checkAndCloseExpiredAuctions();
        loadData();
    }

    private void loadData() {
        List<Auction> all = AuctionManager.getInstance().getAllAuctions();
        allAuctionsTable.setItems(FXCollections.observableArrayList(all));

        totalAuctionsLabel.setText(String.valueOf(all.size()));
        runningLabel.setText(String.valueOf(
                all.stream().filter(a -> a.getStatus() == com.auction.model.enums.AuctionStatus.RUNNING).count()));
        finishedLabel.setText(String.valueOf(
                all.stream().filter(a -> a.getStatus() == com.auction.model.enums.AuctionStatus.FINISHED
                        || a.getStatus() == com.auction.model.enums.AuctionStatus.PAID).count()));
        totalUsersLabel.setText(String.valueOf(UserStore.getAllUsers().size()));

        allAuctionsTable.refresh();
    }

    @FXML
    private void handleCancelAuction() {
        Auction sel = allAuctionsTable.getSelectionModel().getSelectedItem();
        if (sel == null) { alert("Chưa chọn", "Hãy chọn phiên đấu giá cần hủy."); return; }

        TextInputDialog td = new TextInputDialog("Vi phạm quy định");
        td.setTitle("Hủy Phiên Đấu Giá");
        td.setHeaderText("Nhập lý do hủy phiên: " + sel.getItem().getName());
        td.setContentText("Lý do:");
        td.showAndWait().ifPresent(reason -> {
            try {
                Admin admin = (Admin) SessionManager.getCurrentUser();
                admin.resolveDispute(sel, reason);
                loadData();
                alert("Thành công", "Đã hủy phiên đấu giá.");
            } catch (Exception e) {
                alert("Lỗi", e.getMessage());
            }
        });
    }

    @FXML
    private void handleEndAuction() {
        Auction sel = allAuctionsTable.getSelectionModel().getSelectedItem();
        if (sel == null) { alert("Chưa chọn", "Hãy chọn phiên đấu giá cần kết thúc."); return; }
        try {
            sel.endAuction();
            loadData();
        } catch (Exception e) {
            alert("Lỗi", e.getMessage());
        }
    }

    @FXML
    private void handleMarkPaid() {
        Auction sel = allAuctionsTable.getSelectionModel().getSelectedItem();
        if (sel == null) { alert("Chưa chọn", "Hãy chọn phiên đấu giá."); return; }
        try {
            sel.markAsPaid();
            loadData();
            alert("Thành công", "Đã đánh dấu thanh toán thành công."); // Cải thiện báo thành công
        } catch (Exception ex) {
            // --- TÙY CHỈNH THÔNG BÁO CHO QUẢN TRỊ VIÊN ---
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Thao tác thất bại");
            alert.setHeaderText("Không thể đánh dấu thanh toán");

            // Gom logic tạo chữ vào một luồng lặp lại
            Runnable updateText = () -> {
                String warningMsg = "Chi tiết lỗi: " + ex.getMessage();

                if (sel.getStatus() == AuctionStatus.FINISHED && sel.getFinishedTime() != null) {
                    LocalDateTime now = LocalDateTime.now();
                    LocalDateTime deadline = sel.getFinishedTime().plusHours(12);

                    if (now.isBefore(deadline)) {
                        long secs = ChronoUnit.SECONDS.between(now, deadline);
                        long h = secs / 3600, m = (secs % 3600) / 60, s = secs % 60;

                        warningMsg += String.format("\n\n THỜI GIAN CHỜ THANH TOÁN CÒN: %02d:%02d:%02d.", h, m, s);
                        warningMsg += "\n(Phiên sẽ tự động bị HỦY nếu người thắng không nạp đủ tiền khi hết giờ!)";
                    } else {
                        warningMsg += "\n\n Đã quá thời hạn 12h! Phiên này đang chờ hệ thống tự động quét và hủy.";
                    }
                }
                alert.setContentText(warningMsg);
            };

            // Gọi chạy ngay lập tức lần đầu
            updateText.run();

            // Khởi tạo đồng hồ đếm ngược mỗi 1 giây giống hệt giao diện Bidder
            Timeline timeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> updateText.run()));
            timeline.setCycleCount(Timeline.INDEFINITE);
            timeline.play();

            // Dừng đồng hồ khi Admin tắt bảng thông báo
            alert.setOnHidden(e -> timeline.stop());

            alert.showAndWait();
        }
    }

    private void alert(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION, msg, ButtonType.OK);
        a.setTitle(title); a.setHeaderText(null); a.showAndWait();
    }
}