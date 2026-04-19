package com.auction.gui;

import com.auction.model.entity.Item;
import com.auction.model.entity.Seller;
import com.auction.model.enums.ItemType;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.util.HashMap;
import java.util.Map;

public class SellerDashboard {
    private BorderPane view;
    private Seller currentSeller;
    private ObservableList<Item> itemList;
    private ListView<Item> itemListView;

    public SellerDashboard() {
        this.currentSeller = (Seller) MainApp.getCurrentUser();
        this.itemList = FXCollections.observableArrayList(currentSeller.getItems());
        
        view = new BorderPane();
        view.setPadding(new Insets(15));

        HBox header = new HBox(20);
        Label lblWelcome = new Label("Xin chào Seller: " + currentSeller.getUsername());
        Button btnLogout = new Button("Đăng xuất");
        btnLogout.setOnAction(e -> MainApp.showLoginScreen());
        header.getChildren().addAll(lblWelcome, btnLogout);
        view.setTop(header);

        itemListView = new ListView<>(itemList);
        itemListView.setCellFactory(param -> new ListCell<>() {
            @Override
            protected void updateItem(Item item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.getName() + " - Khởi điểm: " + item.getStartingPrice());
                }
            }
        });
        view.setCenter(itemListView);

        VBox formBox = createItemForm();
        view.setBottom(formBox);
    }

    private VBox createItemForm() {
        VBox form = new VBox(10);
        form.setPadding(new Insets(10, 0, 0, 0));

        TextField txtName = new TextField(); txtName.setPromptText("Tên sản phẩm");
        TextField txtDesc = new TextField(); txtDesc.setPromptText("Mô tả");
        TextField txtPrice = new TextField(); txtPrice.setPromptText("Giá khởi điểm");
        
        HBox actionBox = new HBox(10);
        Button btnAdd = new Button("Thêm Sản Phẩm Mới");
        Button btnDelete = new Button("Xóa SP Chọn");

        btnAdd.setOnAction(e -> {
            try {
                Map<String, Object> params = new HashMap<>();
                params.put("brand", "Generic Brand");
                params.put("warrantyMonths", 12);
                
                Item newItem = currentSeller.createItem(
                        txtName.getText(), txtDesc.getText(),
                        Double.parseDouble(txtPrice.getText()), 
                        ItemType.ELECTRONICS, params
                );
                itemList.add(newItem);
                txtName.clear(); txtDesc.clear(); txtPrice.clear();
            } catch (Exception ex) {
                System.out.println("Lỗi nhập liệu: " + ex.getMessage());
            }
        });

        btnDelete.setOnAction(e -> {
            Item selected = itemListView.getSelectionModel().getSelectedItem();
            if (selected != null) {
                if(currentSeller.deleteItem(selected)) {
                    itemList.remove(selected);
                }
            }
        });

        actionBox.getChildren().addAll(btnAdd, btnDelete);
        form.getChildren().addAll(new Label("Quản lý sản phẩm:"), txtName, txtDesc, txtPrice, actionBox);
        return form;
    }

    public BorderPane getView() {
        return view;
    }
}