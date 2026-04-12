package controller;

import dao.UserDAO;
import model.BidTransaction;
import model.UserType.User;
import model.Item.Item;
import model.Notification;

public class UserController {

    private UserDAO userDAO;

    public UserController(UserDAO userDAO) {
        this.userDAO = userDAO;
    }

    // Đăng ký
    public void register(User user) {
        if (user == null) return;

        if (userDAO.findbyUsername(user.getUsername()) != null) {
            System.out.println("Username already exists!");
            return;
        }

        userDAO.add(user);
        System.out.println("Register successful!");
    }

    // Đăng nhập
    public User login(String username, String password) {
        User user = userDAO.login(username, password);

        if (user == null) {
            System.out.println("Invalid username or password!");
            return null;
        }

        if (!user.isActive()) {
            System.out.println("User is inactive!");
            return null;
        }

        System.out.println("Login successful!");
        return user;
    }

    // Số dư
    public void deposit(User user, double amount) {
        if (user != null) user.deposit(amount);
    }

    public void withdraw(User user, double amount) {
        if (user != null) user.withdraw(amount);
    }

    // Hàng chờ Item
    public void addToWatchList(User user, Item item) {
        if (user != null && item != null) {
            user.addToWatchList(item);
        }
    }

    // Xem thông báo
    public void showNotifications(User user) {
        if (user == null) return;

        for (Notification n : user.getNotifications()) {
            System.out.println(n);
        }
    }

    // Lịch sử đấu giá
    public void showBidHistory(User user) {
        if (user == null) return;

        for (BidTransaction bid : user.getBidHistory()) {
            System.out.println(bid);
        }
    }

    // Thông tin
    public void showUserInfo(User user) {
        if (user != null) {
            user.printInfo();
        }
    }
}