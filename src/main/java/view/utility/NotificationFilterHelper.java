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

    private static Consumer<String> startListener;
    private static Consumer<String> finishedListener;
    private static Consumer<String> newBidListener;

    /**
     * Đăng ký lắng nghe broadcast phiên đấu giá bắt đầu & kết thúc,
     * tự động lọc thông báo theo vai trò người dùng.
     *
     * @param tableAuctions Bảng dữ liệu phiên đấu giá hiện tại (dùng để tra cứu thông tin)
     */
    public static void registerNotificationListeners(TableView<Auction> tableAuctions) {
        // === PHIÊN ĐẤU GIÁ BẮT ĐẦU ===
        if (startListener != null) {
            ClientNetworkManager.getInstance().removeListener(shared.Protocol.BROADCAST_AUCTION_START, startListener);
        }
        startListener = (message) -> {
            String[] parts = message.split(shared.Protocol.DELIMITER);
            if (parts.length >= 2) {
                String auctionId = parts[1];
                javafx.application.Platform.runLater(() ->
                        handleNotification(auctionId, tableAuctions,
                                a -> pushStartNotification(a)));
            }
        };
        ClientNetworkManager.getInstance().registerListener(shared.Protocol.BROADCAST_AUCTION_START, startListener);

        // === PHIÊN ĐẤU GIÁ KẾT THÚC ===
        if (finishedListener != null) {
            ClientNetworkManager.getInstance().removeListener(shared.Protocol.BROADCAST_AUCTION_FINISHED, finishedListener);
        }
        finishedListener = (message) -> {
            String[] parts = message.split(shared.Protocol.DELIMITER);
            if (parts.length >= 2) {
                String auctionId = parts[1];
                String winner = parts.length > 2 ? parts[2] : "Không có";
                javafx.application.Platform.runLater(() ->
                        handleNotification(auctionId, tableAuctions,
                                a -> pushFinishedNotification(a, winner)));
            }
        };
        ClientNetworkManager.getInstance().registerListener(shared.Protocol.BROADCAST_AUCTION_FINISHED, finishedListener);

        // === CÓ LƯỢT ĐẶT GIÁ MỚI ===
        if (newBidListener != null) {
            ClientNetworkManager.getInstance().removeListener(shared.Protocol.BROADCAST_NEW_BID, newBidListener);
        }
        newBidListener = (message) -> {
            String[] parts = message.split(shared.Protocol.DELIMITER);
            if (parts.length >= 4) {
                String auctionId = parts[1];
                double newPrice = Double.parseDouble(parts[2]);
                String topBidder = parts[3];
                javafx.application.Platform.runLater(() ->
                        handleNotification(auctionId, tableAuctions,
                                a -> pushNewBidNotification(a, topBidder, newPrice)));
            }
        };
        ClientNetworkManager.getInstance().registerListener(shared.Protocol.BROADCAST_NEW_BID, newBidListener);
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

    /** Đẩy thông báo có lượt đặt giá mới (đã lọc theo vai trò) */
    private static void pushNewBidNotification(Auction auction, String topBidder, double newPrice) {
        boolean shouldNotify = RoleBasedFilterHelper.shouldReceiveNotification(auction);
        // Nếu đây là lượt bid ĐẦU TIÊN của chính Bidder này, data trên client có thể chưa đồng bộ kịp (hasParticipated = false)
        // Ta cần ép buộc hiển thị thông báo để họ biết mình vừa đặt giá thành công
        if (!shouldNotify && network.SessionManager.getInstance().isBidder()) {
            if (topBidder.equals(network.SessionManager.getInstance().getUsername())) {
                shouldNotify = true;
            }
        }
        
        if (shouldNotify) {
            NotificationManager.getInstance()
                    .addNotification("📢 [" + auction.getItem().getName() + "]: " + topBidder + " vừa đặt $" + view.utility.ChartHelper.formatDouble(newPrice));
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
