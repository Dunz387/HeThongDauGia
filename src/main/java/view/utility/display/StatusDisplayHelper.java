package view.utility.display;

/**
 * Helper class tập trung logic hiển thị trạng thái (SRP + OCP).
 * Thay thế các switch-case bị copy-paste trong 6+ controllers.
 * Khi cần thêm trạng thái mới, chỉ cần sửa 1 file duy nhất.
 */
public class StatusDisplayHelper {

    /**
     * Chuyển đổi tên trạng thái đấu giá sang hiển thị tiếng Việt có emoji.
     */
    public static String formatAuctionStatus(String statusName) {
        if (statusName == null) return "—";
        try {
            return model.auction.AuctionStatus.valueOf(statusName).getDisplayName();
        } catch (IllegalArgumentException e) {
            return statusName;
        }
    }

    /**
     * Chuyển đổi tên class Item sang hiển thị tiếng Việt có emoji.
     */
    public static String formatItemType(String className) {
        if (className == null) return "—";
        return switch (className) {
            case "Electronics" -> "🔌 Đồ điện";
            case "Vehicle" -> "🚗 Xe cộ";
            case "Arts" -> "🎨 Nghệ thuật";
            default -> className;
        };
    }

    /**
     * Chuyển đổi tên Role sang hiển thị tiếng Việt.
     */
    public static String formatUserRole(String roleName) {
        if (roleName == null) return "—";
        try {
            return model.user.Role.valueOf(roleName).getDisplayName();
        } catch (IllegalArgumentException e) {
            return roleName;
        }
    }

    /**
     * Chuyển đổi trạng thái hoạt động của User.
     */
    public static String formatUserStatus(boolean isActive) {
        return isActive ? "✅ Hoạt động" : "🚫 Bị khóa";
    }
}
