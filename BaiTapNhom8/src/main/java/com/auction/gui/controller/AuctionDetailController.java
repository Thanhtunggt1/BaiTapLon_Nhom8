package com.auction.gui.controller;

import com.auction.gui.SessionManager;
import com.auction.model.entity.*;
import com.auction.model.enums.AuctionStatus;
import com.auction.pattern.observer.Observer;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.util.Duration;

import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class AuctionDetailController implements Observer {

    // ── Item info ──────────────────────────────────────────────────────────────
    @FXML private Label itemNameLabel;
    @FXML private Label itemDescLabel;
    @FXML private Label itemDetailLabel;

    // ── Live status bar ────────────────────────────────────────────────────────
    @FXML private Label currentPriceLabel;
    @FXML private Label leaderLabel;
    @FXML private Label statusLabel;
    @FXML private Label timeRemainingLabel;

    // ── Chart & history ────────────────────────────────────────────────────────
    @FXML private LineChart<String, Number> priceChart;
    @FXML private TableView<BidTransaction>   bidHistoryTable;
    @FXML private TableColumn<BidTransaction, String> colBidder;
    @FXML private TableColumn<BidTransaction, String> colAmount;
    @FXML private TableColumn<BidTransaction, String> colTime;

    // ── Manual bid panel ───────────────────────────────────────────────────────
    @FXML private TextField bidAmountField;
    @FXML private Button    depositButton;
    @FXML private Label     balanceLabel;
    @FXML private Button    placeBidButton;
    @FXML private Label     bidMessage;

    // ── Auto-bid panel ─────────────────────────────────────────────────────────
    @FXML private TextField maxBidField;
    @FXML private TextField incrementField;
    @FXML private Label     autoBidMessage;

    // ── Info labels ────────────────────────────────────────────────────────────
    @FXML private Label totalBidsLabel;
    @FXML private Label sellerLabel;
    @FXML private Label startPriceLabel;

    private Auction auction;
    private XYChart.Series<String, Number> priceSeries;
    private Timeline countdown;
    private int bidCount = 0; // used as x-axis label

    private static final NumberFormat NF  = NumberFormat.getInstance(new Locale("vi", "VN"));
    private static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("HH:mm:ss");

    // ── Init ───────────────────────────────────────────────────────────────────

    @FXML
    public void initialize() {
        setupBidHistoryTable();
        setupChart();
    }

    public void setAuction(Auction auction) {
        this.auction = auction;
        auction.attach(this);
        refreshUI();
        startCountdown();
        configurePermissions();
    }

    // ── Permissions ────────────────────────────────────────────────────────────

    private void configurePermissions() {
        User user = SessionManager.getCurrentUser();
        boolean isBidder = user instanceof Bidder;
        boolean canBid   = isBidder && auction.getStatus() == AuctionStatus.RUNNING;

        bidAmountField.setDisable(!canBid);
        placeBidButton.setDisable(!canBid);
        maxBidField.setDisable(!isBidder);
        incrementField.setDisable(!isBidder);

        if (isBidder) {
            Bidder bidder = (Bidder) user;
            // ---> SỬA Ở ĐÂY: Ghim TÊN TÀI KHOẢN hiển thị ngay trước SỐ DƯ
            balanceLabel.setText("Tài khoản [" + bidder.getUsername() + "]  |  Số dư: " + NF.format(bidder.getBalance()) + " ₫");
            depositButton.setVisible(true);  // Hiện nút Nạp tiền
            depositButton.setManaged(true);
        } else {
            // ---> SỬA Ở ĐÂY: Nếu là Admin hoặc Seller vào xem, cũng cho hiện tên tài khoản luôn cho rõ ràng
            balanceLabel.setText("Tài khoản [" + user.getUsername() + "]  |  Vai trò: " + user.getClass().getSimpleName());
            depositButton.setVisible(false); // Ẩn nút Nạp tiền
            depositButton.setManaged(false);
            bidMessage.setText("Chỉ tài khoản Bidder mới có thể đặt giá.");
            autoBidMessage.setText("Chỉ tài khoản Bidder mới có thể dùng Auto-Bid.");
        }
    }

    // ── Table setup ────────────────────────────────────────────────────────────

    private void setupBidHistoryTable() {
        colBidder.setCellValueFactory(d ->
                new SimpleStringProperty(d.getValue().getBidder().getUsername()));
        colAmount.setCellValueFactory(d ->
                new SimpleStringProperty(NF.format(d.getValue().getAmount()) + " ₫"));
        colTime.setCellValueFactory(d ->
                new SimpleStringProperty(d.getValue().getTimestamp().format(DTF)));
    }

    private void setupChart() {
        priceSeries = new XYChart.Series<>();
        priceSeries.setName("Giá đặt");
        priceChart.getData().add(priceSeries);
        priceChart.setAnimated(false);
        priceChart.setCreateSymbols(true);
        priceChart.setLegendVisible(false);
    }

    // ── Refresh UI ─────────────────────────────────────────────────────────────

    private void refreshUI() {
        if (auction == null) return;
        Item item = auction.getItem();

        itemNameLabel.setText(item.getName());
        itemDescLabel.setText(item.getDescription());

        if (item instanceof Electronics e) {
            itemDetailLabel.setText("Thương hiệu: " + e.getBrand()
                    + " | Bảo hành: " + e.getWarrantyMonths() + " tháng");
        } else if (item instanceof Art a) {
            itemDetailLabel.setText("Nghệ sĩ: " + a.getArtistName()
                    + " | Năm sáng tác: " + a.getCreationYear());
        } else if (item instanceof Vehicle v) {
            itemDetailLabel.setText("Biển số: " + v.getLicensePlate()
                    + " | Số km: " + NF.format(v.getMileage()));
        }

        currentPriceLabel.setText(NF.format(auction.getCurrentHighestPrice()) + " ₫");
        leaderLabel.setText(auction.getCurrentLeader() != null
                ? auction.getCurrentLeader().getUsername() : "—");
        statusLabel.setText(auction.getStatus().toString());
        statusLabel.setStyle(statusStyle(auction.getStatus()));

        totalBidsLabel.setText("Tổng bid: " + auction.getBidHistory().size());
        sellerLabel.setText("Người bán: " + auction.getSeller().getUsername());
        startPriceLabel.setText("Giá khởi điểm: " + NF.format(item.getStartingPrice()) + " ₫");

        // Bid history (newest first)
        List<BidTransaction> history = new ArrayList<>(auction.getBidHistory());
        Collections.reverse(history);
        bidHistoryTable.setItems(FXCollections.observableArrayList(history));

        // Update chart with any new bids
        int histSize = auction.getBidHistory().size();
        while (bidCount < histSize) {
            BidTransaction bt = auction.getBidHistory().get(bidCount);
            priceSeries.getData().add(
                    new XYChart.Data<>(String.valueOf(bidCount + 1), bt.getAmount()));
            bidCount++;
        }

        // --- Logic nhắc nhở nạp tiền cho người thắng cuộc ---
        User user = SessionManager.getCurrentUser();
        if (auction.getStatus() == AuctionStatus.FINISHED && user instanceof Bidder bidder) {
            boolean isWinner = auction.getCurrentLeader() != null &&
                    auction.getCurrentLeader().equals(bidder);
            if (isWinner) {
                double finalPrice = auction.getCurrentHighestPrice();
                if (bidder.getBalance() < finalPrice) {
                    double missing = finalPrice - bidder.getBalance();
                    bidMessage.setText("⚠ BẠN ĐÃ THẮNG! Vui lòng nạp thêm ít nhất "
                            + NF.format(missing) + " ₫ để có thể thanh toán.");
                    bidMessage.setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold; -fx-background-color: #fdeaea; -fx-padding: 5;");
                } else {
                    bidMessage.setText("🎉 Chúc mừng! Bạn đã thắng. Hãy thanh toán ngay để nhận sản phẩm.");
                    bidMessage.setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold;");
                }
            }
        }

        // Update bid/autobid buttons based on current status
        configurePermissions();
    }

    private String statusStyle(AuctionStatus s) {
        return switch (s) {
            case RUNNING  -> "-fx-text-fill: #27ae60; -fx-font-size: 15; -fx-font-weight: bold;";
            case FINISHED -> "-fx-text-fill: #2980b9; -fx-font-size: 15; -fx-font-weight: bold;";
            case PAID     -> "-fx-text-fill: #8e44ad; -fx-font-size: 15; -fx-font-weight: bold;";
            case CANCELED -> "-fx-text-fill: #e74c3c; -fx-font-size: 15; -fx-font-weight: bold;";
            default       -> "-fx-text-fill: #f39c12; -fx-font-size: 15; -fx-font-weight: bold;";
        };
    }

    // ── Countdown ──────────────────────────────────────────────────────────────

    private void startCountdown() {
        if (countdown != null) countdown.stop();
        countdown = new Timeline(new KeyFrame(Duration.seconds(1), e -> tick()));
        countdown.setCycleCount(Timeline.INDEFINITE);
        countdown.play();
    }

    private void tick() {
        if (auction == null) return;
        LocalDateTime now = LocalDateTime.now();

        // --- Logic đếm ngược cho phiên đang chạy ---
        if (auction.getStatus() == AuctionStatus.RUNNING) {
            LocalDateTime end = auction.getEndTime();
            if (now.isAfter(end)) {
                timeRemainingLabel.setText("Đang xử lý...");
                return;
            }
            long secs = ChronoUnit.SECONDS.between(now, end);
            displayTime(secs, "Kết thúc sau: ");
            if (secs < 30) timeRemainingLabel.setStyle("-fx-text-fill: #e74c3c; -fx-font-size: 15; -fx-font-weight: bold;");
        }
        // --- Logic đếm ngược 1 PHÚT cho hạn thanh toán (Để test) ---
        else if (auction.getStatus() == AuctionStatus.FINISHED && auction.getFinishedTime() != null) {
            // Nhớ đổi thành plusHours(12) khi triển khai thực tế nhé!
            LocalDateTime deadline = auction.getFinishedTime().plusMinutes(1);
            if (now.isAfter(deadline)) {
                timeRemainingLabel.setText("Quá hạn thanh toán");
                timeRemainingLabel.setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;");
                return;
            }
            long secs = ChronoUnit.SECONDS.between(now, deadline);
            displayTime(secs, "Hạn thanh toán: ");
            timeRemainingLabel.setStyle("-fx-text-fill: #e67e22; -fx-font-size: 15; -fx-font-weight: bold;");
        }
        else {
            timeRemainingLabel.setText("Phiên đã đóng");
            if (countdown != null) countdown.stop();
        }
    }

    private void displayTime(long secs, String prefix) {
        long h = secs / 3600, m = (secs % 3600) / 60, s = secs % 60;
        timeRemainingLabel.setText(prefix + String.format("%02d:%02d:%02d", h, m, s));
    }

    // ── Actions ────────────────────────────────────────────────────────────────

    @FXML
    private void handlePlaceBid() {
        User user = SessionManager.getCurrentUser();
        if (!(user instanceof Bidder bidder)) {
            setMsg(bidMessage, " Chỉ Bidder mới có thể đặt giá.", true);
            return;
        }
        String text = bidAmountField.getText().trim();
        if (text.isEmpty()) { setMsg(bidMessage, " Vui lòng nhập số tiền.", true); return; }
        try {
            double amount = Double.parseDouble(text.replace(",", "").replace(".", ""));
            boolean ok = bidder.placeBid(auction, amount);
            if (ok) {
                setMsg(bidMessage, " Đặt giá " + NF.format(amount) + " ₫ thành công!", false);
                bidAmountField.clear();
            }
        } catch (NumberFormatException e) {
            setMsg(bidMessage, " Số tiền không hợp lệ.", true);
        } catch (Exception e) {
            setMsg(bidMessage, " " + e.getMessage(), true);
        }
    }

    @FXML
    private void handleDeposit() {
        User user = SessionManager.getCurrentUser();
        if (!(user instanceof Bidder bidder)) return;

        // Mở hộp thoại nhập số tiền
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Nạp Tiền");
        dialog.setHeaderText("Nạp thêm tiền vào tài khoản");
        dialog.setContentText("Nhập số tiền cần nạp (VNĐ):");

        dialog.showAndWait().ifPresent(input -> {
            try {
                // Xóa các dấu phẩy, chấm nếu người dùng lỡ nhập sai định dạng
                double amount = Double.parseDouble(input.replace(",", "").replace(".", "").trim());
                if (amount <= 0) {
                    setMsg(bidMessage, " Số tiền nạp phải lớn hơn 0.", true);
                    return;
                }

                // Gọi hàm nạp tiền của class Bidder
                bidder.deposit(amount);

                // Cập nhật lại UI số dư ngay lập tức bên trong cửa sổ này VÀ HIỆN CẢ TÊN TÀI KHOẢN
                balanceLabel.setText("Tài khoản [" + bidder.getUsername() + "]  |  Số dư: " + NF.format(bidder.getBalance()) + " ₫");
                setMsg(bidMessage, "Nạp thành công " + NF.format(amount) + " ₫!", false);

                // ĐỒNG BỘ: CẬP NHẬT LÊN THANH HEADER BÊN NGOÀI NGAY LẬP TỨC
                if (MainController.getInstance() != null) {
                    MainController.getInstance().refreshBalanceView();
                }

                // Refresh UI để cập nhật cảnh báo nhắc nạp tiền nếu có
                refreshUI();

            } catch (NumberFormatException ex) {
                setMsg(bidMessage, "Số tiền không hợp lệ. Vui lòng chỉ nhập số.", true);
            }
        });
    }

    @FXML
    private void handleSetupAutoBid() {
        User user = SessionManager.getCurrentUser();
        if (!(user instanceof Bidder bidder)) {
            setMsg(autoBidMessage, " Chỉ Bidder mới có thể cài Auto-Bid.", true);
            return;
        }
        try {
            double maxBid   = Double.parseDouble(maxBidField.getText().trim().replace(",", ""));
            double increment = Double.parseDouble(incrementField.getText().trim().replace(",", ""));
            bidder.setupAutoBid(auction, maxBid, increment);
            setMsg(autoBidMessage, " Đã cài Auto-Bid! Max=" + NF.format(maxBid) + "₫, Bước=" + NF.format(increment) + "₫", false);
        } catch (NumberFormatException e) {
            setMsg(autoBidMessage, " Giá trị không hợp lệ.", true);
        } catch (Exception e) {
            setMsg(autoBidMessage, "❌ " + e.getMessage(), true);
        }
    }

    private void setMsg(Label lbl, String msg, boolean error) {
        lbl.setStyle(error ? "-fx-text-fill: #e74c3c;" : "-fx-text-fill: #27ae60;");
        lbl.setText(msg);
    }

    // ── Observer callback ──────────────────────────────────────────────────────

    @Override
    public void update(Auction auction) {
        Platform.runLater(() -> {
            // Auto-detach if window is closed
            if (priceChart.getScene() == null
                    || priceChart.getScene().getWindow() == null
                    || !priceChart.getScene().getWindow().isShowing()) {
                auction.detach(this);
                if (countdown != null) countdown.stop();
                return;
            }
            refreshUI();
        });
    }
}