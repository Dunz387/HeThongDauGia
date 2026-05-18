package view.controller.menu;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.stage.Stage;
import network.SessionManager;

import java.net.URL;
import java.util.ResourceBundle;

public class UserInfoController implements Initializable {

    @FXML private Label lblUsername;
    @FXML private Label lblId;
    @FXML private Label lblBalance;
    @FXML private Label lblAssetsCount;
    @FXML private Label lblRole;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        loadUserData();
        
        // Đăng ký lắng nghe danh sách auction để đếm tài sản
        network.ClientNetworkManager.getInstance().addAuctionListListener(this::updateAssetsCount);
        
        // Cập nhật số dư realtime bằng cách lắng nghe SessionManager
        SessionManager.getInstance().balanceProperty().addListener((obs, oldVal, newVal) -> {
            javafx.application.Platform.runLater(() -> {
                lblBalance.setText("$" + String.format("%.2f", newVal.doubleValue()));
            });
        });
        
        // Gửi yêu cầu lấy dữ liệu mới nhất từ server để cập nhật số lượng tài sản
        network.ClientNetworkManager.getInstance().sendData(shared.Protocol.REQ_GET_AUCTIONS);
    }

    private void loadUserData() {
        SessionManager session = SessionManager.getInstance();
        if (session.isLoggedIn()) {
            lblUsername.setText(session.getUsername());
            lblId.setText("ID: " + session.getUserId());
            lblBalance.setText("$" + String.format("%.2f", session.getBalance()));
            lblRole.setText(session.getRole().toString());
            lblAssetsCount.setText("Đang tải...");
        }
    }

    private void updateAssetsCount(java.util.List<model.auction.Auction> auctionList) {
        if (auctionList == null) return;
        
        // SRP: delegate sang RoleBasedFilterHelper để đồng bộ logic với bảng tài sản
        long count = view.utility.RoleBasedFilterHelper.countAssets(auctionList);
            
        javafx.application.Platform.runLater(() -> {
            lblAssetsCount.setText(count + " sản phẩm");
        });
    }

    @FXML
    private void closeWindow(ActionEvent event) {
        // Hủy lắng nghe trước khi đóng để tránh memory leak
        network.ClientNetworkManager.getInstance().removeAuctionListListener(this::updateAssetsCount);
        
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.close();
    }
}
