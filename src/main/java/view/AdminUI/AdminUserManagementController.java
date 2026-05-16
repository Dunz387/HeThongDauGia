package view.AdminUI;

import javafx.application.Platform;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import model.user.Bidder;
import model.user.Seller;
import model.user.User;
import network.ClientNetworkManager;
import shared.Protocol;
import view.utility.AlertHelper;
import view.utility.SceneManager;
import view.utility.StatusDisplayHelper;

import java.net.URL;
import java.util.ResourceBundle;

public class AdminUserManagementController implements Initializable {

    @FXML private HBox menuBar;
    @FXML private TableView<User> tableUsers;
    @FXML private TableColumn<User, Integer> colSTT;
    @FXML private TableColumn<User, String> colId;
    @FXML private TableColumn<User, String> colUsername;
    @FXML private TableColumn<User, String> colRole;
    @FXML private TableColumn<User, Double> colBalance;
    @FXML private TableColumn<User, String> colStatus;
    @FXML private TableColumn<User, Void> colAction;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // === CẤU HÌNH CÁC CỘT BẢNG ===
        colSTT.setCellValueFactory(cellData ->
                new SimpleIntegerProperty(tableUsers.getItems().indexOf(cellData.getValue()) + 1).asObject());
        colId.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getId()));
        colUsername.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getUsername()));
        colRole.setCellValueFactory(cellData ->
                new SimpleStringProperty(StatusDisplayHelper.formatUserRole(cellData.getValue().getRole().name())));
        colBalance.setCellValueFactory(cellData -> {
            User u = cellData.getValue();
            double balance = 0.0;
            if (u instanceof Bidder) balance = ((Bidder) u).getBalance();
            else if (u instanceof Seller) balance = ((Seller) u).getBalance();
            return new SimpleDoubleProperty(balance).asObject();
        });
        colStatus.setCellValueFactory(cellData ->
                new SimpleStringProperty(StatusDisplayHelper.formatUserStatus(cellData.getValue().isActive())));

        // === CỘT HÀNH ĐỘNG: NÚT KHÓA / MỞ KHÓA ===
        colAction.setCellFactory(col -> new TableCell<>() {
            private final Button btnToggle = new Button();

            {
                btnToggle.setOnAction(event -> {
                    User user = getTableView().getItems().get(getIndex());
                    boolean currentlyActive = user.isActive();
                    // Gửi lệnh: BAN_USER;;;userId;;;newStatus (true=mở khóa, false=khóa)
                    String request = Protocol.REQ_BAN_USER + Protocol.DELIMITER
                            + user.getId() + Protocol.DELIMITER
                            + (!currentlyActive); // Đảo ngược trạng thái

                    ClientNetworkManager.getInstance().sendData(request);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getIndex() >= getTableView().getItems().size()) {
                    setGraphic(null);
                } else {
                    User user = getTableView().getItems().get(getIndex());
                    // Không cho phép khóa chính Admin
                    if ("ADMIN".equals(user.getRole().name())) {
                        setGraphic(null);
                    } else {
                        if (user.isActive()) {
                            btnToggle.setText("🔒 Khóa");
                            btnToggle.getStyleClass().setAll("btn-danger");
                        } else {
                            btnToggle.setText("🔓 Mở khóa");
                            btnToggle.getStyleClass().setAll("btn-success");
                        }
                        setGraphic(btnToggle);
                    }
                }
            }
        });

        // === LẮNG NGHE KẾT QUẢ KHÓA/MỞ KHÓA (GLOBAL) ===
        ClientNetworkManager.getInstance().registerListener(Protocol.REQ_BAN_USER, (response) -> {
            String[] parts = response.split(Protocol.SEPARATOR);
            Platform.runLater(() -> {
                if (parts.length > 1 && parts[1].equals(Protocol.RES_SUCCESS)) {
                    // Cập nhật lại danh sách ngay lập tức
                    ClientNetworkManager.getInstance().sendData(Protocol.REQ_GET_USERS);
                    AlertHelper.showInfo("Hệ thống", "Đã cập nhật trạng thái người dùng thành công!");
                } else {
                    AlertHelper.showError("Lỗi", parts.length >= 3 ? parts[2] : "Không thể thực hiện thao tác");
                }
            });
        });

        // === LẮNG NGHE DANH SÁCH USER TỪ SERVER (REAL-TIME) ===
        ClientNetworkManager.getInstance().setUserListListener((listFromServer) -> {
            if (listFromServer != null) {
                Platform.runLater(() -> {
                    ObservableList<User> data = FXCollections.observableArrayList(listFromServer);
                    tableUsers.setItems(data);
                    System.out.println("✅ [Admin] Đã cập nhật danh sách " + listFromServer.size() + " người dùng.");
                });
            }
        });

        // === GỬI YÊU CẦU LẤY DANH SÁCH KHI VÀO TRANG ===
        ClientNetworkManager.getInstance().sendData(Protocol.REQ_GET_USERS);
    }

    // === ĐIỀU HƯỚNG ===

    @FXML
    private void goToDashboard(ActionEvent event) {
        Stage stage = (Stage) menuBar.getScene().getWindow();
        SceneManager.goToAdminDashboard(stage);
    }

    @FXML
    private void goToAuctionManagement(ActionEvent event) {
        Stage stage = (Stage) menuBar.getScene().getWindow();
        SceneManager.goToAdminAuctionManagement(stage);
    }

    @FXML
    private void logoutClicked(ActionEvent event) {
        Stage stage = (Stage) menuBar.getScene().getWindow();
        SceneManager.goToLogin(stage);
    }

    @FXML
    private void refreshData(ActionEvent event) {
        ClientNetworkManager.getInstance().sendData(Protocol.REQ_GET_USERS);
    }
}
