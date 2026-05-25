package view.utility;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.util.Duration;
import javafx.stage.Stage;
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
 * 
 * [DESIGN PATTERN APPLIED]
 * - Facade Pattern: Đóng gói các logic phức tạp về đếm ngược thời gian và quản lý listener mạng (Network Listeners) vào một lớp duy nhất, cung cấp các API đơn giản cho các Controller sử dụng (như `initTimer`, `startTimer`, `exitRoom`).
 * - Observer Pattern: Cung cấp cơ chế đăng ký callback (`Runnable`, `Consumer`) để các Controller lắng nghe và phản hồi khi có sự kiện (như `setOnTimeUpdate`, `registerParticipantsListener`).
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

        // Cập nhật ngay lần đầu trước khi Timeline bắt đầu
        refreshTimeRemaining();
        if (onTimeUpdate != null) {
            onTimeUpdate.run();
        }

        totalTimelineTimer = new Timeline(new KeyFrame(Duration.seconds(1), event -> {
            refreshTimeRemaining();
            if (onTimeUpdate != null) onTimeUpdate.run();
            if (totalTimeRemaining <= 0) {
                stopTimer();
                if (onTimerExpired != null) onTimerExpired.run();
            }
        }));
        totalTimelineTimer.setCycleCount(Timeline.INDEFINITE);
        totalTimelineTimer.play();
    }

    private void refreshTimeRemaining() {
        if (auctionEndTime != null) {
            long seconds = java.time.Duration.between(LocalDateTime.now(), auctionEndTime).getSeconds();
            totalTimeRemaining = (int) Math.max(0, seconds);
        }
    }

    public void stopTimer() {
        if (totalTimelineTimer != null) { totalTimelineTimer.stop(); totalTimelineTimer = null; }
    }

    public void extendTime(int addedSeconds) {
        if (auctionEndTime != null) auctionEndTime = auctionEndTime.plusSeconds(addedSeconds);
        totalTimeRemaining += addedSeconds;
    }

    private java.util.Map<String, java.util.List<java.util.function.Consumer<String>>> roomListeners = new java.util.HashMap<>();

    /**
     * Đăng ký listener và lưu trữ để có thể dọn dẹp an toàn khi thoát phòng (tránh xóa nhầm listener global).
     */
    public void registerRoomListener(String protocolKey, java.util.function.Consumer<String> listener) {
        ClientNetworkManager.getInstance().registerListener(protocolKey, listener);
        roomListeners.computeIfAbsent(protocolKey, k -> new java.util.ArrayList<>()).add(listener);
    }

    // === EXIT ROOM ===

    private boolean cleaned = false;

    /**
     * Chỉ dọn dẹp (stop timer, xóa listener, gửi LEAVE_ROOM).
     * Không đóng/chuyển window. Dùng khi window đang tự đóng (onCloseRequest).
     */
    public void cleanup() {
        if (cleaned) return;
        cleaned = true;
        stopTimer();
        
        // Chỉ xóa các listener của riêng phòng này
        for (java.util.Map.Entry<String, java.util.List<java.util.function.Consumer<String>>> entry : roomListeners.entrySet()) {
            for (java.util.function.Consumer<String> listener : entry.getValue()) {
                ClientNetworkManager.getInstance().removeListener(entry.getKey(), listener);
            }
        }
        roomListeners.clear();

        if (auctionId != null) {
            ClientNetworkManager.getInstance().sendData(Protocol.REQ_LEAVE_ROOM + Protocol.DELIMITER + auctionId);
        }
    }

    /**
     * Cleanup + đóng/chuyển window. Dùng khi bấm nút "Thoát".
     */
    public void exitRoom(Stage stage) {
        cleanup();
        if (stage != null) {
            if (stage.getOwner() != null) {
                // Popup window → đóng bình thường
                stage.close();
            } else {
                // Main stage → quay về BaseMenu thay vì đóng
                SceneManager.goToBaseMenu(stage);
            }
        }
    }

    // === COMMON NETWORK LISTENERS ===

    /**
     * Đăng ký listener BROADCAST_TIME_EXTENDED chung.
     */
    public void registerTimeExtendedListener(Runnable onExtend) {
        registerRoomListener(Protocol.BROADCAST_TIME_EXTENDED, (message) -> {
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
        registerRoomListener(Protocol.BROADCAST_PARTICIPANTS, (message) -> {
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
        registerRoomListener(Protocol.BROADCAST_ROOM_KICKED, (message) -> {
            String[] parts = message.split(Protocol.DELIMITER);
            if (parts.length >= 3 && java.util.Objects.equals(parts[1], auctionId)) {
                Platform.runLater(() -> {
                    view.utility.AlertHelper.showWarning("Rời phòng", parts[2]);
                    if (onKicked != null) onKicked.run();
                });
            }
        });
    }
}
