package view.AuctionRoomUI;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.stage.Stage;
import view.utility.SceneManager;
import view.utility.WindowManager;

public class BidOrSellChoiceController {
    
    @FXML
    private void BidChoiceClicked(ActionEvent event) {
        Stage popupStage = (Stage) ((javafx.scene.Node) event.getSource()).getScene().getWindow();
        Stage mainStage = (Stage) popupStage.getOwner();
        
        popupStage.close(); // Đóng popup
        
        // Đấu giá: Chuyển Scene trên cửa sổ chính sang danh sách phòng đấu giá
        if (mainStage != null) {
            SceneManager.goToRoomMenu(mainStage);
        }
    }

    @FXML
    private void SellChoiceClicked(ActionEvent event) {
        Stage popupStage = (Stage) ((javafx.scene.Node) event.getSource()).getScene().getWindow();
        Stage mainStage = (Stage) popupStage.getOwner();
        
        popupStage.close();
        
        // Bán: Mở popup tạo sản phẩm
        if (mainStage != null) {
            WindowManager.openCreateItemWindow(mainStage); 
        } else {
            WindowManager.openCreateItemWindow();
        }
    }

    @FXML
    private void backToMenu(ActionEvent event) {
        Stage popupStage = (Stage) ((javafx.scene.Node) event.getSource()).getScene().getWindow();
        popupStage.close();
    }
}