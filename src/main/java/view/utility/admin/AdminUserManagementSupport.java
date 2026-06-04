package view.utility.admin;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.HBox;
import model.user.User;
import network.ClientNetworkManager;
import shared.Protocol;
import view.utility.display.AlertHelper;

public class AdminUserManagementSupport {
    private final TableView<User> tableUsers;
    private final TableColumn<User, Void> actionColumn;

    public AdminUserManagementSupport(TableView<User> tableUsers, TableColumn<User, Void> actionColumn) {
        this.tableUsers = tableUsers;
        this.actionColumn = actionColumn;
    }

    public void configureActionColumn() {
        actionColumn.setCellFactory(col -> new TableCell<>() {
            private final Button toggleButton = new Button();
            private final Button editBalanceButton = new Button("💰 Sửa tiền");
            private final HBox container = new HBox(8, toggleButton, editBalanceButton);

            {
                toggleButton.setOnAction(event -> toggleUserStatus(getTableView().getItems().get(getIndex())));
                editBalanceButton.setOnAction(event -> showEditBalanceDialog(getTableView().getItems().get(getIndex())));
                editBalanceButton.getStyleClass().setAll("btn-primary");
                container.setAlignment(Pos.CENTER);
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getIndex() >= getTableView().getItems().size()) {
                    setGraphic(null);
                    return;
                }

                User user = getTableView().getItems().get(getIndex());
                if ("ADMIN".equals(user.getRole().name())) {
                    setGraphic(null);
                    return;
                }

                updateToggleButton(user);
                setGraphic(container);
            }

            private void updateToggleButton(User user) {
                if (user.isActive()) {
                    toggleButton.setText("🔒 Khóa");
                    toggleButton.getStyleClass().setAll("btn-danger");
                } else {
                    toggleButton.setText("🔓 Mở khóa");
                    toggleButton.getStyleClass().setAll("btn-success");
                }
            }
        });
    }

    public void registerListeners() {
        registerMutationListener(
                Protocol.REQ_BAN_USER,
                "Hệ thống",
                "Đã cập nhật trạng thái người dùng thành công!",
                "Không thể thực hiện thao tác"
        );
        registerMutationListener(
                Protocol.REQ_UPDATE_USER_BALANCE,
                "Hệ thống",
                "Đã cập nhật số dư người dùng thành công!",
                "Không thể cập nhật số dư"
        );

        ClientNetworkManager.getInstance().clearUserListListeners();
        ClientNetworkManager.getInstance().addUserListListener(listFromServer -> {
            if (listFromServer != null) {
                Platform.runLater(() -> tableUsers.setItems(FXCollections.observableArrayList(listFromServer)));
            }
        });
    }

    public void load() {
        ClientNetworkManager.getInstance().sendData(Protocol.REQ_GET_USERS);
    }

    private void toggleUserStatus(User user) {
        String request = Protocol.REQ_BAN_USER
                + Protocol.DELIMITER + user.getId()
                + Protocol.DELIMITER + !user.isActive();
        ClientNetworkManager.getInstance().sendData(request);
    }

    private void showEditBalanceDialog(User user) {
        TextInputDialog dialog = new TextInputDialog(String.format("%.0f", AdminTableColumns.balanceOf(user)));
        dialog.setTitle("Điều chỉnh số dư");
        dialog.setHeaderText("Người dùng: " + user.getUsername());
        dialog.setContentText("Nhập số dư mới ($):");

        dialog.showAndWait().ifPresent(input -> updateBalance(user, input));
    }

    private void updateBalance(User user, String input) {
        try {
            double newBalance = Double.parseDouble(input);
            ClientNetworkManager.getInstance().sendData(
                    Protocol.REQ_UPDATE_USER_BALANCE
                            + Protocol.DELIMITER + user.getId()
                            + Protocol.DELIMITER + newBalance);
        } catch (NumberFormatException e) {
            AlertHelper.showError("Lỗi", "Vui lòng nhập số hợp lệ!");
        }
    }

    private void registerMutationListener(
            String command,
            String successTitle,
            String successMessage,
            String fallbackError
    ) {
        ClientNetworkManager.getInstance().registerListener(command, response -> {
            String[] parts = response.split(Protocol.DELIMITER);
            Platform.runLater(() -> {
                if (parts.length > 1 && Protocol.RES_SUCCESS.equals(parts[1])) {
                    load();
                    AlertHelper.showInfo(successTitle, successMessage);
                } else {
                    AlertHelper.showError("Lỗi", parts.length >= 3 ? parts[2] : fallbackError);
                }
            });
        });
    }
}
