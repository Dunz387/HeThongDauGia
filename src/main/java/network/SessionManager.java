package network;

import model.user.Role;

/**
 * Singleton lưu trữ thông tin phiên đăng nhập của user hiện tại trên Client.
 * Được cập nhật khi login thành công, và được đọc bởi tất cả Controller.
 */
public class SessionManager {
    private static final SessionManager instance = new SessionManager();

    private String userId;
    private String username;
    private Role role;
    private double balance;
    private boolean loggedIn = false;

    private SessionManager() {}

    public static SessionManager getInstance() {
        return instance;
    }

    /**
     * Lưu thông tin user sau khi login thành công.
     */
    public void setSession(String userId, String username, String roleStr, double balance) {
        this.userId = userId;
        this.username = username;
        this.role = Role.valueOf(roleStr);
        this.balance = balance;
        this.loggedIn = true;
        System.out.println("✅ [Session] Đã lưu phiên: " + username + " (" + roleStr + ") | Balance: $" + balance);
    }

    /**
     * Xóa phiên khi logout.
     */
    public void updateBalance(double newBalance) {
        this.balance = newBalance;
    }

    public void clearSession() {
        this.userId = null;
        this.username = null;
        this.role = null;
        this.balance = 0;
        this.loggedIn = false;
        System.out.println("🚪 [Session] Đã xóa phiên đăng nhập.");
    }

    //Getters

    public String getUserId() { return userId; }
    public String getUsername() { return username; }
    public Role getRole() { return role; }
    public double getBalance() { return balance; }
    public boolean isLoggedIn() { return loggedIn; }

    public void setBalance(double balance) { this.balance = balance; }

    /**
     * Kiểm tra user hiện tại có phải Admin không.
     */
    public boolean isAdmin() {
        return role == Role.ADMIN;
    }

    /**
     * Kiểm tra user hiện tại có phải Seller không.
     */
    public boolean isSeller() {
        return role == Role.SELLER;
    }

    /**
     * Kiểm tra user hiện tại có phải Bidder không.
     */
    public boolean isBidder() {
        return role == Role.BIDDER;
    }
}
