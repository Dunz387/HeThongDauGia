package network;

import javafx.beans.property.*;
import model.user.Role;
import java.util.logging.Logger;

/**
 * Singleton lưu trữ thông tin phiên đăng nhập của user hiện tại trên Client.
 * Sử dụng JavaFX Properties để các Controller có thể bind dữ liệu realtime.
 */
public class SessionManager {
    private static final Logger LOGGER = Logger.getLogger(SessionManager.class.getName());
    private static final SessionManager instance = new SessionManager();

    private final StringProperty userId = new SimpleStringProperty();
    private final StringProperty username = new SimpleStringProperty();
    private final ObjectProperty<Role> role = new SimpleObjectProperty<>();
    private final DoubleProperty balance = new SimpleDoubleProperty(0);
    private final BooleanProperty loggedIn = new SimpleBooleanProperty(false);

    private SessionManager() {}

    public static SessionManager getInstance() {
        return instance;
    }

    /**
     * Lưu thông tin user sau khi login thành công.
     */
    public void setSession(String userId, String username, String roleStr, double balance) {
        this.userId.set(userId);
        this.username.set(username);
        this.role.set(Role.valueOf(roleStr));
        this.balance.set(balance);
        this.loggedIn.set(true);
        LOGGER.info(String.format("✅ [Session] Đã lưu phiên: %s (%s) | Balance: $%.2f", username, roleStr, balance));
    }

    /**
     * Cập nhật số dư.
     */
    public void updateBalance(double newBalance) {
        this.balance.set(newBalance);
    }

    /**
     * Xóa phiên khi logout.
     */
    public void clearSession() {
        this.userId.set(null);
        this.username.set(null);
        this.role.set(null);
        this.balance.set(0);
        this.loggedIn.set(false);
        LOGGER.info("🚪 [Session] Đã xóa phiên đăng nhập.");
    }

    // --- Property Getters (cho việc Binding) ---

    public StringProperty userIdProperty() { return userId; }
    public StringProperty usernameProperty() { return username; }
    public ObjectProperty<Role> roleProperty() { return role; }
    public DoubleProperty balanceProperty() { return balance; }
    public BooleanProperty loggedInProperty() { return loggedIn; }

    // --- Standard Getters/Setters ---

    public String getUserId() { return userId.get(); }
    public String getUsername() { return username.get(); }
    public Role getRole() { return role.get(); }
    public double getBalance() { return balance.get(); }
    public boolean isLoggedIn() { return loggedIn.get(); }

    public void setBalance(double balance) { this.balance.set(balance); }

    /**
     * Kiểm tra user hiện tại có phải Admin không.
     */
    public boolean isAdmin() {
        return role.get() == Role.ADMIN;
    }

    /**
     * Kiểm tra user hiện tại có phải Seller không.
     */
    public boolean isSeller() {
        return role.get() == Role.SELLER;
    }

    /**
     * Kiểm tra user hiện tại có phải Bidder không.
     */
    public boolean isBidder() {
        return role.get() == Role.BIDDER;
    }
}
