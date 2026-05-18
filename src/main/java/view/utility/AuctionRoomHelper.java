package view.utility;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.util.Duration;
import network.ClientNetworkManager;
import shared.Protocol;

import java.time.LocalDateTime;

/**
 * Helper quản lý chức năng chung giữa InRoomController và SellerInRoomController:
 * - Timer đếm ngược
 * - Exit room (cleanup listeners + close window)
 * - Network listener chung (TIME_EXTENDED, PARTICIPANTS, ROOM_KICKED)
 * 
 * Trích xuất để tuân thủ DRY.
 */
public class AuctionRoomHelper {
    private Timeline totalTimelineTimer;
    private LocalDateTime auctionEndTime;
    private int totalTimeRemaining = 0;
    private final String auctionId;

    // Callback để controller cập nhật UI
    private Runnable onTimeUpdate;
    private Runnable onTimerExpired;

    public AuctionRoomHelper(String auctionId) {
        this.auctionId = auctionId;
    }

    public void setOnTimeUpdate(Runnable callback) { this.onTimeUpdate = callback; }
    public void setOnTimerExpired(Runnable callback) { this.onTimerExpired = callback; }

    public int getTotalTimeRemaining() { return totalTimeRemaining; }
    public LocalDateTime getAuctionEndTime() { return auctionEndTime; }

    // === TIMER ===

    public void initTimer(LocalDateTime endTime) {
        this.auctionEndTime = endTime;
        long seconds = java.time.Duration.between(LocalDateTime.now(), endTime).getSeconds();
        this.totalTimeRemaining = (int) Math.max(0, seconds);
    }

    public void startTimer() {
        if (totalTimelineTimer != null) totalTimelineTimer.stop();
        totalTimelineTimer = new Timeline(new KeyFrame(Duration.seconds(1), event -> {
            if (auctionEndTime != null) {
                long seconds = java.time.Duration.between(LocalDateTime.now(), auctionEndTime).getSeconds();
                totalTimeRemaining = (int) Math.max(0, seconds);
                if (onTimeUpdate != null) Platform.runLater(onTimeUpdate);
                if (totalTimeRemaining <= 0) {
                    stopTimer();
                    if (onTimerExpired != null) Platform.runLater(onTimerExpired);
                }
            }
        }));
        totalTimelineTimer.setCycleCount(Timeline.INDEFINITE);
        totalTimelineTimer.play();
    }

    public void stopTimer() {
        if (totalTimelineTimer != null) { totalTimelineTimer.stop(); totalTimelineTimer = null; }
    }

    public void extendTime(int addedSeconds) {
        if (auctionEndTime != null) auctionEndTime = auctionEndTime.plusSeconds(addedSeconds);
        totalTimeRemaining += addedSeconds;
    }

    // === EXIT ROOM ===

    /**
     * Cleanup listeners, gửi LEAVE_ROOM, đóng window.
     * @param protocolKeys Danh sách Protocol key cần clear listener
     */
    public void exitRoom(javafx.stage.Stage stage, String... protocolKeys) {
        stopTimer();
        for (String key : protocolKeys) {
            ClientNetworkManager.getInstance().clearListeners(key);
        }
        if (auctionId != null) {
            ClientNetworkManager.getInstance().sendData(Protocol.REQ_LEAVE_ROOM + Protocol.DELIMITER + auctionId);
        }
        if (stage != null) stage.close();
    }

    // === COMMON NETWORK LISTENERS ===

    /**
     * Đăng ký listener BROADCAST_TIME_EXTENDED chung.
     */
    public void registerTimeExtendedListener(Runnable onExtend) {
        ClientNetworkManager.getInstance().registerListener(Protocol.BROADCAST_TIME_EXTENDED, (message) -> {
            String[] parts = message.split(Protocol.DELIMITER);
            if (parts.length >= 3 && java.util.Objects.equals(parts[1], auctionId)) {
                int addedSeconds = Integer.parseInt(parts[2]);
                extendTime(addedSeconds);
                if (onExtend != null) Platform.runLater(onExtend);
            }
        });
    }

    /**
     * Đăng ký listener BROADCAST_PARTICIPANTS chung.
     */
    public void registerParticipantsListener(java.util.function.Consumer<String> onCount) {
        ClientNetworkManager.getInstance().registerListener(Protocol.BROADCAST_PARTICIPANTS, (message) -> {
            String[] parts = message.split(Protocol.DELIMITER);
            if (parts.length >= 3 && java.util.Objects.equals(parts[1], auctionId)) {
                if (onCount != null) Platform.runLater(() -> onCount.accept(parts[2]));
            }
        });
    }

    /**
     * Đăng ký listener BROADCAST_ROOM_KICKED chung.
     */
    public void registerRoomKickedListener(Runnable onKicked) {
        ClientNetworkManager.getInstance().registerListener(Protocol.BROADCAST_ROOM_KICKED, (message) -> {
            String[] parts = message.split(Protocol.DELIMITER);
            if (parts.length >= 3 && java.util.Objects.equals(parts[1], auctionId)) {
                Platform.runLater(() -> {
                    AlertHelper.showWarning("Rời phòng", parts[2]);
                    if (onKicked != null) onKicked.run();
                });
            }
        });
    }
}
