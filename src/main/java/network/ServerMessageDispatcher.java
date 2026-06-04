package network;

import model.auction.Auction;
import model.user.User;
import shared.Protocol;

import java.util.List;

class ServerMessageDispatcher {
    private final ClientListenerRegistry listeners; // Lưu trữ các listener đã đăng kí để phân phối thông điệp từ server
    private volatile String pendingListHeader; // Biến tạm để lưu trữ header của danh sách đang chờ nhận (dùng cho RES_AUCTION_LIST và RES_USER_LIST)

    ServerMessageDispatcher(ClientListenerRegistry listeners) {
        this.listeners = listeners;
    }

    // Phương thức chính để phân phối dữ liệu từ server đến các listener đã đăng kí
    @SuppressWarnings("unchecked")
    void dispatch(Object serverData) {
        if (serverData instanceof String) {
            dispatchMessage((String) serverData);
        } else if (serverData instanceof List) {
            dispatchList((List<?>) serverData);
        }
    }

    // Phân phối thông điệp dạng String đến các listener đã đăng kí theo command
    private void dispatchMessage(String message) {
        String[] parts = message.split(Protocol.DELIMITER);
        String command = parts[0];

        if (Protocol.RES_AUCTION_LIST.equals(command) || Protocol.RES_USER_LIST.equals(command)) {
            pendingListHeader = command;
        }

        listeners.notifyMessage(command, message);
        dispatchBalanceUpdate(command, parts);
    }

    // Phân phối cập nhật số dư nếu command là RES_UPDATE_BALANCE
    private void dispatchBalanceUpdate(String command, String[] parts) {
        if (!Protocol.RES_UPDATE_BALANCE.equals(command) || parts.length < 2) {
            return;
        }

        double newBalance = Double.parseDouble(parts[1]);
        SessionManager.getInstance().updateBalance(newBalance);
        listeners.notifyBalance(newBalance);
    }

    // Phân phối danh sách đấu giá hoặc danh sách người dùng đến các listener đã đăng kí
    @SuppressWarnings("unchecked")
    private void dispatchList(List<?> serverList) {
        String header = pendingListHeader;
        pendingListHeader = null;

        if (Protocol.RES_USER_LIST.equals(header)) {
            listeners.notifyUserList((List<User>) serverList);
        } else {
            listeners.notifyAuctionList((List<Auction>) serverList);
        }
    }
}
