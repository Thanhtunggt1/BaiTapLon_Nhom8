package com.auction.gui.controller;

import com.auction.gui.SessionManager;
import com.auction.manager.AuctionManager;
import com.auction.model.entity.Auction;
import com.auction.model.entity.Seller;
import com.auction.model.entity.User;
import com.auction.model.enums.AuctionStatus;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class AuctionListController {

    @FXML private ComboBox<String> statusFilter;
    @FXML private Label totalEarningsLabel; // Label hiển thị doanh thu

    @FXML private TableView<Auction> auctionTable;
    @FXML private TableColumn<Auction, String> colItem;
    @FXML private TableColumn<Auction, String> colType;
    @FXML private TableColumn<Auction, String> colStartPrice;
    @FXML private TableColumn<Auction, String> colCurrentPrice;
    @FXML private TableColumn<Auction, String> colLeader;
    @FXML private TableColumn<Auction, String> colStatus;
    @FXML private TableColumn<Auction, String> colEndTime;
    @FXML private TableColumn<Auction, Void>   colAction;

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
        colItem.setCellValueFactory(d ->
                new SimpleStringProperty(d.getValue().getItem().getName()));
        colType.setCellValueFactory(d ->
                new SimpleStringProperty(d.getValue().getItem().getClass().getSimpleName()));
        colStartPrice.setCellValueFactory(d ->
                new SimpleStringProperty(NF.format(d.getValue().getItem().getStartingPrice()) + " ₫"));
        colCurrentPrice.setCellValueFactory(d ->
                new SimpleStringProperty(NF.format(d.getValue().getCurrentHighestPrice()) + " ₫"));
        colLeader.setCellValueFactory(d -> {
            var leader = d.getValue().getCurrentLeader();
            return new SimpleStringProperty(leader != null ? leader.getUsername() : "—");
        });
        colStatus.setCellValueFactory(d ->
                new SimpleStringProperty(d.getValue().getStatus().toString()));
        colEndTime.setCellValueFactory(d ->
                new SimpleStringProperty(d.getValue().getEndTime().format(DTF)));

        // Color status cell - ĐÃ SỬA CÚ PHÁP SWITCH TRUYỀN THỐNG
        colStatus.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setStyle(""); return; }
                setText(item);

                String style = "-fx-text-fill: #f39c12;"; // Màu mặc định (vàng cam)
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

        // Action button
        colAction.setCellFactory(col -> new TableCell<>() {
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
    }

    @FXML
    private void handleFilter() { loadAuctions(); }

    @FXML
    public void handleRefresh() {
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

        // Gọi hàm cập nhật doanh thu
        updateTotalEarnings();
    }

    // Hàm tính tổng doanh thu cho Seller
    private void updateTotalEarnings() {
        if (totalEarningsLabel == null) return;

        User currentUser = SessionManager.getCurrentUser();

        if (currentUser instanceof Seller) {
            Seller seller = (Seller) currentUser;
            double total = seller.getAuctions().stream()
                    .filter(a -> a.getStatus() == com.auction.model.enums.AuctionStatus.PAID)
                    .mapToDouble(a -> a.getCurrentHighestPrice())
                    .sum();

            totalEarningsLabel.setText("Tổng doanh thu: " + NF.format(total) + " đ");
            totalEarningsLabel.setVisible(true);
        } else {
            totalEarningsLabel.setVisible(false);
        }
    }

    private void openDetail(Auction auction) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/auction/gui/auction_detail.fxml"));
            Scene scene = new Scene(loader.load(), 960, 680);
            AuctionDetailController ctrl = loader.getController();
            ctrl.setAuction(auction);

            Stage stage = new Stage();
            stage.setTitle("Chi Tiết – " + auction.getItem().getName());
            stage.setScene(scene);
            stage.setOnHidden(e -> handleRefresh());
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}