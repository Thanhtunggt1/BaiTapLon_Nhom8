package com.auction.gui.controller;

import com.auction.gui.SessionManager;
import com.auction.model.entity.*;
import com.auction.model.enums.ItemType;
import com.auction.network.dto.AuctionDto;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Region;

import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

public class SellerController {
    @FXML private Label totalEarningsLabel;
    @FXML private TableView<Item> itemTable;
    @FXML private TableColumn<Item, String> colItemName;
    @FXML private TableColumn<Item, String> colItemType;
    @FXML private TableColumn<Item, String> colItemPrice;
    @FXML private TableColumn<Item, String> colItemDesc;

    @FXML private TableView<AuctionDto> auctionTable;
    @FXML private TableColumn<AuctionDto, String> colAuctionItem;
    @FXML private TableColumn<AuctionDto, String> colAuctionStatus;
    @FXML private TableColumn<AuctionDto, String> colAuctionCurrent;
    @FXML private TableColumn<AuctionDto, String> colAuctionLeader;
    @FXML private TableColumn<AuctionDto, String> colAuctionBids;
    @FXML private TableColumn<AuctionDto, String> colAuctionEnd;

    private static final NumberFormat NF  = NumberFormat.getInstance(new Locale("vi", "VN"));

    @FXML
    public void initialize() {
        setupItemTable();
        setupAuctionTable();
        loadData();
    }

    private Seller getSeller() {
        return (Seller) SessionManager.getCurrentUser();
    }

