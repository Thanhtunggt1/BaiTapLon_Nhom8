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
import javafx.stage.FileChooser;
import java.io.File;
import java.nio.file.Files;
import java.util.Base64;
import javafx.scene.layout.HBox;

import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
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
    @FXML private TableColumn<AuctionDto, String> colAuctionCurrent;
    @FXML private TableColumn<AuctionDto, String> colAuctionLeader;
    @FXML private TableColumn<AuctionDto, String> colAuctionStatus;
    @FXML private TableColumn<AuctionDto, String> colAuctionEnd;

    private static final NumberFormat NF = NumberFormat.getInstance(new Locale("vi", "VN"));
    private static final DateTimeFormatter DF = DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy");

    @FXML
    public void initialize() {
        setupItemTable();
        setupAuctionTable();
        loadData();

        // Tự động làm mới dữ liệu ngầm mỗi 3 giây để cập nhật trạng thái realtime
        javafx.animation.Timeline autoRefresh = new javafx.animation.Timeline(
                new javafx.animation.KeyFrame(javafx.util.Duration.seconds(3), e -> loadData())
        );
        autoRefresh.setCycleCount(javafx.animation.Timeline.INDEFINITE);
        autoRefresh.play();
    }

    private void setupItemTable() {
        colItemName.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getName()));
        colItemType.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getClass().getSimpleName().toUpperCase()));
        colItemPrice.setCellValueFactory(d -> new SimpleStringProperty(NF.format(d.getValue().getStartingPrice()) + " đ"));
        colItemDesc.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getDescription()));
    }

    private void setupAuctionTable() {
        colAuctionItem.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().itemName));
        colAuctionCurrent.setCellValueFactory(d -> new SimpleStringProperty(NF.format(d.getValue().currentPrice) + " đ"));
        colAuctionLeader.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().currentLeader != null ? d.getValue().currentLeader : "—"));
        colAuctionStatus.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().status));
        colAuctionEnd.setCellValueFactory(d -> {
            try {
                LocalDateTime dt = LocalDateTime.parse(d.getValue().endTime, DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"));
                return new SimpleStringProperty(dt.format(DF));
            } catch(Exception e) {
                return new SimpleStringProperty(d.getValue().endTime);
            }
        });
    }

    private void loadData() {
        // CHẶN LỖI: Kiểm tra vai trò người dùng, nếu không phải SELLER thì thoát ngay lập tức
        com.auction.network.dto.UserDto currentUserDto = SessionManager.getCurrentUserDto();
        if (currentUserDto == null || !"SELLER".equals(currentUserDto.role)) {
            return;
        }

        Seller seller = getSeller();
        if (seller == null) {
            return;
        }

        // Ghi nhớ các dòng đang được click bôi đen trước khi bảng cập nhật
        Item selectedItem = itemTable.getSelectionModel().getSelectedItem();
        AuctionDto selectedAuction = auctionTable.getSelectionModel().getSelectedItem();

        List<Item> items = seller.getItems();

        // Tạo Thread riêng để gọi dữ liệu mạng từ Server ngầm
        new Thread(() -> {
            com.auction.network.Message response = com.auction.network.NetworkClient.getInstance().getAuctions();

            // Đồng bộ kết quả hiển thị lại lên luồng giao diện chính của JavaFX
            javafx.application.Platform.runLater(() -> {
                itemTable.setItems(FXCollections.observableArrayList(items));
                if (selectedItem != null) {
                    itemTable.getSelectionModel().select(selectedItem);
                }
                itemTable.refresh();

                if (response.isSuccess()) {
                    List<AuctionDto> dtos = response.getPayload(new com.google.gson.reflect.TypeToken<List<AuctionDto>>(){}.getType());
                    String currentUser = currentUserDto.username;

                    List<AuctionDto> myAuctions = dtos.stream()
                            .filter(a -> currentUser.equals(a.sellerUsername))
                            .collect(Collectors.toList());

                    auctionTable.setItems(FXCollections.observableArrayList(myAuctions));

                    if (selectedAuction != null) {
                        myAuctions.stream().filter(a -> a.id.equals(selectedAuction.id))
                                .findFirst().ifPresent(a -> auctionTable.getSelectionModel().select(a));
                    }
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
            });
        }).start();
    }

    private Seller getSeller() {
        User localUser = SessionManager.getCurrentUser();
        if (localUser instanceof Seller) {
            return (Seller) localUser;
        }
        return null;
    }

    @FXML private void handleRefreshItems() { loadData(); }
    @FXML private void handleRefreshAuctions() { loadData(); }

    @FXML
    private void handleAddItem() {
        Dialog<Map<String, Object>> diag = new Dialog<>();
        diag.setTitle("Thêm Sản Phẩm Mới");
        diag.setHeaderText("Nhập thông tin sản phẩm");
        ButtonType btnOk = new ButtonType("Lưu", ButtonBar.ButtonData.OK_DONE);
        diag.getDialogPane().getButtonTypes().addAll(btnOk, ButtonType.CANCEL);

        GridPane g = buildGrid();
        TextField tfName = new TextField();
        TextArea taDesc = new TextArea(); taDesc.setPrefRowCount(3);
        TextField tfPrice = new TextField();
        ComboBox<ItemType> cbType = new ComboBox<>(FXCollections.observableArrayList(ItemType.values()));
        cbType.setValue(ItemType.ELECTRONICS);

        Label lbl1 = new Label("Thương hiệu:"), lbl2 = new Label("Bảo hành (tháng):");
        TextField tfParam1 = new TextField(), tfParam2 = new TextField();

        cbType.valueProperty().addListener((obs, oldV, newV) -> {
            if (newV == ItemType.ELECTRONICS) {
                lbl1.setText("Thương hiệu:"); lbl2.setText("Bảo hành (tháng):");
                setVisible(lbl1, tfParam1, true); setVisible(lbl2, tfParam2, true);
            } else if (newV == ItemType.ART) {
                lbl1.setText("Họa sĩ:"); lbl2.setText("Năm sáng tác:");
                setVisible(lbl1, tfParam1, true); setVisible(lbl2, tfParam2, true);
            } else {
                lbl1.setText("Số Km:"); lbl2.setText("Biển số xe:");
                setVisible(lbl1, tfParam1, true); setVisible(lbl2, tfParam2, true);
            }
        });

        g.add(new Label("Tên sản phẩm:"), 0, 0); g.add(tfName, 1, 0);
        g.add(new Label("Mô tả:"), 0, 1); g.add(taDesc, 1, 1);
        g.add(new Label("Giá khởi điểm:"), 0, 2); g.add(tfPrice, 1, 2);
        g.add(new Label("Loại hàng:"), 0, 3); g.add(cbType, 1, 3);
        g.add(lbl1, 0, 4); g.add(tfParam1, 1, 4);
        g.add(lbl2, 0, 5); g.add(tfParam2, 1, 5);

        List<String> base64Images = new ArrayList<>();
        Button btnImg = new Button("Chọn ảnh (Tối đa 5)");
        Label lblImgCount = new Label("Đã chọn 0 ảnh");
        btnImg.setOnAction(e -> {
            FileChooser fc = new FileChooser();
            fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg"));
            List<File> files = fc.showOpenMultipleDialog(btnImg.getScene().getWindow());
            if (files != null) {
                base64Images.clear();
                int count = Math.min(files.size(), 5);
                for (int i = 0; i < count; i++) {
                    try {
                        byte[] bytes = Files.readAllBytes(files.get(i).toPath());
                        base64Images.add(Base64.getEncoder().encodeToString(bytes));
                    } catch (Exception ex) { ex.printStackTrace(); }
                }
                lblImgCount.setText("Đã chọn " + base64Images.size() + " ảnh");
            }
        });
        g.add(new Label("Hình ảnh:"), 0, 6);
        g.add(new HBox(10, btnImg, lblImgCount), 1, 6);

        diag.getDialogPane().setContent(g);
        diag.setResultConverter(b -> {
            if (b == btnOk) {
                Map<String, Object> res = new HashMap<>();
                res.put("name", tfName.getText().trim());
                res.put("desc", taDesc.getText().trim());
                res.put("price", tfPrice.getText().trim());
                res.put("type", cbType.getValue());
                res.put("p1", tfParam1.getText().trim());
                res.put("p2", tfParam2.getText().trim());
                res.put("images", base64Images);
                return res;
            }
            return null;
        });

        diag.showAndWait().ifPresent(res -> {
            try {
                String name = (String) res.get("name");
                String desc = (String) res.get("desc");
                double price = Double.parseDouble((String) res.get("price"));
                ItemType type = (ItemType) res.get("type");
                String p1 = (String) res.get("p1");
                String p2 = (String) res.get("p2");
                List<String> images = (List<String>) res.get("images");

                if (name.isEmpty()) throw new IllegalArgumentException("Tên không được trống.");

                Map<String, Object> params = new HashMap<>();
                if (type == ItemType.ELECTRONICS) {
                    params.put("brand", p1); params.put("warrantyMonths", Integer.parseInt(p2));
                } else if (type == ItemType.ART) {
                    params.put("artistName", p1); params.put("creationYear", Integer.parseInt(p2));
                } else {
                    params.put("mileage", Double.parseDouble(p1)); params.put("licensePlate", p2);
                }

                com.auction.network.dto.CreateItemDto dto = new com.auction.network.dto.CreateItemDto();
                dto.name = name; dto.description = desc; dto.startingPrice = price;
                dto.itemType = type.name(); dto.params = params; dto.imagesBase64 = images;

                com.auction.network.Message msg = com.auction.network.NetworkClient.getInstance().createItem(dto);
                if (msg.isSuccess()) {
                    Map<String, String> payload = msg.getPayload(new com.google.gson.reflect.TypeToken<Map<String, String>>(){}.getType());
                    String generatedId = payload.get("id");
                    Seller s = getSeller();
                    if (s != null) {
                        Item item = s.createItem(name, desc, price, type, params);
                        item.setImagesBase64(images);
                        java.lang.reflect.Field f = Entity.class.getDeclaredField("id");
                        f.setAccessible(true); f.set(item, generatedId);
                    }
                    alert("Thành công", "Đã thêm sản phẩm lên hệ thống!");
                    loadData();
                } else alert("Lỗi", msg.getErrorMessage());
            } catch(Exception ex) { alert("Lỗi dữ liệu", "Vui lòng điền đúng định dạng số."); }
        });
    }

    @FXML
    private void handleEditItem() {
        Item sel = itemTable.getSelectionModel().getSelectedItem();
        if (sel == null) { alert("Thông báo", "Vui lòng chọn sản phẩm cần sửa."); return; }

        Dialog<Map<String, String>> diag = new Dialog<>();
        diag.setTitle("Sửa Sản Phẩm");
        ButtonType btnOk = new ButtonType("Cập nhật", ButtonBar.ButtonData.OK_DONE);
        diag.getDialogPane().getButtonTypes().addAll(btnOk, ButtonType.CANCEL);

        GridPane g = buildGrid();
        TextField tfName = new TextField(sel.getName());
        TextArea taDesc = new TextArea(sel.getDescription()); taDesc.setPrefRowCount(3);
        TextField tfPrice = new TextField(String.valueOf(sel.getStartingPrice()));

        g.add(new Label("Tên sản phẩm:"), 0, 0); g.add(tfName, 1, 0);
        g.add(new Label("Mô tả:"), 0, 1); g.add(taDesc, 1, 1);
        g.add(new Label("Giá khởi điểm:"), 0, 2); g.add(tfPrice, 1, 2);

        diag.getDialogPane().setContent(g);
        diag.setResultConverter(b -> {
            if (b == btnOk) {
                Map<String, String> r = new HashMap<>();
                r.put("name", tfName.getText().trim()); r.put("desc", taDesc.getText().trim()); r.put("price", tfPrice.getText().trim());
                return r;
            }
            return null;
        });

        diag.showAndWait().ifPresent(res -> {
            try {
                String name = res.get("name"); String desc = res.get("desc"); double price = Double.parseDouble(res.get("price"));
                com.auction.network.Message msg = com.auction.network.NetworkClient.getInstance().updateItem(sel.getId(), name, desc, price);
                if (msg.isSuccess()) {
                    Seller s = getSeller();
                    if (s != null) s.updateItem(sel, name, desc, price);
                    alert("Thành công", "Đã cập nhật sản phẩm.");
                    loadData();
                } else alert("Lỗi", msg.getErrorMessage());
            } catch (Exception ex) { alert("Lỗi", ex.getMessage()); }
        });
    }

    @FXML
    private void handleDeleteItem() {
        Item sel = itemTable.getSelectionModel().getSelectedItem();
        if (sel == null) return;
        alert("Thông báo", "Tính năng xóa cục bộ tạm thời chưa đồng bộ API Server.");
    }

    @FXML
    private void handleCreateAuction() {
        Item sel = itemTable.getSelectionModel().getSelectedItem();
        if (sel == null) { alert("Thông báo", "Vui lòng chọn 1 sản phẩm trong kho để đấu giá."); return; }

        Dialog<Map<String, Object>> diag = new Dialog<>();
        diag.setTitle("Tạo Phiên Đấu Giá");
        ButtonType btnOk = new ButtonType("Tạo", ButtonBar.ButtonData.OK_DONE);
        diag.getDialogPane().getButtonTypes().addAll(btnOk, ButtonType.CANCEL);

        GridPane g = buildGrid();
        TextField tfDuration = new TextField("60");
        CheckBox chkStartNow = new CheckBox("Bắt đầu ngay lập tức (RUNNING)"); chkStartNow.setSelected(true);

        g.add(new Label("Sản phẩm:"), 0, 0); g.add(new Label(sel.getName()), 1, 0);
        g.add(new Label("Thời gian chạy (Phút):"), 0, 1); g.add(tfDuration, 1, 1);
        g.add(chkStartNow, 1, 2);

        diag.getDialogPane().setContent(g);
        diag.setResultConverter(b -> {
            if (b == btnOk) {
                Map<String, Object> r = new HashMap<>();
                r.put("dur", tfDuration.getText().trim()); r.put("now", chkStartNow.isSelected());
                return r;
            }
            return null;
        });

        diag.showAndWait().ifPresent(res -> {
            try {
                int dur = Integer.parseInt((String) res.get("dur"));
                boolean now = (Boolean) res.get("now");

                com.auction.network.dto.CreateAuctionDto dto = new com.auction.network.dto.CreateAuctionDto();
                dto.itemId = sel.getId(); dto.durationMinutes = dur; dto.startNow = now;

                com.auction.network.Message msg = com.auction.network.NetworkClient.getInstance().createAuction(dto);
                if (msg.isSuccess()) {
                    alert("Thành công", "Đã khởi tạo phiên đấu giá trên hệ thống!");
                    loadData();
                } else alert("Thất bại", msg.getErrorMessage());
            } catch(Exception ex) { alert("Lỗi nhập liệu", "Vui lòng điền đúng số phút."); }
        });
    }

    @FXML
    private void handleStartAuction() {
        AuctionDto sel = auctionTable.getSelectionModel().getSelectedItem();
        if (sel == null) { alert("Thông báo", "Vui lòng chọn một phiên đấu giá OPEN."); return; }
        if (!"OPEN".equals(sel.status)) { alert("Lỗi hành động", "Phiên đấu giá này đã hoặc đang chạy rồi."); return; }

        com.auction.network.Message res = com.auction.network.NetworkClient.getInstance().startAuction(sel.id);
        if (res.isSuccess()) {
            alert("Kích hoạt thành công", "Phiên đấu giá đã chuyển sang trạng thái RUNNING.");
            loadData();
        } else {
            alert("Lỗi máy chủ", res.getErrorMessage());
        }
    }

    @FXML
    private void handleCancelOrEndEarly() {
        AuctionDto sel = auctionTable.getSelectionModel().getSelectedItem();
        if (sel == null) { alert("Thông báo", "Vui lòng chọn một phiên đấu giá trong bảng."); return; }

        if ("OPEN".equals(sel.status)) {
            if (confirm("Hủy phiên", "Bạn có chắc chắn muốn HỦY phiên đấu giá chưa diễn ra này không?")) {
                com.auction.network.Message res = com.auction.network.NetworkClient.getInstance().cancelAuction(sel.id);
                if (res.isSuccess()) {
                    alert("Đã hủy", "Đã hủy phiên đấu giá thành công.");
                    loadData();
                } else alert("Lỗi Server", res.getErrorMessage());
            }
        } else if ("RUNNING".equals(sel.status)) {
            if (sel.bidCount == 0) {
                if (confirm("Hủy phiên", "Phiên đang chạy và chưa có ai đặt giá. Bạn có muốn HỦY không?")) {
                    com.auction.network.Message res = com.auction.network.NetworkClient.getInstance().cancelAuction(sel.id);
                    if (res.isSuccess()) {
                        alert("Đã hủy", "Đã hủy phiên thành công.");
                        loadData();
                    } else alert("Lỗi Server", res.getErrorMessage());
                }
            } else {
                if (confirm("Chốt bán sớm", "Phiên đang diễn ra và ĐÃ CÓ NGƯỜI ĐẶT GIÁ. Bạn có chắc chắn muốn KẾT THÚC NGAY BÂY GIỜ để chốt bán không?")) {
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
        a.setTitle(title); a.setHeaderText(null);
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
        Alert a = new Alert(Alert.AlertType.INFORMATION); a.setTitle(title); a.setHeaderText(null); a.setContentText(msg); a.showAndWait();
    }
}