package model.UserType;

import java.util.ArrayList;
import java.util.List;


/**
 * Lớp Admin đại diện cho quản trị viên của hệ thống đấu giá.
 * Kế thừa từ lớp User và bổ sung các đặc quyền quản lý hệ thống, 
 * theo dõi hoạt động và xử lý vi phạm.
 */
public class Admin extends User {

    // --- 1. Liệt kê (Enums) ---
    public enum AdminLevel {
        SUPER_ADMIN,    // Quản trị viên cấp cao nhất, toàn quyền
        MODERATOR,      // Người kiểm duyệt nội dung, duyệt vật phẩm
        SUPPORT         // Nhân viên hỗ trợ giải quyết tranh chấp
    }

    // --- 2. Các thuộc tính riêng của Admin ---
    private AdminLevel level;
    private String department;
    private List<String> actionLogs; // Lưu trữ nhật ký các hành động của Admin (Audit Trail)

    /**
     * Constructor dùng để khởi tạo một Admin mới.
     * Kế thừa thông tin cơ bản từ lớp User và thiết lập quyền hạn quản trị.
     */
    public Admin(String username, String password, String email, String fullName, 
                 String phone, String address, String city, String country, 
                 AdminLevel level, String department) {
        
        super(username, password, email, fullName, phone, address, city, country);
        
        this.level = level;
        this.department = department;
        this.actionLogs = new ArrayList<>();
        
        logAction("Account created and assigned to department: " + department);
    }

    // --- 3. Getter và Setter (Tính đóng gói) ---

    public AdminLevel getLevel() { return level; }
    
    public void setLevel(AdminLevel level) { 
        this.level = level; 
        logAction("Admin level changed to " + level);
    }

    public String getDepartment() { return department; }
    
    public void setDepartment(String department) { 
        this.department = department; 
        logAction("Transferred to department: " + department);
    }

    public List<String> getActionLogs() { return actionLogs; }

    // --- 4. Logic nghiệp vụ cốt lõi (Hành vi quản trị) ---

    /**
     * Phương thức hỗ trợ nội bộ để ghi lại các thao tác của Admin vào hệ thống log.
     * @param action Hành động vừa thực hiện.
     */
    private void logAction(String action) {
        // Trong thực tế sẽ đi kèm timestamp (thời gian thực hiện)
        this.actionLogs.add("[LOG] " + action);
    }

    /**
     * Khóa tài khoản của một người tham gia đấu giá nếu phát hiện vi phạm.
     * Yêu cầu quyền MODERATOR hoặc SUPER_ADMIN.
     * @param bidder Đối tượng người dùng bị khóa.
     * @param reason Lý do khóa tài khoản.
     * @return true nếu khóa thành công, false nếu không đủ thẩm quyền.
     */
    public boolean banBidder(Bidder bidder, String reason) {
        if (this.level == AdminLevel.SUPPORT) {
            System.out.println("Error: SUPPORT level admins do not have permission to ban users.");
            return false;
        }

        bidder.setBanned(true); // Gọi phương thức từ lớp Bidder
        bidder.setReputationScore(0); // Phạt điểm uy tín về 0
        
        String logMessage = "Banned bidder '" + bidder.getUsername() + "'. Reason: " + reason;
        logAction(logMessage);
        
        System.out.println("Success: " + logMessage);
        return true;
    }

    /**
     * Duyệt một vật phẩm để cho phép đưa lên sàn đấu giá.
     * @param itemName Tên vật phẩm.
     */
    public void approveItem(String itemName) {
        String logMessage = "Approved item for auction: '" + itemName + "'";
        logAction(logMessage);
        System.out.println("System Alert: " + logMessage);
    }

    /**
     * In ra toàn bộ lịch sử thao tác của Admin này.
     */
    public void printActionLogs() {
        System.out.println("\n--- Audit Logs for Admin: " + getUsername() + " ---");
        if (actionLogs.isEmpty()) {
            System.out.println("No actions logged yet.");
        } else {
            for (String log : actionLogs) {
                System.out.println(log);
            }
        }
        System.out.println("----------------------------------------\n");
    }

    // --- 5. Ghi đè phương thức (Overridden Methods) ---

    @Override
    public String getRole() {
        return "ADMIN";
    }

    @Override
    public void printInfo() {
        System.out.println("\n========== ADMIN PROFILE ==========");
        
        // In ra các thông tin cơ bản của User (đã được định nghĩa ở lớp cha)
        super.printInfo(); 
        
        // In ra các thông tin đặc thù của riêng Admin
        System.out.println("--- Administration Details ---");
        System.out.println("Role          : " + getRole());
        System.out.println("Admin Level   : " + this.level);
        System.out.println("Department    : " + this.department);
        System.out.println("Total Actions : " + this.actionLogs.size() + " logged events");
    }
}