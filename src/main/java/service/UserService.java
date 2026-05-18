package service;

import dao.UserDAO;
import model.user.Bidder;
import model.user.Seller;
import model.user.User;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Service chuyên quản lý User: xác thực, đăng ký, truy vấn danh sách.
 * Tách từ AuctionManager để tuân thủ SRP.
 */
public class UserService {
    private static final Logger LOGGER = Logger.getLogger(UserService.class.getName());
    private static final UserService instance = new UserService();
    private final List<User> users;

    private UserService() {
        this.users = UserDAO.loadUsers();
    }

    public static UserService getInstance() { return instance; }

    public List<User> getAllUsers() {
        return new ArrayList<>(users);
    }

    public List<User> getUsersRef() {
        return users;
    }

    public User findUserById(String userId) {
        synchronized (users) {
            for (User u : users) {
                if (u.getId().equals(userId)) return u;
            }
        }
        return null;
    }

    public User authenticateUser(String username, String password) {
        synchronized (users) {
            for (User u : users) {
                if (u.getUsername().equals(username) && u.login(password)) {
                    if (!u.isActive()) return null;
                    return u;
                }
            }
        }
        return null;
    }

    public boolean registerNewUser(String username, String password, String role) {
        synchronized (users) {
            for (User u : users) {
                if (u.getUsername().equals(username)) return false;
            }
            User newUser;
            if ("SELLER".equals(role)) {
                newUser = new Seller("U-" + System.currentTimeMillis(), username, password, 0.0);
            } else {
                newUser = new Bidder("U-" + System.currentTimeMillis(), username, password, 100000.0);
            }
            users.add(newUser);
            UserDAO.saveUser(newUser);
            return true;
        }
    }
}