    private void setupItemTable() {
        String leftAlign = "-fx-alignment: CENTER-LEFT; -fx-padding: 0 0 0 10;";
        colItemName.setStyle(leftAlign);
        colItemType.setStyle(leftAlign);
        colItemPrice.setStyle(leftAlign);
        colItemDesc.setStyle(leftAlign);

        colItemName.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getName()));
        colItemType.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getClass().getSimpleName()));
        colItemPrice.setCellValueFactory(d -> new SimpleStringProperty(NF.format(d.getValue().getStartingPrice()) + " ₫"));
        colItemDesc.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getDescription()));
    }

    private void setupAuctionTable() {
        String leftAlign = "-fx-alignment: CENTER-LEFT; -fx-padding: 0 0 0 10;";
        colAuctionItem.setStyle(leftAlign);
        colAuctionStatus.setStyle(leftAlign);
        colAuctionCurrent.setStyle(leftAlign);
        colAuctionLeader.setStyle(leftAlign);
        colAuctionBids.setStyle(leftAlign);
        colAuctionEnd.setStyle(leftAlign);

        colAuctionItem.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().itemName));
        colAuctionStatus.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().status));
        colAuctionCurrent.setCellValueFactory(d -> new SimpleStringProperty(NF.format(d.getValue().currentPrice) + " ₫"));
        colAuctionLeader.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().currentLeader != null ? d.getValue().currentLeader : "—"));
        colAuctionBids.setCellValueFactory(d -> new SimpleStringProperty(String.valueOf(d.getValue().bidCount)));

        colAuctionEnd.setCellValueFactory(d -> {
            try {
                LocalDateTime dt = LocalDateTime.parse(d.getValue().endTime, DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"));
                return new SimpleStringProperty(dt.format(DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy")));
            } catch (Exception e) {
                return new SimpleStringProperty(d.getValue().endTime);
            }
        });

        colAuctionStatus.setCellFactory(col -> new TableCell<>() {
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

    private void loadData() {
        itemTable.setItems(FXCollections.observableArrayList(getSeller().getItems()));
        itemTable.refresh();

        com.auction.network.Message response = com.auction.network.NetworkClient.getInstance().getAuctions();
        if (response.isSuccess()) {
            List<AuctionDto> dtos = response.getPayload(new com.google.gson.reflect.TypeToken<List<AuctionDto>>(){}.getType());
            String currentUser = SessionManager.getCurrentUserDto().username;

            List<AuctionDto> myAuctions = dtos.stream()
                    .filter(a -> currentUser.equals(a.sellerUsername))
                    .collect(Collectors.toList());

            auctionTable.setItems(FXCollections.observableArrayList(myAuctions));
            auctionTable.refresh();

            double total = myAuctions.stream()
                    .filter(a -> "PAID".equals(a.status))
                    .mapToDouble(a -> a.currentPrice)
                    .sum();
            if (totalEarningsLabel != null) {
                totalEarningsLabel.setText("Tổng doanh thu: " + NF.format(total) + " đ");
            }

            if (MainController.getInstance() != null) {
                MainController.getInstance().refreshBalanceView();
            }
        }
    }

    @FXML private void handleRefreshItems() { loadData(); }
    @FXML private void handleRefreshAuctions() { loadData(); }

    @FXML
    private void handleAddItem() {
        Dialog<Item> dialog = new Dialog<>();
        dialog.setTitle("Thêm Sản Phẩm Mới");
        ButtonType addBtn = new ButtonType("Thêm", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(addBtn, ButtonType.CANCEL);

        GridPane g = buildGrid();
        TextField name = new TextField(); name.setPromptText("Tên sản phẩm");
        TextField desc = new TextField(); desc.setPromptText("Mô tả");
        TextField price = new TextField(); price.setPromptText("Giá khởi điểm (VNĐ)");
        ComboBox<ItemType> typeBox = new ComboBox<>(FXCollections.observableArrayList(ItemType.values()));
        typeBox.setValue(ItemType.ELECTRONICS);

        TextField brand = new TextField("Samsung"); TextField warranty = new TextField("24");
        TextField artist = new TextField(); TextField year = new TextField("2020");
        TextField mileage = new TextField("0"); TextField plate = new TextField();

        Label lBrand = new Label("Thương hiệu:"); Label lWarr = new Label("Bảo hành (tháng):");
        Label lArtist= new Label("Nghệ sĩ:"); Label lYear = new Label("Năm sáng tác:");
        Label lMile = new Label("Số km:"); Label lPlate = new Label("Biển số:");

        int row = 0;
        g.add(new Label("Loại sản phẩm:"), 0, row); g.add(typeBox, 1, row++);
        g.add(new Label("Tên:"), 0, row); g.add(name, 1, row++);
        g.add(new Label("Mô tả:"), 0, row); g.add(desc, 1, row++);
        g.add(new Label("Giá khởi điểm:"), 0, row); g.add(price, 1, row++);
        g.add(lBrand, 0, row); g.add(brand, 1, row++); g.add(lWarr, 0, row); g.add(warranty, 1, row++);
        g.add(lArtist, 0, row); g.add(artist, 1, row++); g.add(lYear, 0, row); g.add(year, 1, row++);
        g.add(lMile, 0, row); g.add(mileage, 1, row++); g.add(lPlate, 0, row); g.add(plate, 1, row);

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
                    case ART -> { params.put("artistName", artist.getText().trim()); params.put("creationYear", Integer.parseInt(year.getText().trim())); }
                    case VEHICLE -> { params.put("mileage", Double.parseDouble(mileage.getText().trim())); params.put("licensePlate", plate.getText().trim()); }
                }

                com.auction.network.dto.CreateItemDto dto = new com.auction.network.dto.CreateItemDto();
                dto.name = name.getText().trim(); dto.description = desc.getText().trim();
                dto.startingPrice = p; dto.itemType = typeBox.getValue().toString(); dto.params = params;

                com.auction.network.Message res = com.auction.network.NetworkClient.getInstance().createItem(dto);
                if (res.isSuccess()) {
                    Map<String, Object> map = res.getPayload(Map.class);
                    String serverId = (String) map.get("id");

                    Item localItem = getSeller().createItem(dto.name, dto.description, p, typeBox.getValue(), params);
                    java.lang.reflect.Field idField = Entity.class.getDeclaredField("id");
                    idField.setAccessible(true);
                    idField.set(localItem, serverId);
                    return localItem;
                } else {
                    alert("Lỗi Server", res.getErrorMessage()); return null;
                }
            } catch (Exception ex) { alert("Lỗi", ex.getMessage()); return null; }
        });
        dialog.showAndWait();
        loadData();
    }

    @FXML
    private void handleEditItem() {
        Item sel = itemTable.getSelectionModel().getSelectedItem();
        if (sel == null) { alert("Chưa chọn", "Hãy chọn sản phẩm cần sửa."); return; }

        boolean isLocked = auctionTable.getItems().stream()
                .anyMatch(dto -> dto.itemName.equals(sel.getName()) &&
                        ("RUNNING".equals(dto.status) || "FINISHED".equals(dto.status) || "PAID".equals(dto.status)));

        if (isLocked) { alert("Lỗi thao tác", "Sản phẩm này đang được đấu giá hoặc đã bán."); return; }

        Dialog<Boolean> dialog = new Dialog<>();
        dialog.setTitle("Sửa Sản Phẩm: " + sel.getName());
        ButtonType saveBtn = new ButtonType("Lưu", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveBtn, ButtonType.CANCEL);

        GridPane g = buildGrid();
        TextField tfName = new TextField(sel.getName()); TextField tfDesc = new TextField(sel.getDescription());
        TextField tfPrice = new TextField(String.format(java.util.Locale.US, "%.0f", sel.getStartingPrice()));
        g.add(new Label("Tên:"), 0, 0); g.add(tfName, 1, 0); g.add(new Label("Mô tả:"), 0, 1); g.add(tfDesc, 1, 1); g.add(new Label("Giá:"), 0, 2); g.add(tfPrice, 1, 2);
        dialog.getDialogPane().setContent(g);

        dialog.setResultConverter(btn -> {
            if (btn != saveBtn) return null;
            double np = 0; try { np = Double.parseDouble(tfPrice.getText().trim()); } catch (Exception ignored) {}

            com.auction.network.Message res = com.auction.network.NetworkClient.getInstance().updateItem(sel.getId(), tfName.getText(), tfDesc.getText(), np);

            if (res.isSuccess()) {
                getSeller().updateItem(sel, tfName.getText(), tfDesc.getText(), np);
                return true;
            } else {
                alert("Lỗi Server", res.getErrorMessage());
                return null;
            }
        });
        dialog.showAndWait();
        loadData();
    }

    @FXML
    private void handleDeleteItem() {
        Item sel = itemTable.getSelectionModel().getSelectedItem();
        if (sel == null) { alert("Chưa chọn", "Hãy chọn sản phẩm cần xóa."); return; }

        boolean isLocked = auctionTable.getItems().stream()
                .anyMatch(dto -> dto.itemName.equals(sel.getName()) &&
                        ("RUNNING".equals(dto.status) || "FINISHED".equals(dto.status) || "PAID".equals(dto.status)));

        if (isLocked) {
            alert("Lỗi thao tác", "Không thể xóa! Sản phẩm này đang được đấu giá hoặc đã bán."); return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Xóa sản phẩm \"" + sel.getName() + "\"?", ButtonType.YES, ButtonType.NO);
        confirm.showAndWait().ifPresent(b -> {
            if (b == ButtonType.YES) {
                getSeller().deleteItem(sel);
                loadData();
            }
        });
    }

    @FXML
    private void handleCreateAuction() {
        List<Item> availableItems = getSeller().getItems().stream()
                .filter(item -> auctionTable.getItems().stream()
                        .noneMatch(dto -> dto.itemName.equals(item.getName()) && !"CANCELED".equals(dto.status)))
                .collect(Collectors.toList());

        if (availableItems.isEmpty()) { alert("Không có sản phẩm", "Tất cả sản phẩm đều đang được đấu giá hoặc đã bán."); return; }

        Dialog<Boolean> dialog = new Dialog<>();
        dialog.setTitle("Tạo Phiên Đấu Giá");
        ButtonType createBtn = new ButtonType("Tạo", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(createBtn, ButtonType.CANCEL);

        GridPane g = buildGrid();
        ComboBox<Item> itemBox = new ComboBox<>(FXCollections.observableArrayList(availableItems));
        itemBox.setConverter(new javafx.util.StringConverter<>() {
            public String toString(Item i) { return i != null ? i.getName() : ""; }
            public Item fromString(String s) { return null; }
        });
        itemBox.setValue(availableItems.get(0));

        TextField durationField = new TextField("60");
        CheckBox startNow = new CheckBox("Bắt đầu ngay lập tức"); startNow.setSelected(true);

        g.add(new Label("Sản phẩm:"), 0, 0); g.add(itemBox, 1, 0);
        g.add(new Label("Thời lượng (phút):"), 0, 1); g.add(durationField, 1, 1);
        g.add(startNow, 0, 2, 2, 1);
        dialog.getDialogPane().setContent(g);

        dialog.setResultConverter(btn -> {
            if (btn != createBtn) return null;
            try {
                Item item = itemBox.getValue();
                int mins = Integer.parseInt(durationField.getText().trim());

                com.auction.network.dto.CreateAuctionDto dto = new com.auction.network.dto.CreateAuctionDto();
                dto.itemId = item.getId();
                dto.durationMinutes = mins;
                dto.startNow = startNow.isSelected();

                com.auction.network.Message res = com.auction.network.NetworkClient.getInstance().createAuction(dto);
                if (res.isSuccess()) {
                    loadData();
                    return true;
                } else {
                    alert("Lỗi Server", res.getErrorMessage()); return null;
                }
            } catch (Exception ex) { alert("Lỗi", ex.getMessage()); return null; }
        });
        dialog.showAndWait();
    }

    @FXML
    private void handleStartAuction() {
        AuctionDto sel = auctionTable.getSelectionModel().getSelectedItem();
        if (sel == null) {
            alert("Chưa chọn", "Hãy chọn phiên đấu giá.");
            return;
        }

        if (!"OPEN".equals(sel.status)) {
            alert("Thông báo", "Chỉ có thể bắt đầu phiên đang ở trạng thái OPEN!");
            return;
        }

        com.auction.network.Message res = com.auction.network.NetworkClient.getInstance().startAuction(sel.id);
        if (res.isSuccess()) {
            alert("Thành công", "Phiên đấu giá đã được kích hoạt thành công!");
            loadData();
        } else {
            alert("Lỗi Server", res.getErrorMessage());
        }
    }

    @FXML
    private void handleCancelOrEndEarly() {
        AuctionDto sel = auctionTable.getSelectionModel().getSelectedItem();
        if (sel == null) { alert("Chưa chọn", "Hãy chọn phiên đấu giá cần xử lý."); return; }

        if (!"RUNNING".equals(sel.status) && !"OPEN".equals(sel.status)) {
            alert("Không hợp lệ", "Chỉ có thể can thiệp vào các phiên đang Mở hoặc Đang chạy."); return;
        }

        long minutesRemaining = 0;
        try {
            LocalDateTime end = LocalDateTime.parse(sel.endTime, DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"));
            minutesRemaining = java.time.temporal.ChronoUnit.MINUTES.between(LocalDateTime.now(), end);
        } catch(Exception e){}

        boolean hasBids = sel.currentLeader != null;

        if (minutesRemaining >= 60) {
            if (!hasBids) {
                if (confirm("Xác nhận", "Chưa có ai đặt giá. Bạn có chắc chắn muốn hủy bài không?")) {
                    com.auction.network.Message res = com.auction.network.NetworkClient.getInstance().cancelAuction(sel.id);
                    if (res.isSuccess()) loadData();
                    else alert("Lỗi Server", res.getErrorMessage());
                }
            } else {
                ChoiceDialog<String> dialog = new ChoiceDialog<>("Hủy toàn bộ (chịu phí phạt)", "Hủy toàn bộ (chịu phí phạt)", "Kết thúc sớm và bán cho người cao nhất");
                dialog.setTitle("Lựa chọn xử lý"); dialog.setHeaderText("Bạn có 2 lựa chọn (Còn " + minutesRemaining + " phút):"); dialog.setContentText("Hành động:");
                dialog.showAndWait().ifPresent(choice -> {
                    if (choice.equals("Hủy toàn bộ (chịu phí phạt)")) {
                        com.auction.network.Message res = com.auction.network.NetworkClient.getInstance().cancelAuction(sel.id);
                        if (res.isSuccess()) alert("Đã hủy", "Phiên đấu giá đã bị hủy.");
                        else alert("Lỗi Server", res.getErrorMessage());
                    } else {
                        com.auction.network.Message res = com.auction.network.NetworkClient.getInstance().endAuction(sel.id);
                        if (res.isSuccess()) alert("Đã kết thúc", "Đã chốt bán sớm!");
                        else alert("Lỗi Server", res.getErrorMessage());
                    }
                    loadData();
                });
            }
        } else {
            if (!hasBids) {
                if (confirm("Xác nhận", "Chưa có ai đặt giá. Kết thúc sớm phiên?")) {
                    com.auction.network.Message res = com.auction.network.NetworkClient.getInstance().cancelAuction(sel.id);
                    if (res.isSuccess()) loadData();
                    else alert("Lỗi Server", res.getErrorMessage());
                }
            } else {
                // --- SỬA Ở ĐÂY: Hiện đầy đủ chữ cho thông báo Bắt buộc bán ---
                if (confirm("Bắt buộc bán", "Thời gian còn dưới 60 phút và đã có người đặt giá.\nBạn có muốn KẾT THÚC PHIÊN NGAY BÂY GIỜ để chốt bán không?")) {
                    com.auction.network.Message res = com.auction.network.NetworkClient.getInstance().endAuction(sel.id);
                    if (res.isSuccess()) {
                        alert("Đã kết thúc", "Đã chốt bán sớm thành công!");
                        loadData();
                    } else alert("Lỗi Server", res.getErrorMessage());
                }
            }
        }
    }

    private boolean confirm(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.CONFIRMATION, msg, ButtonType.YES, ButtonType.NO);
        a.setTitle(title);
        a.setHeaderText(null);
        // ÉP GIÃN NỞ ĐỂ HIỆN ĐỦ NỘI DUNG
        a.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
        java.util.Optional<ButtonType> res = a.showAndWait();
        return res.isPresent() && res.get() == ButtonType.YES;
    }

    private GridPane buildGrid() {
        GridPane g = new GridPane(); g.setHgap(10); g.setVgap(8); g.setPadding(new Insets(15)); g.setMinWidth(380); return g;
    }

    private void setVisible(Label lbl, TextField tf, boolean visible) {
        lbl.setVisible(visible); lbl.setManaged(visible); tf.setVisible(visible); tf.setManaged(visible);
    }

    private void alert(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION, msg, ButtonType.OK);
        a.setTitle(title);
        a.setHeaderText(null);
        // ÉP GIÃN NỞ ĐỂ HIỆN ĐỦ NỘI DUNG
        a.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
        a.showAndWait();
    }
}