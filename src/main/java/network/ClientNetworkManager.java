package network;

import shared.Protocol;
import model.auction.Auction;
import model.user.User;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ClientNetworkManager {
    private static final Logger LOGGER = Logger.getLogger(ClientNetworkManager.class.getName());
    private static volatile ClientNetworkManager instance; // T18: volatile cho thread-safe
    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;

    // Danh sách các listener xử lý tin nhắn text từ Server
    private Map<String, List<Consumer<String>>> messageListeners = new ConcurrentHashMap<>();
    private List<Consumer<List<Auction>>> auctionListListeners = new CopyOnWriteArrayList<>();
    private List<Consumer<List<User>>> userListListeners = new CopyOnWriteArrayList<>();
    private List<Consumer<Double>> balanceListeners = new CopyOnWriteArrayList<>();

    // Lưu tạm cờ (header) để phân loại danh sách nhận từ Server
    private volatile String pendingListHeader = null;

    private ClientNetworkManager() {}

    // Khởi tạo Singleton an toàn (Thread-safe)
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

    public synchronized boolean isConnected() {
        return socket != null && !socket.isClosed() && socket.isConnected();
    }

    public synchronized boolean connect(String ip, int port) {
        if (isConnected()) {
            LOGGER.info("ℹ️ Đã có kết nối sẵn sàng.");
            return true;
        }
        try {
            socket = new Socket(ip, port);
            socket.setSoTimeout(0); // Không timeout cho việc đọc
            out = new ObjectOutputStream(socket.getOutputStream());
            out.flush();
            in = new ObjectInputStream(socket.getInputStream());

            startListeningThread();
            LOGGER.info("✅ Đã kết nối thành công tới Server!");
            return true;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "❌ Lỗi kết nối tới Server: " + e.getMessage());
            return false;
        }
    }

    public synchronized boolean sendData(Object data) {
        try {
            if (out != null && socket != null && !socket.isClosed()) {
                out.writeObject(data);
                out.reset(); // Xóa cache để tránh lỗi tham chiếu và rò rỉ bộ nhớ
                out.flush();
                return true;
            } else {
                LOGGER.warning("⚠️ Không thể gửi dữ liệu: Chưa kết nối tới Server.");
                return false;
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "❌ Lỗi gửi dữ liệu", e);
            disconnect(); // Ngắt kết nối nếu xảy ra lỗi nghiêm trọng
            return false;
        }
    }

    public synchronized void disconnect() {
        try {
            if (out != null) out.close();
            if (in != null) in.close();
            if (socket != null) socket.close();
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Lỗi khi đóng kết nối", e);
        } finally {
            out = null;
            in = null;
            socket = null;
            LOGGER.info("🔌 Đã ngắt kết nối tới Server.");
        }
    }

    public synchronized void logout() {
        if (isConnected()) {
            sendData(Protocol.REQ_LOGOUT);
        }
        SessionManager.getInstance().clearSession();
    }

    // Đăng ký listener để xử lý các lệnh dạng chuỗi
    public void registerListener(String command, Consumer<String> listener) {
        messageListeners.computeIfAbsent(command, k -> new CopyOnWriteArrayList<>()).add(listener);
    }

    /**
     * Hủy đăng ký listener cho một command cụ thể.
     * Quan trọng khi Controller bị dispose để tránh memory leak.
     */
    public void removeListener(String command, Consumer<String> listener) {
        List<Consumer<String>> listeners = messageListeners.get(command);
        if (listeners != null) {
            listeners.remove(listener);
        }
    }

    /**
     * Xóa TẤT CẢ listener cho một command. Dùng khi cần reset hoàn toàn.
     */
    public void clearListeners(String command) {
        messageListeners.remove(command);
    }

    // Đăng ký listener để nhận danh sách phiên đấu giá
    public void addAuctionListListener(Consumer<List<Auction>> listener) {
        auctionListListeners.add(listener);
    }

    public void removeAuctionListListener(Consumer<List<Auction>> listener) {
        auctionListListeners.remove(listener);
    }

    public void clearAuctionListListeners() {
        auctionListListeners.clear();
    }


    // Đăng ký listener để nhận danh sách người dùng
    public void addUserListListener(Consumer<List<User>> listener) {
        userListListeners.add(listener);
    }

    public void removeUserListListener(Consumer<List<User>> listener) {
        userListListeners.remove(listener);
    }

    public void clearUserListListeners() {
        userListListeners.clear();
    }

    public void addBalanceListener(Consumer<Double> listener) {
        balanceListeners.add(listener);
    }

    public void removeBalanceListener(Consumer<Double> listener) {
        balanceListeners.remove(listener);
    }

    @SuppressWarnings("unchecked")
    private void startListeningThread() {
        Thread listenerThread = new Thread(() -> {
            try {
                Object serverData;
                while ((serverData = in.readObject()) != null) {
                    if (serverData instanceof String) {
                        String message = (String) serverData;
                        String[] parts = message.split(Protocol.DELIMITER);
                        String command = parts[0];

                        // Nếu là header báo hiệu danh sách sắp đến, lưu lại để phân loại
                        if (command.equals(Protocol.RES_AUCTION_LIST) || command.equals(Protocol.RES_USER_LIST)) {
                            pendingListHeader = command;
                        }

                        // Kích hoạt tất cả listener tương ứng với lệnh
                        List<Consumer<String>> listeners = messageListeners.get(command);
                        if (listeners != null) {
                            for (Consumer<String> listener : listeners) {
                                try {
                                    listener.accept(message);
                                } catch (Exception e) {
                                    LOGGER.log(Level.WARNING, "❌ Lỗi trong listener [" + command + "]", e);
                                }
                            }
                        }

                        // Xử lý lệnh cập nhật số dư từ Server
                        if (command.equals(Protocol.RES_UPDATE_BALANCE) && parts.length >= 2) {
                            double newBalance = Double.parseDouble(parts[1]);
                            SessionManager.getInstance().updateBalance(newBalance);
                            for (Consumer<Double> listener : balanceListeners) {
                                listener.accept(newBalance);
                            }
                        }
                        
                        // Xử lý lệnh FORCE_LOGOUT qua listener pattern (không gọi thẳng View)
                        // Logic UI sẽ được đăng ký bởi LoginController thông qua registerListener()
                    } else if (serverData instanceof List) {
                        // Phân loại List dựa trên header đã nhận trước đó
                        String header = pendingListHeader;
                        pendingListHeader = null; // Reset ngay sau khi dùng

                        if (Protocol.RES_USER_LIST.equals(header)) {
                            // Phát danh sách người dùng tới các listener
                            List<User> userList = (List<User>) serverData;
                            for (Consumer<List<User>> listener : userListListeners) {
                                try {
                                    listener.accept(userList);
                                } catch (Exception e) {
                                    LOGGER.log(Level.WARNING, "❌ Lỗi trong userListListener", e);
                                }
                            }
                        } else {
                            // Phát danh sách phiên đấu giá tới các listener
                            List<Auction> auctionList = (List<Auction>) serverData;
                            for (Consumer<List<Auction>> listener : auctionListListeners) {
                                try {
                                    listener.accept(auctionList);
                                } catch (Exception e) {
                                    LOGGER.log(Level.WARNING, "❌ Lỗi trong auctionListListener", e);
                                }
                            }
                        }
                    }
                }
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "❌ Mất kết nối tới Server", e);
                disconnect();
            }
        });
        listenerThread.setDaemon(true);
        listenerThread.start();
    }
}