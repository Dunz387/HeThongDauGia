package view.utility;

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
        return switch (statusName) {
            case "OPEN" -> "⏳ Chưa bắt đầu";
            case "RUNNING" -> "🔥 Đang diễn ra";
            case "FINISHED" -> "✅ Đã kết thúc";
            case "PAID" -> "💰 Đã thanh toán";
            case "CANCELED" -> "❌ Đã hủy";
            default -> statusName;
        };
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
        return switch (roleName) {
            case "ADMIN" -> "Quản trị viên";
            case "SELLER" -> "Người bán";
            case "BIDDER" -> "Người mua";
            default -> roleName;
        };
    }

    /**
     * Chuyển đổi trạng thái hoạt động của User.
     */
    public static String formatUserStatus(boolean isActive) {
        return isActive ? "✅ Hoạt động" : "🚫 Bị khóa";
    }
}
