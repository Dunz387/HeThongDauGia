package server.handler;

import model.user.User;
import service.UserService;
import shared.Protocol;
import server.AuctionServer;

import java.util.logging.Logger;

/**
 * Xử lý các request xác thực: Login, Register, Logout.
 * Tách từ ClientHandler để tuân thủ SRP.
 */
public class AuthHandler {
    private static final Logger LOGGER = Logger.getLogger(AuthHandler.class.getName());
    private final server.ClientHandler clientHandler;
    private final AuctionServer server;

    public AuthHandler(server.ClientHandler clientHandler, AuctionServer server) {
        this.clientHandler = clientHandler;
        this.server = server;
    }

    public void handleLogin(String u, String p) {
        User user = UserService.getInstance().authenticateUser(u, p);
        if (user != null) {
            if (server.isUserLoggedIn(user.getId())) {
                clientHandler.sendData(Protocol.REQ_LOGIN + Protocol.DELIMITER + Protocol.RES_FAIL
                        + Protocol.DELIMITER + "Tài khoản này hiện đang đăng nhập ở một nơi khác!");
                return;
            }

            clientHandler.setLoggedInUser(user);
            double balance = 0.0;
            if (user instanceof model.user.Bidder) balance = ((model.user.Bidder) user).getAvailableBalance();
            else if (user instanceof model.user.Seller) balance = ((model.user.Seller) user).getBalance();

            clientHandler.sendData(Protocol.REQ_LOGIN + Protocol.DELIMITER + Protocol.RES_SUCCESS
                    + Protocol.DELIMITER + user.getRole()
                    + Protocol.DELIMITER + user.getId()
                    + Protocol.DELIMITER + user.getUsername()
                    + Protocol.DELIMITER + balance);
        } else {
            clientHandler.sendData(Protocol.REQ_LOGIN + Protocol.DELIMITER + Protocol.RES_FAIL + Protocol.DELIMITER + "Tài khoản không tồn tại hoặc đã bị khóa!");
        }
    }

    public void handleRegister(String u, String p, String role) {
        if (UserService.getInstance().registerNewUser(u, p, role)) {
            clientHandler.sendData(Protocol.REQ_REGISTER + Protocol.DELIMITER + Protocol.RES_SUCCESS);
        } else {
            clientHandler.sendData(Protocol.REQ_REGISTER + Protocol.DELIMITER + Protocol.RES_FAIL);
        }
    }

    public void handleLogout() {
        LOGGER.info("🚪 User logged out: " + (clientHandler.getLoggedInUser() != null ? clientHandler.getLoggedInUser().getUsername() : "Guest"));
        try {
            for (String roomId : clientHandler.getCurrentRoomIds()) {
                server.leaveRoom(roomId, clientHandler);
            }
            clientHandler.getCurrentRoomIds().clear();
        } catch (Exception e) {
            LOGGER.warning("Lỗi khi rời phòng đấu giá lúc logout");
        }
        clientHandler.setLoggedInUser(null);
        clientHandler.sendData(Protocol.REQ_LOGOUT + Protocol.DELIMITER + Protocol.RES_SUCCESS);
    }
}
