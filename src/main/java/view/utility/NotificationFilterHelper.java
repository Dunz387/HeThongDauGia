package view.utility;

import javafx.scene.control.TableView;
import model.auction.Auction;
import network.ClientNetworkManager;
import network.NotificationManager;

import java.util.List;
import java.util.function.Consumer;

/**
 * Xử lý lọc và phân phối thông báo đấu giá theo vai trò người dùng (SRP).
 * Tách từ BaseMenuController để tuân thủ Single Responsibility Principle.
 */
public class NotificationFilterHelper {

    private NotificationFilterHelper() {} // Utility class

    /**
     * Đăng ký lắng nghe broadcast phiên đấu giá bắt đầu & kết thúc,
     * tự động lọc thông báo theo vai trò người dùng.
     *
     * @param tableAuctions Bảng dữ liệu phiên đấu giá hiện tại (dùng để tra cứu thông tin)
     */
    public static void registerNotificationListeners(TableView<Auction> tableAuctions) {
        // === PHIÊN ĐẤU GIÁ BẮT ĐẦU ===
        ClientNetworkManager.getInstance().clearListeners(shared.Protocol.BROADCAST_AUCTION_START);
        ClientNetworkManager.getInstance().registerListener(shared.Protocol.BROADCAST_AUCTION_START,
                (message) -> {
                    String[] parts = message.split(shared.Protocol.DELIMITER);
                    if (parts.length >= 2) {
                        String auctionId = parts[1];
                        javafx.application.Platform.runLater(() ->
                                handleNotification(auctionId, tableAuctions,
                                        a -> pushStartNotification(a)));
                    }
                });

        // === PHIÊN ĐẤU GIÁ KẾT THÚC ===
        ClientNetworkManager.getInstance().clearListeners(shared.Protocol.BROADCAST_AUCTION_FINISHED);
        ClientNetworkManager.getInstance().registerListener(shared.Protocol.BROADCAST_AUCTION_FINISHED,
                (message) -> {
                    String[] parts = message.split(shared.Protocol.DELIMITER);
                    if (parts.length >= 2) {
                        String auctionId = parts[1];
                        String winner = parts.length > 2 ? parts[2] : "Không có";
                        javafx.application.Platform.runLater(() ->
                                handleNotification(auctionId, tableAuctions,
                                        a -> pushFinishedNotification(a, winner)));
                    }
                });
    }

    /**
     * Xử lý thông báo chung: tìm auction trong bảng, nếu chưa có thì đợi danh sách cập nhật.
     */
    private static void handleNotification(String auctionId, TableView<Auction> table,
                                           Consumer<Auction> notificationAction) {
        Auction auction = findInTable(auctionId, table);
        if (auction != null) {
            notificationAction.accept(auction);
        } else {
            // Đăng ký listener tạm thời chờ danh sách cập nhật
            Consumer<List<Auction>> oneTimeListener = new Consumer<>() {
                @Override
                public void accept(List<Auction> list) {
                    javafx.application.Platform.runLater(() -> {
                        Auction a = findInTable(auctionId, table);
                        if (a != null) {
                            notificationAction.accept(a);
                            ClientNetworkManager.getInstance().removeAuctionListListener(this);
                        }
                    });
                }
            };
            ClientNetworkManager.getInstance().addAuctionListListener(oneTimeListener);
        }
    }

    /** Đẩy thông báo phiên bắt đầu (đã lọc theo vai trò) */
    private static void pushStartNotification(Auction auction) {
        if (RoleBasedFilterHelper.shouldReceiveNotification(auction)) {
            NotificationManager.getInstance()
                    .addNotification("🚀 Một phiên đấu giá mới (" + auction.getItem().getName() + ") đã bắt đầu!");
        }
    }

    /** Đẩy thông báo phiên kết thúc (đã lọc theo vai trò) */
    private static void pushFinishedNotification(Auction auction, String winner) {
        if (RoleBasedFilterHelper.shouldReceiveNotification(auction)) {
            NotificationManager.getInstance()
                    .addNotification("🏆 [" + auction.getItem().getName() + "] kết thúc. Người thắng: " + winner);
        }
    }

    /** Tìm auction trong bảng theo ID */
    private static Auction findInTable(String auctionId, TableView<Auction> table) {
        for (Auction a : table.getItems()) {
            if (a.getId().equals(auctionId)) {
                return a;
            }
        }
        return null;
    }
}
