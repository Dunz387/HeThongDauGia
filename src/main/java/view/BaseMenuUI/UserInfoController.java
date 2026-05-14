package view.BaseMenuUI;

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
        
        // Đăng ký lắng nghe số dư realtime
        network.ClientNetworkManager.getInstance().addBalanceListener(this::updateBalanceUI);
        
        // Gửi yêu cầu lấy dữ liệu mới nhất từ server
        network.ClientNetworkManager.getInstance().sendData(shared.Protocol.REQ_GET_AUCTIONS);
    }

    private void updateBalanceUI(Double newBalance) {
        javafx.application.Platform.runLater(() -> {
            lblBalance.setText("$" + String.format("%.2f", newBalance));
        });
    }

    private void loadUserData() {
        SessionManager session = SessionManager.getInstance();
        if (session.isLoggedIn()) {
            lblUsername.setText(session.getUsername());
            lblId.setText("ID: " + session.getUserId());
            lblBalance.setText("$" + String.format("%.2f", session.getBalance()));
            lblRole.setText(session.getRole().toString());
            lblAssetsCount.setText("0 sản phẩm");
        }
    }

    private void updateAssetsCount(java.util.List<model.auction.Auction> auctionList) {
        if (auctionList == null) return;
        
        String currentUserId = SessionManager.getInstance().getUserId();
        long count = auctionList.stream()
            .filter(a -> a.getItem().getOwner() != null && a.getItem().getOwner().getId().equals(currentUserId))
            .count();
            
        javafx.application.Platform.runLater(() -> {
            lblAssetsCount.setText(count + " sản phẩm");
        });
    }

    @FXML
    private void closeWindow(ActionEvent event) {
        // Hủy lắng nghe trước khi đóng để tránh memory leak
        network.ClientNetworkManager.getInstance().removeAuctionListListener(this::updateAssetsCount);
        network.ClientNetworkManager.getInstance().removeBalanceListener(this::updateBalanceUI);
        
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.close();
    }
}
