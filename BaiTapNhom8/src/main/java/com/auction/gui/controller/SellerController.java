package com.auction.gui.controller;

import com.auction.gui.SessionManager;
import com.auction.manager.AuctionManager;
import com.auction.model.entity.*;
import com.auction.model.enums.ItemType;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;

import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public class SellerController {
    @FXML
    private Label totalEarningsLabel;

    // Item tab
    @FXML private TableView<Item>               itemTable;
    @FXML private TableColumn<Item, String>     colItemName;
    @FXML private TableColumn<Item, String>     colItemType;
    @FXML private TableColumn<Item, String>     colItemPrice;
    @FXML private TableColumn<Item, String>     colItemDesc;

    // Auction tab
    @FXML private TableView<Auction>            auctionTable;
    @FXML private TableColumn<Auction, String>  colAuctionItem;
    @FXML private TableColumn<Auction, String>  colAuctionStatus;
    @FXML private TableColumn<Auction, String>  colAuctionCurrent;
    @FXML private TableColumn<Auction, String>  colAuctionLeader;
    @FXML private TableColumn<Auction, String>  colAuctionBids;
    @FXML private TableColumn<Auction, String>  colAuctionEnd;

    private static final NumberFormat NF  = NumberFormat.getInstance(new Locale("vi", "VN"));
    private static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @FXML
    public void initialize() {
        setupItemTable();
        setupAuctionTable();
        loadData();
    }

    private Seller getSeller() {
        return (Seller) SessionManager.getCurrentUser();
    }

    // Table setup

    private void setupItemTable() {
        colItemName.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getName()));
        colItemType.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getClass().getSimpleName()));
        colItemPrice.setCellValueFactory(d -> new SimpleStringProperty(NF.format(d.getValue().getStartingPrice()) + " ₫"));
        colItemDesc.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getDescription()));
    }

    private void setupAuctionTable() {
        colAuctionItem.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getItem().getName()));
        colAuctionStatus.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getStatus().toString()));
        colAuctionCurrent.setCellValueFactory(d -> new SimpleStringProperty(NF.format(d.getValue().getCurrentHighestPrice()) + " ₫"));
        colAuctionLeader.setCellValueFactory(d -> {
            var leader = d.getValue().getCurrentLeader();
            return new SimpleStringProperty(leader != null ? leader.getUsername() : "—");
        });
        colAuctionBids.setCellValueFactory(d -> new SimpleStringProperty(String.valueOf(d.getValue().getBidHistory().size())));
        colAuctionEnd.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getEndTime().format(DTF)));

        // Color status
        colAuctionStatus.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String s, boolean empty) {
                super.updateItem(s, empty);
                if (empty || s == null) { setText(null); setStyle(""); return; }
                setText(s);

                String style = "";
                switch (s) {
                    case "RUNNING":
                        style = "-fx-text-fill: #27ae60;";
                        break;
                    case "FINISHED":
                        style = "-fx-text-fill: #2980b9;";
                        break;
                    case "CANCELED":
                        style = "-fx-text-fill: #e74c3c;";
                        break;
                    case "PAID":
                        style = "-fx-text-fill: #8e44ad;";
                        break;
                }
                setStyle(style);
            }
        });
    }

    private void loadData() {
        itemTable.setItems(FXCollections.observableArrayList(getSeller().getItems()));
        auctionTable.setItems(FXCollections.observableArrayList(getSeller().getAuctions()));
        itemTable.refresh();
        auctionTable.refresh();
        updateTotalEarnings();
    }

    // Item actions
    @FXML private void handleRefreshItems()    { loadData(); }
    @FXML private void handleRefreshAuctions() { loadData(); }

    @FXML
    private void handleAddItem() {
        // Build dialog
        Dialog<Item> dialog = new Dialog<>();
        dialog.setTitle("Thêm Sản Phẩm Mới");
        ButtonType addBtn = new ButtonType("Thêm", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(addBtn, ButtonType.CANCEL);

        GridPane g = buildGrid();

        TextField name   = new TextField();  name.setPromptText("Tên sản phẩm");
        TextField desc   = new TextField();  desc.setPromptText("Mô tả");
        TextField price  = new TextField();  price.setPromptText("Giá khởi điểm (VNĐ)");
        ComboBox<ItemType> typeBox = new ComboBox<>(FXCollections.observableArrayList(ItemType.values()));
        typeBox.setValue(ItemType.ELECTRONICS);

        // Extra fields
        TextField brand    = new TextField("Samsung");
        TextField warranty = new TextField("24");
        TextField artist   = new TextField();  artist.setPromptText("Tên nghệ sĩ");
        TextField year     = new TextField("2020");
        TextField mileage  = new TextField("0");
        TextField plate    = new TextField();  plate.setPromptText("29A-12345");

        Label lBrand = new Label("Thương hiệu:");
        Label lWarr  = new Label("Bảo hành (tháng):");
        Label lArtist= new Label("Nghệ sĩ:");
        Label lYear  = new Label("Năm sáng tác:");
        Label lMile  = new Label("Số km:");
        Label lPlate = new Label("Biển số:");

        int row = 0;
        g.add(new Label("Loại sản phẩm:"), 0, row); g.add(typeBox, 1, row++);
        g.add(new Label("Tên:"),            0, row); g.add(name,    1, row++);
        g.add(new Label("Mô tả:"),          0, row); g.add(desc,    1, row++);
        g.add(new Label("Giá khởi điểm:"),  0, row); g.add(price,   1, row++);
        g.add(lBrand,  0, row); g.add(brand,   1, row++);
        g.add(lWarr,   0, row); g.add(warranty, 1, row++);
        g.add(lArtist, 0, row); g.add(artist,   1, row++);
        g.add(lYear,   0, row); g.add(year,     1, row++);
        g.add(lMile,   0, row); g.add(mileage,  1, row++);
        g.add(lPlate,  0, row); g.add(plate,    1, row);

        Runnable toggle = () -> {
            ItemType t = typeBox.getValue();
            boolean e = t == ItemType.ELECTRONICS, a = t == ItemType.ART, v = t == ItemType.VEHICLE;
            setVisible(lBrand, brand, e); setVisible(lWarr, warranty, e);
            setVisible(lArtist, artist, a); setVisible(lYear, year, a);
            setVisible(lMile, mileage, v); setVisible(lPlate, plate, v);
        };
        typeBox.valueProperty().addListener((o, ov, nv) -> toggle.run());
        toggle.run();

        dialog.getDialogPane().setContent(g);
        dialog.setResultConverter(btn -> {
            if (btn != addBtn) return null;
            try {
                double p = Double.parseDouble(price.getText().trim());
                Map<String, Object> params = new HashMap<>();
                switch (typeBox.getValue()) {
                    case ELECTRONICS -> { params.put("brand", brand.getText().trim()); params.put("warrantyMonths", Integer.parseInt(warranty.getText().trim())); }
                    case ART         -> { params.put("artistName", artist.getText().trim()); params.put("creationYear", Integer.parseInt(year.getText().trim())); }
                    case VEHICLE     -> { params.put("mileage", Double.parseDouble(mileage.getText().trim())); params.put("licensePlate", plate.getText().trim()); }
                }
                return getSeller().createItem(name.getText().trim(), desc.getText().trim(), p, typeBox.getValue(), params);
            } catch (Exception ex) {
                alert("Lỗi", ex.getMessage()); return null;
            }
        });
        dialog.showAndWait();
        loadData();
    }

    @FXML
    private void handleEditItem() {
        Item sel = itemTable.getSelectionModel().getSelectedItem();
        if (sel == null) { alert("Chưa chọn", "Hãy chọn sản phẩm cần sửa."); return; }

        // KIỂM TRA Ở GIAO DIỆN: Cấm nếu đang RUNNING, FINISHED, hoặc PAID. Bỏ qua OPEN.
        boolean isLocked = getSeller().getAuctions().stream()
                .anyMatch(a -> a.getItem().equals(sel)
                        && (a.getStatus() == com.auction.model.enums.AuctionStatus.RUNNING
                        || a.getStatus() == com.auction.model.enums.AuctionStatus.FINISHED
                        || a.getStatus() == com.auction.model.enums.AuctionStatus.PAID));

        if (isLocked) {
            alert("Lỗi thao tác", "Sản phẩm này đang được đấu giá hoặc đã bán.\nBạn không thể sửa thông tin để đảm bảo tính minh bạch cho người mua!");
            return;
        }

        // --- Phần tạo Dialog sửa sản phẩm ---
        Dialog<Boolean> dialog = new Dialog<>();
        dialog.setTitle("Sửa Sản Phẩm: " + sel.getName());
        ButtonType saveBtn = new ButtonType("Lưu", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveBtn, ButtonType.CANCEL);

        GridPane g = buildGrid();
        TextField tfName  = new TextField(sel.getName());
        TextField tfDesc  = new TextField(sel.getDescription());
        TextField tfPrice = new TextField(String.valueOf(sel.getStartingPrice()));

        g.add(new Label("Tên:"),  0, 0); g.add(tfName,  1, 0);
        g.add(new Label("Mô tả:"),0, 1); g.add(tfDesc,  1, 1);
        g.add(new Label("Giá:"),  0, 2); g.add(tfPrice, 1, 2);
        dialog.getDialogPane().setContent(g);

        dialog.setResultConverter(btn -> {
            if (btn != saveBtn) return null;
            double np = 0;
            try {
                np = Double.parseDouble(tfPrice.getText().trim());
            } catch (Exception ignored) {}

            getSeller().updateItem(sel, tfName.getText(), tfDesc.getText(), np);
            return true;
        });

        dialog.showAndWait();
        loadData();
    }

    @FXML
    private void handleDeleteItem() {
        Item sel = itemTable.getSelectionModel().getSelectedItem();
        if (sel == null) { alert("Chưa chọn", "Hãy chọn sản phẩm cần xóa."); return; }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
            "Xóa sản phẩm \"" + sel.getName() + "\"?", ButtonType.YES, ButtonType.NO);
        confirm.setTitle("Xác nhận xóa");
        confirm.showAndWait().ifPresent(b -> {
            if (b == ButtonType.YES) { getSeller().deleteItem(sel); loadData(); }
        });
    }

    // Auction actions

    @FXML
    private void handleCreateAuction() {
        // Lọc ra các sản phẩm hợp lệ (chưa đấu giá, hoặc phiên cũ đã bị HỦY)
        java.util.List<Item> availableItems = getSeller().getItems().stream()
                .filter(item -> getSeller().getAuctions().stream()
                        .noneMatch(a -> a.getItem().equals(item) &&
                                a.getStatus() != com.auction.model.enums.AuctionStatus.CANCELED))
                .collect(java.util.stream.Collectors.toList());

        // Nếu không có sản phẩm nào rảnh rỗi thì báo lỗi luôn, không mở Dialog nữa
        if (availableItems.isEmpty()) {
            alert("Không có sản phẩm", "Tất cả sản phẩm của bạn đều đang được đấu giá hoặc đã bán. Hãy thêm sản phẩm mới!");
            return;
        }

        Dialog<Boolean> dialog = new Dialog<>();
        dialog.setTitle("Tạo Phiên Đấu Giá");
        ButtonType createBtn = new ButtonType("Tạo", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(createBtn, ButtonType.CANCEL);

        GridPane g = buildGrid();

        // ĐƯA DANH SÁCH ĐÃ LỌC VÀO COMBOBOX THAY VÌ getSeller().getItems()
        ComboBox<Item> itemBox = new ComboBox<>(FXCollections.observableArrayList(availableItems));

        itemBox.setConverter(new javafx.util.StringConverter<>() {
            public String toString(Item i)   { return i != null ? i.getName() : ""; }
            public Item fromString(String s) { return null; }
        });
        itemBox.setValue(availableItems.get(0)); // Gán giá trị mặc định an toàn

        TextField durationField = new TextField("60");
        CheckBox startNow = new CheckBox("Bắt đầu ngay lập tức");
        startNow.setSelected(true);

        //Phần add các thành phần vào GridPane bên dưới giữ nguyên không đổi
        g.add(new Label("Sản phẩm:"),         0, 0); g.add(itemBox,      1, 0);
        g.add(new Label("Thời lượng (phút):"), 0, 1); g.add(durationField,1, 1);
        g.add(startNow, 0, 2, 2, 1);
        dialog.getDialogPane().setContent(g);

        dialog.setResultConverter(btn -> {
            if (btn != createBtn) return null;
            try {
                Item item = itemBox.getValue();
                int mins = Integer.parseInt(durationField.getText().trim());
                LocalDateTime now = LocalDateTime.now();
                Auction a = getSeller().createAuction(item, now, now.plusMinutes(mins));
                AuctionManager.getInstance().registerAuction(a);
                if (startNow.isSelected()) a.startAuction();
                return true;
            } catch (Exception ex) {
                alert("Lỗi", ex.getMessage()); return null;
            }
        });
        dialog.showAndWait();
        loadData();
    }

    @FXML
    private void handleStartAuction() {
        Auction sel = auctionTable.getSelectionModel().getSelectedItem();
        if (sel == null) {
            alert("Chưa chọn", "Hãy chọn phiên đấu giá.");
            return;
        }

        // Kiểm tra nhanh trạng thái trước khi ném cho Manager
        if (sel.getStatus() != com.auction.model.enums.AuctionStatus.OPEN) {
            alert("Thông báo", "Phiên đấu giá này đã được tự động bắt đầu hoặc đã kết thúc!");
            loadData(); // Tự động làm mới giao diện
            return;
        }

        try {
            AuctionManager.getInstance().startAuction(sel);
            loadData();
        } catch (Exception e) {
            alert("Lỗi", e.getMessage());
            loadData(); // Chốt chặn cuối: tải lại dữ liệu nếu có bất kỳ lỗi nào xảy ra
        }
    }


    /**
     * Cập nhật hiển thị tổng số tiền từ các phiên đã PAID
     */
    private void updateTotalEarnings() {
        if (totalEarningsLabel == null) return; // Tránh lỗi nếu giao diện chưa load kịp

        // Tính tổng tiền các phiên đã thanh toán (PAID)
        double total = getSeller().getAuctions().stream()
                .filter(a -> a.getStatus() == com.auction.model.enums.AuctionStatus.PAID)
                .mapToDouble(a -> a.getCurrentHighestPrice())
                .sum();

        // Format số tiền cho đẹp (VD: 15.000.000 đ)
        java.text.NumberFormat format = java.text.NumberFormat.getInstance(new java.util.Locale("vi", "VN"));
        totalEarningsLabel.setText("Tổng doanh thu: " + format.format(total) + " đ");
    }

    //Helpers

    private GridPane buildGrid() {
        GridPane g = new GridPane();
        g.setHgap(10); g.setVgap(8);
        g.setPadding(new Insets(15));
        g.setMinWidth(380);
        return g;
    }

    private void setVisible(Label lbl, TextField tf, boolean visible) {
        lbl.setVisible(visible); lbl.setManaged(visible);
        tf.setVisible(visible);  tf.setManaged(visible);
    }

    private void alert(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION, msg, ButtonType.OK);
        a.setTitle(title); a.setHeaderText(null); a.showAndWait();
    }
}