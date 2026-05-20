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
 * 
 * [DESIGN PATTERN APPLIED]
 * - Observer Pattern: Đăng ký lắng nghe các sự kiện broadcast từ Server (ClientNetworkManager).
 * - Command / Strategy Pattern: Sử dụng `Consumer<Auction> notificationAction` để đóng gói các hành động tạo thông báo khác nhau (Start, Finished, New Bid) truyền vào `handleNotification`.
 * - Facade Pattern: Cung cấp giao diện đơn giản (`registerNotificationListeners`) ẩn đi logic xử lý đồng bộ danh sách và mạng phức tạp bên dưới.
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
        registerGlobalListListener();

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
                double finalPrice = parts.length > 3 ? Double.parseDouble(parts[3]) : 0;
                javafx.application.Platform.runLater(() ->
                        handleNotification(auctionId, tableAuctions,
                                a -> pushFinishedNotification(a, winner, finalPrice)));
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

    private static java.util.List<Auction> latestAllAuctions = new java.util.ArrayList<>();
    private static final java.util.function.Consumer<java.util.List<Auction>> globalListListener = (listFromServer) -> {
        if (listFromServer != null) {
            latestAllAuctions = listFromServer;
        }
    };

    private static void registerGlobalListListener() {
        ClientNetworkManager.getInstance().removeAuctionListListener(globalListListener);
        ClientNetworkManager.getInstance().addAuctionListListener(globalListListener);
    }

    private static Auction findInLatestList(String auctionId) {
        if (latestAllAuctions != null) {
            for (Auction a : latestAllAuctions) {
                if (a.getId().equals(auctionId)) {
                    return a;
                }
            }
        }
        return null;
    }

    /**
     * Xử lý thông báo chung: tìm auction trong danh sách thô chưa lọc từ Server.
     */
    private static void handleNotification(String auctionId, TableView<Auction> table,
                                           Consumer<Auction> notificationAction) {
        registerGlobalListListener();
        
        Auction auction = findInLatestList(auctionId);
        
        // Cố gắng tìm trong bảng hiện tại nếu không thấy trong latestAllAuctions (để tránh race condition)
        if (auction == null && table != null && table.getItems() != null) {
            for (Auction a : table.getItems()) {
                if (a.getId().equals(auctionId)) {
                    auction = a;
                    break;
                }
            }
        }

        if (auction != null) {
            notificationAction.accept(auction);
        } else {
            // Đăng ký listener tạm thời chờ danh sách cập nhật từ Server
            Consumer<List<Auction>> oneTimeListener = new Consumer<>() {
                @Override
                public void accept(List<Auction> list) {
                    javafx.application.Platform.runLater(() -> {
                        Auction a = null;
                        if (list != null) {
                            for (Auction item : list) {
                                if (item.getId().equals(auctionId)) {
                                    a = item;
                                    break;
                                }
                            }
                        }
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
    private static void pushFinishedNotification(Auction auction, String winner, double finalPrice) {
        if (RoleBasedFilterHelper.shouldReceiveNotification(auction)) {
            NotificationManager.getInstance()
                    .addNotification("🏁 PHIÊN ĐẤU GIÁ KẾT THÚC! [" + auction.getItem().getName() + "] - Người thắng: " + winner + " ($" + view.utility.ChartHelper.formatDouble(finalPrice) + ")");
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
            int round = (auction.getBidHistory() != null ? auction.getBidHistory().size() : 0) + 1;
            NotificationManager.getInstance()
                    .addNotification("📢 [" + auction.getItem().getName() + "] - Lượt #" + round + ": " + topBidder + " vừa đặt $" + view.utility.ChartHelper.formatDouble(newPrice));
        }
    }
}
