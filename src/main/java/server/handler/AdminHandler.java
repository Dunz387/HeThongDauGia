package server.handler;

import model.user.Admin;
import model.user.User;
import service.AdminService;
import service.UserService;
import shared.Protocol;
import server.AuctionServer;

import java.util.List;
import java.util.logging.Logger;

/**
 * Xử lý các request quản trị: GetUsers, BanUser, KickUser, DeleteAuction, UpdateBalance.
 * Tách từ ClientHandler để tuân thủ SRP.
 */
public class AdminHandler {
    private static final Logger LOGGER = Logger.getLogger(AdminHandler.class.getName());
    private final server.ClientHandler clientHandler;
    private final AuctionServer server;

    public AdminHandler(server.ClientHandler clientHandler, AuctionServer server) {
        this.clientHandler = clientHandler;
        this.server = server;
    }

    private User getUser() { return clientHandler.getLoggedInUser(); }

    public void handleGetUsers() {
        if (getUser() instanceof Admin) {
            List<User> list = UserService.getInstance().getAllUsers();
            clientHandler.sendData(Protocol.RES_USER_LIST);
            clientHandler.sendData(list);
        } else {
            clientHandler.sendData(Protocol.REQ_GET_USERS + Protocol.DELIMITER + Protocol.RES_FAIL + Protocol.DELIMITER + "Quyền truy cập bị từ chối!");
        }
    }

    public void handleBanUser(String targetId, String statusStr) {
        if (getUser() instanceof Admin) {
            boolean isEnable = Boolean.parseBoolean(statusStr);
            if (AdminService.getInstance().banUser(targetId, isEnable)) {
                clientHandler.sendData(Protocol.REQ_BAN_USER + Protocol.DELIMITER + Protocol.RES_SUCCESS);
                if (!isEnable) {
                    server.forceLogoutUser(targetId);
                }
                server.broadcastUserList();
            } else {
                clientHandler.sendData(Protocol.REQ_BAN_USER + Protocol.DELIMITER + Protocol.RES_FAIL + Protocol.DELIMITER + "Lỗi cập nhật trạng thái!");
            }
        } else {
            clientHandler.sendData(Protocol.REQ_BAN_USER + Protocol.DELIMITER + Protocol.RES_FAIL + Protocol.DELIMITER + "Chỉ Admin mới được thực hiện!");
        }
    }

    public void handleUpdateUserBalance(String targetId, String newBalanceStr) {
        if (getUser() instanceof Admin) {
            try {
                double newBalance = Double.parseDouble(newBalanceStr);
                if (newBalance < 0) {
                    clientHandler.sendData(Protocol.REQ_UPDATE_USER_BALANCE + Protocol.DELIMITER + Protocol.RES_FAIL + Protocol.DELIMITER + "Số dư không thể là số âm!");
                    return;
                }
                String result = AdminService.getInstance().updateUserBalanceForce(targetId, newBalance);
                if ("SUCCESS".equals(result)) {
                    clientHandler.sendData(Protocol.REQ_UPDATE_USER_BALANCE + Protocol.DELIMITER + Protocol.RES_SUCCESS);
                    server.broadcastUserList();
                    server.sendBalanceUpdateToUser(targetId);
                } else {
                    clientHandler.sendData(Protocol.REQ_UPDATE_USER_BALANCE + Protocol.DELIMITER + Protocol.RES_FAIL + Protocol.DELIMITER + result);
                }
            } catch (NumberFormatException e) {
                clientHandler.sendData(Protocol.REQ_UPDATE_USER_BALANCE + Protocol.DELIMITER + Protocol.RES_FAIL + Protocol.DELIMITER + "Số tiền không hợp lệ.");
            }
        } else {
            clientHandler.sendData(Protocol.REQ_UPDATE_USER_BALANCE + Protocol.DELIMITER + Protocol.RES_FAIL + Protocol.DELIMITER + "Chỉ Admin mới được thực hiện!");
        }
    }

    public void handleDeleteAuction(String auctionId) {
        if (getUser() instanceof Admin) {
            if (AdminService.getInstance().deleteAuctionForce(auctionId)) {
                clientHandler.sendData(Protocol.REQ_DELETE_AUCTION + Protocol.DELIMITER + Protocol.RES_SUCCESS);
                server.broadcastRoomKicked(auctionId, "Phiên đấu giá đã bị Admin xóa!");
                server.broadcastAuctionList();
            } else {
                clientHandler.sendData(Protocol.REQ_DELETE_AUCTION + Protocol.DELIMITER + Protocol.RES_FAIL + Protocol.DELIMITER + "Không tìm thấy phiên đấu giá!");
            }
        } else {
            clientHandler.sendData(Protocol.REQ_DELETE_AUCTION + Protocol.DELIMITER + Protocol.RES_FAIL + Protocol.DELIMITER + "Chỉ Admin mới được thực hiện!");
        }
    }

    public void handleKickUser(String auctionId, String targetUsername) {
        if (getUser() instanceof Admin) {
            server.kickUserFromRoom(auctionId, targetUsername);
        }
    }
}
