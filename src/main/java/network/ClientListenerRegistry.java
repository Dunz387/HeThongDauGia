package network;

import model.auction.Auction;
import model.user.User;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

class ClientListenerRegistry {
    private static final Logger LOGGER = Logger.getLogger(ClientListenerRegistry.class.getName());

    // Lưu trữ các listener theo command, sử dụng ConcurrentHashMap để đảm bảo thread-safety
    private final Map<String, List<Consumer<String>>> messageListeners = new ConcurrentHashMap<>();
    // Các listener đặc biệt cho danh sách đấu giá, danh sách người dùng và cập nhật số dư
    private final List<Consumer<List<Auction>>> auctionListListeners = new CopyOnWriteArrayList<>();
    // Các listener đặc biệt cho danh sách người dùng
    private final List<Consumer<List<User>>> userListListeners = new CopyOnWriteArrayList<>();
    // Các listener đặc biệt cho cập nhật số dư
    private final List<Consumer<Double>> balanceListeners = new CopyOnWriteArrayList<>();

    // Quản lý đăng kí listener
    void registerListener(String command, Consumer<String> listener) {
        messageListeners.computeIfAbsent(command, k -> new CopyOnWriteArrayList<>()).add(listener);
    }

    // Quản lý hủy đăng kí listener
    void removeListener(String command, Consumer<String> listener) {
        List<Consumer<String>> listeners = messageListeners.get(command);
        if (listeners != null) {
            listeners.remove(listener);
        }
    }

    // Quản lý hủy đăng kí tất cả listener
    void clearListeners(String command) {
        messageListeners.remove(command);
    }

    // Quản lý thêm listener cho danh sách đấu giá
    void addAuctionListListener(Consumer<List<Auction>> listener) {
        auctionListListeners.add(listener);
    }

    // Quản lý hủy listener cho danh sách đấu giá
    void removeAuctionListListener(Consumer<List<Auction>> listener) {
        auctionListListeners.remove(listener);
    }

    // Quản lý hủy tất cả listener cho danh sách đấu giá
    void clearAuctionListListeners() {
        auctionListListeners.clear();
    }

    // Quản lý thêm listener cho danh sách người dùng
    void addUserListListener(Consumer<List<User>> listener) {
        userListListeners.add(listener);
    }

    // Quản lý hủy listener cho danh sách người dùng
    void removeUserListListener(Consumer<List<User>> listener) {
        userListListeners.remove(listener);
    }

    // Quản lý hủy tất cả listener cho danh sách người dùng
    void clearUserListListeners() {
        userListListeners.clear();
    }

    // Quản lý thêm listener cho cập nhật số dư
    void addBalanceListener(Consumer<Double> listener) {
        balanceListeners.add(listener);
    }

    // Quản lý hủy listener cho cập nhật số dư
    void removeBalanceListener(Consumer<Double> listener) {
        balanceListeners.remove(listener);
    }

    // Quản lý hủy tất cả listener cho cập nhật số dư
    void clearBalanceListeners() {
        balanceListeners.clear();
    }

    // Quản lý thông báo cho tất cả listener
    void notifyMessage(String command, String message) {
        List<Consumer<String>> listeners = messageListeners.get(command);
        if (listeners == null) {
            return;
        }

        for (Consumer<String> listener : listeners) {
            try {
                listener.accept(message);
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Error in listener [" + command + "]", e);
            }
        }
    }

    // Quản lý thông báo cho phiên đấu giá
    void notifyAuctionList(List<Auction> auctionList) {
        for (Consumer<List<Auction>> listener : auctionListListeners) {
            try {
                listener.accept(auctionList);
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Error in auctionListListener", e);
            }
        }
    }

    // Quản lý thông báo cho user
    void notifyUserList(List<User> userList) {
        for (Consumer<List<User>> listener : userListListeners) {
            try {
                listener.accept(userList);
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Error in userListListener", e);
            }
        }
    }

    // Quản lý thông báo cho cập nhật số dư
    void notifyBalance(double newBalance) {
        for (Consumer<Double> listener : balanceListeners) {
            try {
                listener.accept(newBalance);
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Error in balanceListener", e);
            }
        }
    }
}
