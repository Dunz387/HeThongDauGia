package server.notification;

import model.user.Bidder;
import model.user.Seller;
import model.user.User;
import server.ClientHandler;
import shared.Protocol;

import java.util.List;
import java.util.logging.Logger;

public final class ClientNotifier {
    private static final Logger LOGGER = Logger.getLogger(ClientNotifier.class.getName());
    private final List<ClientHandler> clients;

    public ClientNotifier(List<ClientHandler> clients) {
        this.clients = clients;
    }

    public void sendBalanceUpdateToUser(String userId) {
        for (ClientHandler client : clients) {
            User user = client.getLoggedInUser();
            if (user != null && user.getId().equals(userId)) {
                client.sendData(Protocol.RES_UPDATE_BALANCE + Protocol.DELIMITER + balanceOf(user));
            }
        }
    }

    public boolean isUserLoggedIn(String userId) {
        if (userId == null) {
            return false;
        }
        for (ClientHandler client : clients) {
            User user = client.getLoggedInUser();
            if (user != null && userId.equals(user.getId())) {
                return true;
            }
        }
        return false;
    }

    public void notifyAutoBidExpired(Bidder bidder) {
        for (ClientHandler client : clients) {
            User user = client.getLoggedInUser();
            if (user != null && user.getId().equals(bidder.getId())) {
                client.sendData(Protocol.REQ_AUTOBID + Protocol.DELIMITER
                        + Protocol.RES_SUCCESS + Protocol.DELIMITER + "CANCEL");
                break;
            }
        }
    }

    public void forceLogoutUser(String targetUserId) {
        for (ClientHandler client : clients) {
            User user = client.getLoggedInUser();
            if (user != null && targetUserId.equals(user.getId())) {
                LOGGER.info("Force logout: " + user.getUsername());
                client.forceLogout("Tài khoản của bạn đã bị cấm bởi hệ thống!");
                break;
            }
        }
    }

    public static double balanceOf(User user) {
        if (user instanceof Bidder) {
            return ((Bidder) user).getAvailableBalance();
        }
        if (user instanceof Seller) {
            return ((Seller) user).getBalance();
        }
        return 0.0;
    }
}
