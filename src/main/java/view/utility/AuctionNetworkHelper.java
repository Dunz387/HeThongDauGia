package view.utility;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.TableView;
import model.auction.Auction;
import network.ClientNetworkManager;
import shared.Protocol;

import java.util.function.Predicate;

/**
 * Helper class tập trung xử lý đăng ký network listener cho bảng Auction (SRP + DIP).
 * T7: Sửa dùng addAuctionListListener (multi-listener) thay vì setAuctionListListener (single).
 */
public class AuctionNetworkHelper {

    /**
     * Đăng ký lắng nghe danh sách đấu giá từ Server và hiển thị lên bảng.
     * Tự động gửi yêu cầu lấy danh sách khi được gọi.
     *
     * @param table Bảng TableView cần cập nhật
     */
    public static void registerAuctionListListener(TableView<Auction> table) {
        registerAuctionListListener(table, null);
    }

    /**
     * Đăng ký lắng nghe danh sách đấu giá từ Server với bộ lọc tùy chọn.
     * T7: Sử dụng addAuctionListListener để hỗ trợ nhiều Controller cùng lắng nghe.
     *
     * @param table  Bảng TableView cần cập nhật
     * @param filter Bộ lọc (VD: chỉ hiển thị RUNNING). Null = hiển thị tất cả.
     */
    public static void registerAuctionListListener(TableView<Auction> table, Predicate<Auction> filter) {
        // GIẢI PHÓNG BỘ NHỚ: Xóa listener cũ của bảng đấu giá nếu có
        ClientNetworkManager.getInstance().clearAuctionListListeners();

        // T7: Dùng addAuctionListListener thay vì setAuctionListListener
        ClientNetworkManager.getInstance().addAuctionListListener((listFromServer) -> {
            if (listFromServer != null) {
                var displayList = (filter != null)
                        ? listFromServer.stream().filter(filter).toList()
                        : listFromServer;

                ObservableList<Auction> data = FXCollections.observableArrayList(displayList);
                Platform.runLater(() -> table.setItems(data));
            }
        });

        // Lắng nghe BROADCAST_NEW_BID để tự động cập nhật bảng real-time
        registerBidUpdateRefresh();

        // Gửi yêu cầu lấy danh sách đấu giá mới nhất
        requestAuctionList();
    }

    /**
     * Đăng ký lắng nghe BROADCAST_NEW_BID để tự động refresh danh sách.
     */
    public static void registerBidUpdateRefresh() {
        ClientNetworkManager.getInstance().clearListeners(Protocol.BROADCAST_NEW_BID);
        ClientNetworkManager.getInstance().registerListener(Protocol.BROADCAST_NEW_BID, (message) -> {
            ClientNetworkManager.getInstance().sendData(Protocol.REQ_GET_AUCTIONS);
        });
    }

    /**
     * Gửi yêu cầu lấy danh sách đấu giá mới nhất từ Server.
     */
    public static void requestAuctionList() {
        ClientNetworkManager.getInstance().sendData(Protocol.REQ_GET_AUCTIONS);
    }
}
