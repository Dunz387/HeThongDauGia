package view.utility.auction;

import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import network.session.SessionManager;
import view.utility.display.AlertHelper;

import java.util.function.Consumer;

/**
 * Builds the room participant-management popup used by seller/admin screens.
 */
public final class ParticipantManagementDialog {
    private ParticipantManagementDialog() {
    }

    public static void show(Stage owner, ObservableList<String> participants, Consumer<String> onKickUser) {
        Stage popupStage = new Stage();
        popupStage.setTitle("Quan ly nguoi dung trong phong");
        if (owner != null) {
            popupStage.initOwner(owner);
        }
        popupStage.initModality(Modality.WINDOW_MODAL);

        VBox layout = new VBox(15);
        layout.setPadding(new Insets(20));
        layout.setStyle("-fx-background-color: #faf9f0;");

        Label titleLabel = new Label("Nguoi dung trong phong");
        titleLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #1a1a1a; -fx-font-family: 'Segoe UI';");

        ListView<String> listView = new ListView<>(participants);
        listView.setPrefHeight(300);
        listView.setPrefWidth(350);
        listView.setStyle("-fx-background-radius: 8; -fx-border-radius: 8; -fx-border-color: #e8e8e0;");
        listView.setCellFactory(param -> new ParticipantCell(popupStage, onKickUser));

        Label placeholder = new Label("Chua co nguoi dung nao khac");
        placeholder.setStyle("-fx-text-fill: #999; -fx-font-family: 'Segoe UI';");
        listView.setPlaceholder(placeholder);

        Button closeBtn = new Button("Dong");
        closeBtn.getStyleClass().setAll("btn-primary");
        closeBtn.setStyle("-fx-padding: 6 18; -fx-font-weight: bold; -fx-cursor: hand;");
        closeBtn.setOnAction(e -> popupStage.close());

        HBox btnContainer = new HBox(closeBtn);
        btnContainer.setAlignment(Pos.CENTER_RIGHT);
        layout.getChildren().addAll(titleLabel, listView, btnContainer);

        Scene scene = new Scene(layout);
        java.net.URL cssResource = ParticipantManagementDialog.class.getResource("/view/styles.css");
        if (cssResource != null) {
            scene.getStylesheets().add(cssResource.toExternalForm());
        }

        popupStage.setScene(scene);
        popupStage.show();
    }

    private static class ParticipantCell extends ListCell<String> {
        private final Stage popupStage;
        private final Consumer<String> onKickUser;
        private final HBox hbox = new HBox(10);
        private final Label nameLabel = new Label();
        private final Button kickBtn = new Button("Duoi");
        private final Region spacer = new Region();

        ParticipantCell(Stage popupStage, Consumer<String> onKickUser) {
            this.popupStage = popupStage;
            this.onKickUser = onKickUser;
            hbox.setAlignment(Pos.CENTER_LEFT);
            HBox.setHgrow(spacer, Priority.ALWAYS);
            kickBtn.getStyleClass().setAll("btn-danger");
            kickBtn.setStyle("-fx-font-size: 11px; -fx-padding: 3 10; -fx-font-weight: bold; -fx-cursor: hand;");
            nameLabel.setStyle("-fx-font-family: 'Segoe UI'; -fx-font-weight: 500; -fx-text-fill: #333333;");
            hbox.getChildren().addAll(nameLabel, spacer, kickBtn);
        }

        @Override
        protected void updateItem(String item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setText(null);
                setGraphic(null);
                return;
            }

            nameLabel.setText(item);
            boolean isCurrentUser = item.equals(SessionManager.getInstance().getUsername());
            kickBtn.setVisible(!isCurrentUser);
            kickBtn.setOnAction(e -> confirmKick(item));
            setGraphic(hbox);
        }

        private void confirmKick(String username) {
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setTitle("Xac nhan duoi");
            confirm.setHeaderText(null);
            confirm.setContentText("Ban co chac chan muon duoi '" + username + "' khoi phong?");
            confirm.initOwner(popupStage);
            confirm.showAndWait().ifPresent(response -> {
                if (response == ButtonType.OK) {
                    onKickUser.accept(username);
                    AlertHelper.showInfo("Thanh cong", "Da gui yeu cau duoi " + username);
                }
            });
        }
    }
}
