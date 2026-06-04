package network;

import model.auction.Auction;
import model.user.User;
import shared.Protocol;

import java.io.ObjectInputStream;
import java.util.List;
import java.util.function.Consumer;

public class ClientNetworkManager {
    private static volatile ClientNetworkManager instance;

    private final ClientConnection connection;
    private final ClientListenerRegistry listeners;
    private final ServerMessageDispatcher dispatcher;

    private ClientNetworkManager() {
        this.connection = new ClientConnection();
        this.listeners = new ClientListenerRegistry();
        this.dispatcher = new ServerMessageDispatcher(listeners);
    }

    // Đảm bảo chỉ có một instance của ClientNetworkManager tồn tại (Singleton pattern)
    public static ClientNetworkManager getInstance() {
        if (instance == null) {
            synchronized (ClientNetworkManager.class) {
                if (instance == null) {
                    instance = new ClientNetworkManager();
                }
            }
        }
        return instance;
    }

    // Kiểm tra kết nối hiện tại với server
    public synchronized boolean isConnected() {
        return connection.isConnected();
    }

    // Kết nối đến server với địa chỉ IP và cổng được cung cấp.
    public synchronized boolean connect(String ip, int port) {
        if (isConnected()) {
            return true;
        }

        if (!connection.connect(ip, port)) {
            return false;
        }

        startListeningThread(connection.getInputStream());
        return true;
    }

    // Gửi dữ liệu đến server
    public synchronized boolean sendData(Object data) {
        boolean sent = connection.sendData(data);
        if (!sent && connection.isConnected()) {
            disconnect();
        }
        return sent;
    }

    // Ngắt kết nối khỏi server
    public synchronized void disconnect() {
        connection.close();
    }

    // Đăng xuất khỏi hệ thống
    public synchronized void logout() {
        if (isConnected()) {
            sendData(Protocol.REQ_LOGOUT);
        }
        SessionManager.getInstance().clearSession();
    }

    // Đăng ký một listener cho một lệnh cụ thể
    public void registerListener(String command, Consumer<String> listener) {
        listeners.registerListener(command, listener);
    }

    // Hủy đăng ký một listener cho một lệnh cụ thể
    public void removeListener(String command, Consumer<String> listener) {
        listeners.removeListener(command, listener);
    }

    // Xóa tất cả listener cho một lệnh cụ thể
    public void clearListeners(String command) {
        listeners.clearListeners(command);
    }

    // Các phương thức tiện ích để đăng ký listener cho các loại dữ liệu cụ thể như danh sách đấu giá, danh sách người dùng và số dư
    public void addAuctionListListener(Consumer<List<Auction>> listener) {
        listeners.addAuctionListListener(listener);
    }

    public void removeAuctionListListener(Consumer<List<Auction>> listener) {
        listeners.removeAuctionListListener(listener);
    }

    public void clearAuctionListListeners() {
        listeners.clearAuctionListListeners();
    }

    public void addUserListListener(Consumer<List<User>> listener) {
        listeners.addUserListListener(listener);
    }

    public void removeUserListListener(Consumer<List<User>> listener) {
        listeners.removeUserListListener(listener);
    }

    public void clearUserListListeners() {
        listeners.clearUserListListeners();
    }

    public void addBalanceListener(Consumer<Double> listener) {
        listeners.addBalanceListener(listener);
    }

    public void removeBalanceListener(Consumer<Double> listener) {
        listeners.removeBalanceListener(listener);
    }

    private void startListeningThread(ObjectInputStream in) {
        Thread listenerThread = new Thread(
                new ServerListenerTask(in, dispatcher, this::disconnect),
                "client-server-listener"
        );
        listenerThread.setDaemon(true);
        listenerThread.start();
    }
}
