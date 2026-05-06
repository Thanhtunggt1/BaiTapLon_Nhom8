package com.auction.gui.controller;

import com.auction.gui.SessionManager;
import com.auction.manager.AuctionManager;
import com.auction.model.entity.Auction;
import com.auction.model.entity.Bidder;
import com.auction.model.entity.Seller;
import com.auction.model.entity.User;
import com.auction.model.enums.AuctionStatus;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.Region;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class AuctionListController {

    @FXML private ComboBox<String> statusFilter;
    @FXML private Label totalEarningsLabel;

    @FXML private TableView<Auction> auctionTable;
    @FXML private TableColumn<Auction, String> colItem;
    @FXML private TableColumn<Auction, String> colType;
    @FXML private TableColumn<Auction, String> colStartPrice;
    @FXML private TableColumn<Auction, String> colCurrentPrice;
    @FXML private TableColumn<Auction, String> colLeader;
    @FXML private TableColumn<Auction, String> colStatus;
    @FXML private TableColumn<Auction, String> colEndTime;
    @FXML private TableColumn<Auction, Void>   colAction;
    @FXML private TableColumn<Auction, Void>   colPay;

    private static final NumberFormat NF  = NumberFormat.getInstance(new Locale("vi", "VN"));
    private static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("dd/MM HH:mm:ss");

    @FXML
    public void initialize() {
        statusFilter.getItems().addAll("Tất cả", "OPEN", "RUNNING", "FINISHED", "CANCELED", "PAID");
        statusFilter.setValue("Tất cả");
        setupColumns();
        loadAuctions();
    }

    private void setupColumns() {
        colItem.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getItem().getName()));
        colType.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getItem().getClass().getSimpleName()));
        colStartPrice.setCellValueFactory(d -> new SimpleStringProperty(NF.format(d.getValue().getItem().getStartingPrice()) + " ₫"));
        colCurrentPrice.setCellValueFactory(d -> new SimpleStringProperty(NF.format(d.getValue().getCurrentHighestPrice()) + " ₫"));
        colLeader.setCellValueFactory(d -> {
            var leader = d.getValue().getCurrentLeader();
            return new SimpleStringProperty(leader != null ? leader.getUsername() : "—");
        });
        colStatus.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getStatus().toString()));
        colEndTime.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getEndTime().format(DTF)));

        colStatus.setCellFactory(col -> new TableCell<Auction, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                    return;
                }
                setText(item);
                String style = "-fx-text-fill: #f39c12;";
                switch (item) {
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

        colAction.setCellFactory(col -> new TableCell<Auction, Void>() {
            private final Button btn = new Button("Xem");
            {
                btn.setStyle("-fx-background-color: #2980b9; -fx-text-fill: white; -fx-cursor: hand;");
                btn.setOnAction(e -> openDetail(getTableView().getItems().get(getIndex())));
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : btn);
            }
        });

        colPay.setCellFactory(col -> new TableCell<Auction, Void>() {
            private final Button btnPay = new Button("Thanh toán");
            {
                btnPay.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; -fx-cursor: hand;");
                btnPay.setOnAction(e -> handleDirectPay(getTableView().getItems().get(getIndex())));
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    Auction a = getTableView().getItems().get(getIndex());
                    User user = SessionManager.getCurrentUser();

                    boolean isWinner = (user instanceof Bidder) && a.getCurrentLeader() != null
                            && a.getCurrentLeader().equals(user);

                    if (a.getStatus() == AuctionStatus.FINISHED && isWinner) {
                        setGraphic(btnPay);
                    } else {
                        setGraphic(null);
                    }
                }
            }
        });
    }

    private void handleDirectPay(Auction auction) {
        try {
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                    "Xác nhận thanh toán " + NF.format(auction.getCurrentHighestPrice()) + " ₫?",
                    ButtonType.YES, ButtonType.NO);
            confirm.setGraphic(null); // Tắt icon
            confirm.setTitle("Thanh toán trực tiếp");

            if (confirm.showAndWait().orElse(ButtonType.NO) == ButtonType.YES) {
                // Thực hiện trừ tiền và đổi trạng thái
                auction.markAsPaid();

                if (MainController.getInstance() != null) {
                    MainController.getInstance().refreshBalanceView();
                }

                handleRefresh();

                // TẠO BIÊN LAI MUA HÀNG
                String receipt = String.format("""
                    🎉 GIAO DỊCH HOÀN TẤT!
                    
                    🧾 BIÊN LAI MUA HÀNG
                    --------------------------------------------------
                    Mã phiên đấu giá: %s
                    Tên sản phẩm: %s
                    Giá thanh toán: %s ₫
                    
                    📦 THÔNG TIN GIAO NHẬN
                    Người bán: %s
                    Trạng thái: Chờ người bán bàn giao sản phẩm
                    
                    (Vui lòng liên hệ người bán qua hệ thống để thống nhất thời gian và địa điểm giao nhận hàng).
                    --------------------------------------------------
                    Cảm ơn bạn đã sử dụng Hệ Thống Đấu Giá Trực Tuyến!
                    """,
                        auction.getId().substring(0, 8).toUpperCase(),
                        auction.getItem().getName(),
                        NF.format(auction.getCurrentHighestPrice()),
                        auction.getSeller().getUsername()
                );

                Alert success = new Alert(Alert.AlertType.INFORMATION);
                success.setGraphic(null); // Tắt icon
                success.setTitle("Biên lai thanh toán");
                success.setHeaderText("Đã thanh toán thành công!");
                success.setContentText(receipt);
                success.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
                success.showAndWait();
            }
        } catch (Exception ex) {
            // KHÔI PHỤC LẠI BẢNG ĐẾM NGƯỢC KHI THIẾU TIỀN
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setGraphic(null); // Tắt icon
            alert.setTitle("Yêu cầu nạp tiền");
            alert.setHeaderText("Thanh toán không thành công");

            Runnable updateText = () -> {
                String warningMsg = "Số dư tài khoản của bạn không đủ để thanh toán cho món hàng này!";

                if (auction.getStatus() == AuctionStatus.FINISHED && auction.getFinishedTime() != null) {
                    LocalDateTime now = LocalDateTime.now();
                    LocalDateTime deadline = auction.getFinishedTime().plusHours(12);

                    if (now.isBefore(deadline)) {
                        long secs = ChronoUnit.SECONDS.between(now, deadline);
                        long h = secs / 3600, m = (secs % 3600) / 60, s = secs % 60;

                        warningMsg += String.format("\n\n⏱ BẠN CÒN ĐÚNG %02d:%02d:%02d ĐỂ GIỮ HÀNG.", h, m, s);
                        warningMsg += "\nHãy nạp thêm tiền ngay để hoàn tất mua hàng. Nếu quá hạn, hệ thống sẽ tự động hủy phiên đấu giá của bạn!";
                    } else {
                        warningMsg += "\n\n⚠ Đã quá thời hạn thanh toán! Hệ thống đang tiến hành hủy phiên đấu giá.";
                    }
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

    @FXML private void handleFilter() { loadAuctions(); }

    @FXML public void handleRefresh() {
        AuctionManager.getInstance().checkAndCloseExpiredAuctions();
        loadAuctions();
    }

    private void loadAuctions() {
        List<Auction> all = AuctionManager.getInstance().getAllAuctions();
        String filter = statusFilter.getValue();
        if (filter != null && !"Tất cả".equals(filter)) {
            AuctionStatus target = AuctionStatus.valueOf(filter);
            all = all.stream().filter(a -> a.getStatus() == target).collect(Collectors.toList());
        }
        auctionTable.setItems(FXCollections.observableArrayList(all));
        auctionTable.refresh();
        updateTotalEarnings();
    }

    private void updateTotalEarnings() {
        if (MainController.getInstance() != null) {
            MainController.getInstance().refreshBalanceView();
        }
    }

    private void openDetail(Auction auction) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/auction/gui/auction_detail.fxml"));
            Scene scene = new Scene(loader.load(), 960, 680);
            AuctionDetailController ctrl = loader.getController();
            ctrl.setAuction(auction);
            Stage stage = new Stage();
            stage.setTitle("Chi Tiết – " + auction.getItem().getName());
            stage.setScene(scene);
            stage.setOnHidden(e -> handleRefresh());
            stage.show();
        } catch (Exception e) { e.printStackTrace(); }
    }
}