package server.command;

import server.ClientHandler;
import shared.Protocol;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Registry quản lý và ánh xạ các loại gói tin tới các đối tượng Command.
 */
public class CommandRegistry {
    private static final Logger LOGGER = Logger.getLogger(CommandRegistry.class.getName());
    private final Map<String, Command> commands = new HashMap<>();

    public CommandRegistry() {
        initCommands();
    }

    private void initCommands() {
        // --- Auth Commands ---
        commands.put(Protocol.REQ_LOGIN, (parts, client) -> {
            if (parts.length >= 3) client.getAuthHandler().handleLogin(parts[1], parts[2]);
        });
        commands.put(Protocol.REQ_REGISTER, (parts, client) -> {
            if (parts.length >= 4) client.getAuthHandler().handleRegister(parts[1], parts[2], parts[3]);
            else if (parts.length >= 3) client.getAuthHandler().handleRegister(parts[1], parts[2], "BIDDER");
        });
        commands.put(Protocol.REQ_LOGOUT, (parts, client) -> client.getAuthHandler().handleLogout());

        // --- Auction Commands ---
        commands.put(Protocol.REQ_GET_AUCTIONS, (parts, client) -> client.getAuctionHandler().handleGetAuctions());
        commands.put(Protocol.REQ_CREATE_ITEM, (parts, client) -> {
            if (parts.length >= 6) client.getAuctionHandler().handleCreateItem(parts[1], parts[2], parts[3], parts[4], parts[5]);
        });
        commands.put(Protocol.REQ_UPDATE_ITEM, (parts, client) -> {
            if (parts.length >= 7) client.getAuctionHandler().handleUpdateItem(parts[1], parts[2], parts[3], parts[4], parts[5], parts[6]);
        });
        commands.put(Protocol.REQ_DELETE_ITEM, (parts, client) -> {
            if (parts.length >= 2) client.getAuctionHandler().handleDeleteItem(parts[1]);
        });
        commands.put(Protocol.REQ_BID, (parts, client) -> {
            if (parts.length >= 3) client.getAuctionHandler().handleBid(parts[1], parts[2]);
        });
        commands.put(Protocol.REQ_AUTOBID, (parts, client) -> {
            if (parts.length >= 3) client.getAuctionHandler().handleAutoBid(parts[1], parts[2], parts.length >= 4 ? parts[3] : null);
        });
        commands.put(Protocol.REQ_JOIN_ROOM, (parts, client) -> {
            if (parts.length >= 2) client.getAuctionHandler().handleJoinRoom(parts[1]);
        });
        commands.put(Protocol.REQ_LEAVE_ROOM, (parts, client) -> {
            if (parts.length >= 2) client.getAuctionHandler().handleLeaveRoom(parts[1]);
        });

        // --- Admin Commands ---
        commands.put(Protocol.REQ_GET_USERS, (parts, client) -> client.getAdminHandler().handleGetUsers());
        commands.put(Protocol.REQ_BAN_USER, (parts, client) -> {
            if (parts.length >= 3) client.getAdminHandler().handleBanUser(parts[1], parts[2]);
        });
        commands.put(Protocol.REQ_KICK_USER, (parts, client) -> {
            if (parts.length >= 3) client.getAdminHandler().handleKickUser(parts[1], parts[2]);
        });
        commands.put(Protocol.REQ_DELETE_AUCTION, (parts, client) -> {
            if (parts.length >= 2) client.getAdminHandler().handleDeleteAuction(parts[1]);
        });
        commands.put(Protocol.REQ_UPDATE_USER_BALANCE, (parts, client) -> {
            if (parts.length >= 3) client.getAdminHandler().handleUpdateUserBalance(parts[1], parts[2]);
        });

        // --- Financial Commands ---
        commands.put(Protocol.REQ_DEPOSIT, (parts, client) -> {
            if (parts.length >= 2) client.getFinancialHandler().handleDeposit(parts[1]);
        });
        commands.put(Protocol.REQ_WITHDRAW, (parts, client) -> {
            if (parts.length >= 2) client.getFinancialHandler().handleWithdraw(parts[1]);
        });
    }

    public void executeCommand(String cmd, String[] parts, ClientHandler client) {
        Command command = commands.get(cmd);
        if (command != null) {
            command.execute(parts, client);
        } else {
            LOGGER.warning("⚠️ Không tìm thấy Command cho giao thức: " + cmd);
        }
    }
}
