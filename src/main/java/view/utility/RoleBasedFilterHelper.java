package view.utility;

import model.auction.Auction;
import model.auction.AuctionStatus;
import network.SessionManager;

import java.util.function.Predicate;

/**
 * Tập trung mọi logic lọc Auction theo vai trò người dùng (SRP + DRY).
 * Được sử dụng bởi: AssetsListController, BaseMenuController, RoomMenuChoiceController, UserInfoController.
 */
public class RoleBasedFilterHelper {

    private RoleBasedFilterHelper() {} // Utility class

    // ============================
    // 1. BỘ LỌC BẢNG TÀI SẢN
    // ============================

    /**
     * Seller: Chỉ thấy sản phẩm còn đăng bán (chưa kết thúc).
     * Bidder: Chỉ thấy sản phẩm đấu giá thành công (đã kết thúc & mình thắng).
     * Admin: Thấy tất cả.
     */
    public static Predicate<Auction> getAssetsFilter() {
        String userId = SessionManager.getInstance().getUserId();
        if (SessionManager.getInstance().isAdmin()) return a -> true;
        if (SessionManager.getInstance().isSeller()) return a -> isOwnedBySeller(a, userId);
        if (SessionManager.getInstance().isBidder()) return a -> isWonByBidder(a, userId);
        return a -> false;
    }

    // ============================
    // 2. BỘ LỌC PHÒNG ĐẤU GIÁ
    // ============================

    /**
     * Seller: Chỉ thấy phòng của mình đang diễn ra.
     * Bidder & Admin: Thấy tất cả phòng đang diễn ra.
     */
    public static Predicate<Auction> getRoomFilter() {
        String userId = SessionManager.getInstance().getUserId();
        if (SessionManager.getInstance().isAdmin()) return a -> true;
        if (SessionManager.getInstance().isSeller()) {
            return a -> isRunning(a) && isOwner(a, userId);
        }
        // Bidder: tất cả phòng đang chạy
        return RoleBasedFilterHelper::isRunning;
    }

    // ============================
    // 3. KIỂM TRA QUYỀN NHẬN THÔNG BÁO
    // ============================

    /**
     * Seller: Nhận thông báo từ phòng tài sản của mình.
     * Bidder: Nhận thông báo từ phòng mình đang tham dự.
     * Admin: Nhận tất cả.
     */
    public static boolean shouldReceiveNotification(Auction auction) {
        String userId = SessionManager.getInstance().getUserId();
        if (SessionManager.getInstance().isAdmin()) return true;
        if (SessionManager.getInstance().isSeller()) return isOwner(auction, userId);
        if (SessionManager.getInstance().isBidder()) return hasParticipated(auction, userId);
        return false;
    }

    // ============================
    // 4. ĐẾM SỐ TÀI SẢN (Profile)
    // ============================

    /**
     * Đếm tài sản sở hữu: dùng cùng logic với bảng tài sản.
     * Seller: đếm sản phẩm đang bán.
     * Bidder: đếm sản phẩm đã thắng.
     */
    public static long countAssets(java.util.List<Auction> auctionList) {
        String userId = SessionManager.getInstance().getUserId();
        return auctionList.stream()
                .filter(a -> isOwnedBySeller(a, userId) || isWonByBidder(a, userId))
                .count();
    }

    // ============================
    // PHƯƠNG THỨC NỀN TẢNG (Private)
    // ============================

    /** Sản phẩm đang được Seller đăng bán (chưa kết thúc & mình là người bán gốc) */
    private static boolean isOwnedBySeller(Auction a, String userId) {
        return a.getStatus() != AuctionStatus.FINISHED &&
               a.getSeller() != null &&
               a.getSeller().getId().equals(userId);
    }

    /** Sản phẩm Bidder đã đấu giá thắng cuộc (đã kết thúc & mình là người trả giá cao nhất) */
    private static boolean isWonByBidder(Auction a, String userId) {
        return a.getStatus() == AuctionStatus.FINISHED &&
               a.getHighestBidder() != null &&
               a.getHighestBidder().getId().equals(userId);
    }

    /** Kiểm tra mình là người bán gốc của sản phẩm */
    private static boolean isOwner(Auction a, String userId) {
        return a.getSeller() != null &&
               a.getSeller().getId().equals(userId);
    }

    /** Kiểm tra mình có tham gia đặt giá trong phiên này không */
    private static boolean hasParticipated(Auction a, String userId) {
        return a.getBidHistory().stream()
                .anyMatch(t -> t.getBidder().getId().equals(userId));
    }

    /** Kiểm tra phiên đấu giá đang diễn ra */
    private static boolean isRunning(Auction a) {
        return AuctionStatus.RUNNING == a.getStatus();
    }
}
