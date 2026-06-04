package view.utility.menu;

import javafx.application.Platform;
import javafx.scene.control.Alert;
import network.client.ClientNetworkManager;
import shared.Protocol;
import view.utility.display.AlertHelper;

public final class MenuNetworkSupport {
    private MenuNetworkSupport() {
    }

    public static void registerTransactionResponses(boolean showFailures) {
        registerMoneyResponse(Protocol.REQ_DEPOSIT, "Nạp tiền thành công!", "Nạp tiền thất bại: ", showFailures);
        registerMoneyResponse(Protocol.REQ_WITHDRAW, "Rút tiền thành công!", "Rút tiền thất bại: ", showFailures);
    }

    public static void registerAssetMutationResponses() {
        registerMutationResponse(Protocol.REQ_UPDATE_ITEM, "Thông báo sửa phiên đấu giá", "Sửa thành công!", "Sửa thất bại!");
        registerMutationResponse(Protocol.REQ_DELETE_ITEM, "Thông báo xóa phiên đấu giá", "Xóa thành công!", "Xóa thất bại!");
    }

    private static void registerMoneyResponse(String request, String successMessage, String failurePrefix, boolean showFailures) {
        ClientNetworkManager.getInstance().clearListeners(request);
        ClientNetworkManager.getInstance().registerListener(request, message -> {
            String[] parts = message.split(Protocol.DELIMITER);
            if (parts.length >= 2 && parts[1].equals(Protocol.RES_SUCCESS)) {
                Platform.runLater(() -> AlertHelper.showInfo("Thành công", successMessage));
                return;
            }

            if (showFailures) {
                String reason = parts.length > 2 ? parts[2] : "Lỗi hệ thống";
                Platform.runLater(() -> AlertHelper.showError("Thất bại", failurePrefix + reason));
            }
        });
    }

    private static void registerMutationResponse(String request, String header, String defaultSuccess, String defaultFailure) {
        ClientNetworkManager.getInstance().clearListeners(request);
        ClientNetworkManager.getInstance().registerListener(request, message -> {
            String[] parts = message.split(Protocol.DELIMITER);
            if (parts.length <= 1) {
                return;
            }

            Platform.runLater(() -> {
                boolean success = parts[1].equals(Protocol.RES_SUCCESS);
                Alert alert = new Alert(success ? Alert.AlertType.INFORMATION : Alert.AlertType.ERROR);
                alert.setHeaderText(header);
                alert.setContentText(parts.length > 2 ? parts[2] : (success ? defaultSuccess : defaultFailure));
                alert.show();
            });
        });
    }
}
