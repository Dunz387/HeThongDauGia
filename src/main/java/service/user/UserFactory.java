package service.user;

import model.user.Bidder;
import model.user.Seller;
import model.user.User;

final class UserFactory {
    private static final double DEFAULT_BIDDER_BALANCE = 100000.0;
    private static final double DEFAULT_SELLER_BALANCE = 0.0;

    private UserFactory() {
    }

    static User create(String username, String password, String role) {
        String userId = "U-" + System.currentTimeMillis();
        if ("SELLER".equals(role)) {
            return new Seller(userId, username, password, DEFAULT_SELLER_BALANCE);
        }
        return new Bidder(userId, username, password, DEFAULT_BIDDER_BALANCE);
    }
}
