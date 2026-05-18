package view.utility;

import javafx.scene.control.TextInputDialog;
import javafx.stage.Stage;
import network.ClientNetworkManager;
import network.SessionManager;
import shared.Protocol;
import view.utility.AlertHelper;

import java.util.Optional;

/**
 * Helper chứa các logic UI dùng chung cho các menu controller.
 * Trích xuất để tránh duplicate giữa BaseMenuController và AssetsListController (DRY).
 */
public class MenuHelper {

    /**
     * Xử lý nạp/rút tiền tùy theo role của user hiện tại.
     */
    public static void handleTransaction() {
        if (SessionManager.getInstance().isBidder()) {
            showAmountDialog("Nạp tiền", "Nhập số tiền muốn nạp:")
                .ifPresent(amount -> ClientNetworkManager.getInstance().sendData(Protocol.REQ_DEPOSIT + Protocol.DELIMITER + amount));
        } else if (SessionManager.getInstance().isSeller()) {
            showAmountDialog("Rút tiền", "Nhập số tiền muốn rút:")
                .ifPresent(amount -> ClientNetworkManager.getInstance().sendData(Protocol.REQ_WITHDRAW + Protocol.DELIMITER + amount));
        }
    }

    private static Optional<String> showAmountDialog(String title, String header) {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle(title);
        dialog.setHeaderText(header);
        dialog.setContentText("Số tiền ($):");
        return dialog.showAndWait()
                .map(String::trim)
                .filter(s -> !s.isEmpty());
    }

    /**
     * Cấu hình hiển thị nút giao dịch và label số dư theo role.
     */
    public static void setupTransactionButton(javafx.scene.control.Button btn) {
        if (btn == null) return;
        if (SessionManager.getInstance().isBidder()) {
            btn.setText("Nạp tiền");
        } else if (SessionManager.getInstance().isSeller()) {
            btn.setText("Rút tiền");
        } else {
            btn.setVisible(false);
            btn.setManaged(false);
        }
    }

    /**
     * Khởi tạo hiển thị số dư và đăng ký listener realtime.
     */
    public static void setupBalanceLabel(javafx.scene.control.Label balanceLabel) {
        if (balanceLabel == null) return;
        balanceLabel.setText(String.format("💰 Số dư: $%,.0f", SessionManager.getInstance().getBalance()));
        SessionManager.getInstance().balanceProperty().addListener((obs, oldVal, newVal) ->
            javafx.application.Platform.runLater(() ->
                balanceLabel.setText(String.format("💰 Số dư: $%,.0f", newVal.doubleValue()))));
    }
}
