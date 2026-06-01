package view.utility.table;

import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.text.Text;
import javafx.util.Callback;
import network.NotificationManager;

/**
 * Factory tạo cell tự động xuống dòng cho bảng thông báo.
 * Trích xuất từ 3 controller (InRoom, SellerInRoom, AssetsList) để tránh duplicate (DRY).
 */
public class WrappingTextCellFactory implements Callback<TableColumn<NotificationManager.NotificationItem, String>, TableCell<NotificationManager.NotificationItem, String>> {
    @Override
    public TableCell<NotificationManager.NotificationItem, String> call(TableColumn<NotificationManager.NotificationItem, String> tc) {
        return new TableCell<>() {
            private final Text textNode = new Text();
            {
                textNode.wrappingWidthProperty().bind(tc.widthProperty().subtract(10));
                textNode.setStyle("-fx-fill: #333333; -fx-font-family: 'Segoe UI'; -fx-font-size: 13px;");
                setGraphic(textNode);
            }
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    textNode.setText(null);
                    setGraphic(null);
                } else {
                    textNode.setText(item);
                    setGraphic(textNode);
                }
            }
        };
    }
}
