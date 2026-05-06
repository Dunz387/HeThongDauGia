package view.BaseMenuUI;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.Pane;
import javafx.util.Duration;

public class NotificationMenuHandler {

    private final Pane darkOverlay;
    private final ScrollPane notificationMenu;
    private final double menuWidth;
    private boolean isOpen = false;
    
    private Timeline currentAnimation; 

    public NotificationMenuHandler(Pane darkOverlay, ScrollPane notificationMenu, double menuWidth) {
        this.darkOverlay = darkOverlay;
        this.notificationMenu = notificationMenu;
        this.menuWidth = menuWidth;

        // Set initial state
        this.isOpen = false;
        
        // Push the menu out of bounds (RIGHT side) by setting positive TranslateX
        this.notificationMenu.setTranslateX(this.menuWidth); 
        this.notificationMenu.setVisible(false);
        this.darkOverlay.setOpacity(0.0);
        this.darkOverlay.setVisible(false);

        // Protect event: Only close when clicking DIRECTLY on the dark overlay
        this.darkOverlay.setOnMouseClicked(event -> {
            if (event.getTarget() == this.darkOverlay) {
                closeMenu();
            }
        });
    }

    public void toggleMenu() {
        if (isOpen) {
            closeMenu();
        } else {
            openMenu();
        }
    }

    public void openMenu() {
        if (isOpen) return;
        
        // Lock state immediately to prevent double-click issues
        isOpen = true; 

        notificationMenu.setVisible(true);
        darkOverlay.setVisible(true);

        // Stop current animation if it's running
        if (currentAnimation != null) currentAnimation.stop();

        // Slide in: From positive menuWidth to 0
        currentAnimation = new Timeline(
            new KeyFrame(Duration.seconds(0.3),
                new KeyValue(darkOverlay.opacityProperty(), 0.5),
                new KeyValue(notificationMenu.translateXProperty(), 0)
            )
        );
        currentAnimation.play();
    }

    public void closeMenu() {
        if (!isOpen) return;
        
        // Lock state immediately
        isOpen = false; 

        if (currentAnimation != null) currentAnimation.stop();

        // Slide out: From 0 to positive menuWidth (pushing it off-screen to the right)
        currentAnimation = new Timeline(
            new KeyFrame(Duration.seconds(0.3),
                new KeyValue(darkOverlay.opacityProperty(), 0.0),
                new KeyValue(notificationMenu.translateXProperty(), menuWidth) 
            )
        );
        currentAnimation.setOnFinished(e -> {
            // Hide components completely after animation finishes
            if (!isOpen) { 
                darkOverlay.setVisible(false);
                notificationMenu.setVisible(false);
            }
        });
        currentAnimation.play();
    }

    public boolean isOpen() {
        return isOpen;
    }
}