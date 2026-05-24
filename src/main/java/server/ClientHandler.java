package server;

import model.user.User;
import server.handler.AdminHandler;
import server.handler.AuctionHandler;
import server.handler.AuthHandler;
import server.handler.FinancialHandler;
import service.AuctionManager;
import shared.Protocol;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Router chính xử lý kết nối Client. Delegate request sang các Handler chuyên
 * biệt (SRP).
 */
public class ClientHandler implements Runnable {
    private static final Logger LOGGER = Logger.getLogger(ClientHandler.class.getName());
    private Socket socket;
    private AuctionServer server;
    private User loggedInUser = null;
    private Set<String> currentRoomIds = ConcurrentHashMap.newKeySet();
    private ObjectOutputStream out;
    private ObjectInputStream in;

    // Các handler chuyên biệt
    private final AuthHandler authHandler;
    private final AuctionHandler auctionHandler;
    private final AdminHandler adminHandler;
    private final FinancialHandler financialHandler;

    public ClientHandler(Socket socket, AuctionServer server, AuctionManager manager) {
        this.socket = socket;
        this.server = server;
        this.authHandler = new AuthHandler(this, server);
        this.auctionHandler = new AuctionHandler(this, server, manager);
        this.adminHandler = new AdminHandler(this, server);
        this.financialHandler = new FinancialHandler(this);
    }

    public User getLoggedInUser() {
        return loggedInUser;
    }

    public void setLoggedInUser(User user) {
        this.loggedInUser = user;
    }

    public Set<String> getCurrentRoomIds() {
        return currentRoomIds;
    }

    public String getUserId() {
        return (loggedInUser != null) ? loggedInUser.getId() : null;
    }

    @Override
    public void run() {
        try {
            out = new ObjectOutputStream(socket.getOutputStream());
            out.flush();
            in = new ObjectInputStream(socket.getInputStream());

            Object obj;
            while ((obj = in.readObject()) != null) {
                if (obj instanceof String) {
                    String[] parts = ((String) obj).split(Protocol.DELIMITER);
                    String cmd = parts[0];
                    routeCommand(cmd, parts);
                }
            }
        } catch (java.io.EOFException | java.net.SocketException e) {
            LOGGER.info("🔌 Client đã ngắt kết nối: " + socket.getInetAddress());
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "❌ Lỗi không xác định khi xử lý Client: " + socket.getInetAddress(), e);
        } finally {
            cleanup();
        }
    }

    private void routeCommand(String cmd, String[] parts) {
        switch (cmd) {
        // Auth
        case Protocol.REQ_LOGIN:
            if (parts.length >= 3)
                authHandler.handleLogin(parts[1], parts[2]);
            break;
        case Protocol.REQ_REGISTER:
            if (parts.length >= 4)
                authHandler.handleRegister(parts[1], parts[2], parts[3]);
            else if (parts.length >= 3)
                authHandler.handleRegister(parts[1], parts[2], "BIDDER");
            break;
        case Protocol.REQ_LOGOUT:
            authHandler.handleLogout();
            break;
        // Auction
        case Protocol.REQ_GET_AUCTIONS:
            auctionHandler.handleGetAuctions();
            break;
        case Protocol.REQ_CREATE_ITEM:
            if (parts.length >= 6)
                auctionHandler.handleCreateItem(parts[1], parts[2], parts[3], parts[4], parts[5]);
            break;
        case Protocol.REQ_UPDATE_ITEM:
            if (parts.length >= 7)
                auctionHandler.handleUpdateItem(parts[1], parts[2], parts[3], parts[4], parts[5], parts[6]);
            break;
        case Protocol.REQ_DELETE_ITEM:
            if (parts.length >= 2)
                auctionHandler.handleDeleteItem(parts[1]);
            break;
        case Protocol.REQ_BID:
            if (parts.length >= 3)
                auctionHandler.handleBid(parts[1], parts[2]);
            break;
        case Protocol.REQ_AUTOBID:
            if (parts.length >= 3)
                auctionHandler.handleAutoBid(parts[1], parts[2], parts.length >= 4 ? parts[3] : null);
            break;
        case Protocol.REQ_JOIN_ROOM:
            if (parts.length >= 2)
                auctionHandler.handleJoinRoom(parts[1]);
            break;
        case Protocol.REQ_LEAVE_ROOM:
            if (parts.length >= 2)
                auctionHandler.handleLeaveRoom(parts[1]);
            break;
        // Admin
        case Protocol.REQ_GET_USERS:
            adminHandler.handleGetUsers();
            break;
        case Protocol.REQ_BAN_USER:
            if (parts.length >= 3)
                adminHandler.handleBanUser(parts[1], parts[2]);
            break;
        case Protocol.REQ_KICK_USER:
            if (parts.length >= 3)
                adminHandler.handleKickUser(parts[1], parts[2]);
            break;
        case Protocol.REQ_DELETE_AUCTION:
            if (parts.length >= 2)
                adminHandler.handleDeleteAuction(parts[1]);
            break;
        case Protocol.REQ_UPDATE_USER_BALANCE:
            if (parts.length >= 3)
                adminHandler.handleUpdateUserBalance(parts[1], parts[2]);
            break;
        // Financial
        case Protocol.REQ_DEPOSIT:
            if (parts.length >= 2)
                financialHandler.handleDeposit(parts[1]);
            break;
        case Protocol.REQ_WITHDRAW:
            if (parts.length >= 2)
                financialHandler.handleWithdraw(parts[1]);
            break;
        }
    }

    private void cleanup() {
        try {
            for (String roomId : currentRoomIds) {
                server.leaveRoom(roomId, this);
            }
            currentRoomIds.clear();
            server.removeClient(this);
            if (in != null)
                in.close();
            if (out != null)
                out.close();
            if (socket != null && !socket.isClosed())
                socket.close();
        } catch (java.io.IOException e) {
            LOGGER.log(Level.FINE, "Lỗi khi đóng tài nguyên client", e);
        }
    }

    public void forceLogout(String message) {
        try {
            for (String roomId : currentRoomIds) {
                server.leaveRoom(roomId, this);
            }
            currentRoomIds.clear();
        } catch (Exception e) {
            LOGGER.warning("Lỗi khi rời phòng đấu giá lúc force logout");
        }
        loggedInUser = null;
        sendData(Protocol.BROADCAST_FORCE_LOGOUT + Protocol.DELIMITER + message);
    }

    public synchronized void sendData(Object data) {
        try {
            if (out != null && socket != null && !socket.isClosed()) {
                out.writeObject(data);
                out.reset();
                out.flush();
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Lỗi gửi dữ liệu tới Client: " + socket.getInetAddress(), e);
        }
    }
}