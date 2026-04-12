package model.UserType;

import java.util.ArrayList;
import java.util.List;

/**
 * Lớp Bidder đại diện cho người dùng tham gia vào các phiên đấu giá.
 * Lớp này kế thừa các thuộc tính và phương thức cơ bản từ lớp User, 
 * đồng thời bổ sung các đặc điểm liên quan đến việc đặt giá, số dư tài khoản và độ uy tín.
 */
public class Bidder extends User {

    // --- 1. Liệt kê (Enums) ---
    public enum MembershipTier {
        BRONZE, SILVER, GOLD, PLATINUM
    }

    // --- 2. Các thuộc tính riêng của Bidder ---
    private double accountBalance;
    private int reputationScore; // Ví dụ: từ 0 đến 100. Điểm uy tín thấp có thể bị cấm đặt giá.
    private boolean isBanned;
    private MembershipTier tier;
    private List<String> bidHistory; // Trong ứng dụng thực tế, nên dùng List<Bid> hoặc List<Item>

    /**
     * Constructor dùng để khởi tạo một Bidder mới.
     * Kế thừa thông tin cơ bản từ lớp User và thiết lập các giá trị mặc định cho Bidder.
     */
    public Bidder(String username, String password, String email, String fullName, 
                  String phone, String address, String city, String country) {
        
        super(username, password, email, fullName, phone, address, city, country);
        
        // Khởi tạo các giá trị mặc định cho một người tham gia đấu giá mới
        this.accountBalance = 0.0;
        this.reputationScore = 100; // Bắt đầu với điểm uy tín tuyệt đối
        this.isBanned = false;
        this.tier = MembershipTier.BRONZE;
        this.bidHistory = new ArrayList<>();
    }

    // --- 3. Getter và Setter (Tính đóng gói) ---

    public double getAccountBalance() { return accountBalance; }
    
    public int getReputationScore() { return reputationScore; }
    
    public void setReputationScore(int reputationScore) {
        // Đảm bảo điểm uy tín luôn bị giới hạn trong khoảng từ 0 đến 100
        this.reputationScore = Math.max(0, Math.min(100, reputationScore)); 
    }

    public boolean isBanned() { return isBanned; }
    
    public void setBanned(boolean banned) { isBanned = banned; }

    public MembershipTier getTier() { return tier; }

    public List<String> getBidderBidHistory() { return bidHistory; }

    // --- 4. Logic nghiệp vụ cốt lõi (Hành vi) ---

    /**
     * Nạp tiền vào tài khoản của người đấu giá.
     *
     * @param amount Số tiền muốn nạp.
     */
    public void deposit(double amount) {
        if (amount <= 0) {
            System.out.println("Error: Deposit amount must be strictly positive.");
            return; // Chỉ dùng return để thoát hàm, không trả về giá trị
        }
        this.accountBalance += amount;
        System.out.println("Successfully deposited $" + amount + ". New balance: $" + this.accountBalance);
    }

    /**
     * Mô phỏng việc đặt giá cho một vật phẩm.
     * Phương thức này sẽ kiểm tra trạng thái tài khoản, số dư và điểm uy tín trước khi cho phép đặt giá.
     * @param itemName Tên của vật phẩm đang được đấu giá.
     * @param bidAmount Số tiền người dùng muốn đặt.
     * @return true nếu đặt giá thành công, false nếu thất bại.
     */
    public boolean placeBid(String itemName, double bidAmount) {
        if (this.isBanned) {
            System.out.println("Error: Your account is banned from participating in auctions.");
            return false;
        }
        if (this.reputationScore < 50) {
            System.out.println("Error: Reputation score too low to place bids. Contact support.");
            return false;
        }
        if (bidAmount > this.accountBalance) {
            System.out.println("Error: Insufficient funds. Please deposit more money.");
            return false;
        }

        // Trừ tiền trong tài khoản (hoặc tạm giữ tiền) và lưu lại lịch sử đặt giá
        this.accountBalance -= bidAmount;
        String record = "Bid $" + bidAmount + " on '" + itemName + "'";
        this.bidHistory.add(record);
        
        System.out.println("Success! You placed a bid of $" + bidAmount + " on " + itemName);
        updateMembershipTier(); // Kiểm tra xem người dùng có đủ điều kiện để thăng hạng thành viên không
        return true;
    }

    /**
     * Phương thức hỗ trợ nội bộ (private) để tự động nâng cấp hạng thành viên của người dùng 
     * dựa trên mức độ hoạt động (số lần đặt giá).
     */
    private void updateMembershipTier() {
        int totalBids = this.bidHistory.size();
        if (totalBids > 50) this.tier = MembershipTier.PLATINUM;
        else if (totalBids > 20) this.tier = MembershipTier.GOLD;
        else if (totalBids > 5) this.tier = MembershipTier.SILVER;
    }

    // --- 5. Ghi đè phương thức (Overridden Methods) ---

    @Override
    public String getRole() {
        return "BIDDER";
    }

    @Override
    public void printInfo() {
        System.out.println("\n BIDDER PROFILE: ");
        
        // In ra các thông tin cơ bản của User (đã được định nghĩa ở lớp cha)
        super.printInfo(); 
        
        // In ra các thông tin đặc thù của riêng Bidder
        System.out.println("--- Account Status ---");
        System.out.println("Role          : " + getRole());
        System.out.println("Tier          : " + this.tier);
        System.out.println("Balance       : $" + String.format("%.2f", this.accountBalance));
        System.out.println("Reputation    : " + this.reputationScore + "/100");
        System.out.println("Account Status: " + (this.isBanned ? "BANNED" : "ACTIVE"));
        System.out.println("Total Bids    : " + this.bidHistory.size());
    }
}
