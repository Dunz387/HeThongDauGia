package view.utility;

import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TableView;
import javafx.scene.control.Dialog;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.TextField;
import javafx.scene.control.TextArea;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Alert;
import javafx.scene.layout.GridPane;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import model.auction.Auction;
import network.ClientNetworkManager;
import network.SessionManager;
import shared.Protocol;
import java.time.Duration;
import java.time.LocalDateTime;

/**
 * Helper để cấu hình Context Menu cho bảng Auction.
 * Cung cấp tính năng Sửa và Xóa phiên đấu giá cho Seller và Admin.
 * Trích xuất từ RoomMenuChoiceController và AssetsListController (DRY).
 */
public class AuctionContextMenuHelper {

    public static void setupContextMenu(TableView<Auction> table) {
        ContextMenu contextMenu = new ContextMenu();
        MenuItem editItem = new MenuItem("Sửa thông tin phiên đấu giá");
        MenuItem deleteItem = new MenuItem("Xóa phiên đấu giá");

        // Bidder không có quyền thao tác trên menu này
        if (SessionManager.getInstance().isBidder()) {
            return;
        }

        editItem.setOnAction(e -> {
            Auction selected = table.getSelectionModel().getSelectedItem();
            if (selected != null) {
                boolean isAdmin = SessionManager.getInstance().isAdmin();
                boolean isOwner = selected.getItem().getOwner() != null && 
                                 selected.getItem().getOwner().getId().equals(SessionManager.getInstance().getUserId());

                if (!isAdmin && !isOwner) {
                    AlertHelper.showWarning("Cảnh báo", "Bạn chỉ có thể sửa thông tin của chính mình!");
                    return;
                }

                showEditDialog(selected, isAdmin);
            }
        });

        deleteItem.setOnAction(e -> {
            Auction selected = table.getSelectionModel().getSelectedItem();
            if (selected != null) {
                boolean isAdmin = SessionManager.getInstance().isAdmin();
                boolean isOwner = selected.getItem().getOwner() != null && 
                                 selected.getItem().getOwner().getId().equals(SessionManager.getInstance().getUserId());

                if (!isAdmin && !isOwner) {
                    AlertHelper.showWarning("Cảnh báo", "Bạn chỉ có thể xóa phiên của chính mình!");
                    return;
                }

                Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
                confirm.setTitle("Xác nhận Xóa");
                confirm.setHeaderText("Bạn có chắc muốn xóa phiên đấu giá này không?");
                confirm.setContentText("Mã: " + selected.getId() + "\nLưu ý: Seller chỉ xóa được nếu chưa có ai đặt giá.");
                confirm.showAndWait().ifPresent(res -> {
                    if (res == ButtonType.OK) {
                        String deleteCommand = (isAdmin ? Protocol.REQ_DELETE_AUCTION : Protocol.REQ_DELETE_ITEM) 
                                             + Protocol.DELIMITER + selected.getId();
                        ClientNetworkManager.getInstance().sendData(deleteCommand);
                    }
                });
            }
        });

        contextMenu.getItems().addAll(editItem, deleteItem);
        table.setContextMenu(contextMenu);
    }

    private static void showEditDialog(Auction selected, boolean isAdmin) {
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Sửa phiên đấu giá");
        dialog.setHeaderText("Chỉnh sửa thông tin phiên: " + selected.getId());

        ButtonType updateButtonType = new ButtonType("Cập nhật", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(updateButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField nameField = new TextField(selected.getItem().getName());
        TextArea descField = new TextArea(selected.getItem().getDescription());
        descField.setPrefRowCount(3);
        
        ComboBox<String> typeBox = new ComboBox<>(FXCollections.observableArrayList("ELECTRONICS", "ART", "VEHICLE"));
        String currentType = model.item.ItemFactory.getItemTypeString(selected.getItem());
        typeBox.setValue(currentType);

        TextField priceField = new TextField(String.valueOf(selected.getStartingPrice()));
        priceField.setDisable(!isAdmin); // Chỉ ADMIN mới được sửa giá

        TextField timeField = new TextField(String.valueOf(Duration.between(LocalDateTime.now(), selected.getEndTime()).toMinutes()));
        timeField.setDisable(!isAdmin); // Chỉ ADMIN mới được sửa thời gian

        grid.add(new Label("Tên sản phẩm:"), 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(new Label("Mô tả:"), 0, 1);
        grid.add(descField, 1, 1);
        grid.add(new Label("Loại:"), 0, 2);
        grid.add(typeBox, 1, 2);
        grid.add(new Label("Giá khởi điểm ($):"), 0, 3);
        grid.add(priceField, 1, 3);
        grid.add(new Label("Thời gian còn lại (phút):"), 0, 4);
        grid.add(timeField, 1, 4);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == updateButtonType) {
                String name = nameField.getText().trim();
                String desc = descField.getText().trim();
                String price = priceField.getText().trim();
                String duration = timeField.getText().trim();

                if (ValidationHelper.isEmpty(name)) {
                    AlertHelper.showWarning("Lỗi", "Tên sản phẩm không được để trống!");
                    return null;
                }
                if (!ValidationHelper.isValidStartPrice(price)) {
                    AlertHelper.showWarning("Lỗi", "Giá khởi điểm phải là số dương!");
                    return null;
                }
                if (!ValidationHelper.isValidDuration(duration)) {
                    AlertHelper.showWarning("Lỗi", "Thời lượng phải là số nguyên dương!");
                    return null;
                }

                return Protocol.REQ_UPDATE_ITEM + Protocol.DELIMITER + 
                       selected.getId() + Protocol.DELIMITER + 
                       name + Protocol.DELIMITER + 
                       desc.replace("\n", " ") + Protocol.DELIMITER + 
                       typeBox.getValue() + Protocol.DELIMITER + 
                       price + Protocol.DELIMITER + 
                       duration;
            }
            return null;
        });

        dialog.showAndWait().ifPresent(command -> {
            ClientNetworkManager.getInstance().sendData(command);
        });
    }
}
