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

public class ClientNetworkManager {
    private static volatile ClientNetworkManager instance; // T18: volatile cho thread-safe
    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;

    // T7: Chuyển từ single listener sang DANH SÁCH listeners để nhiều Controller cùng nhận broadcast
    private Map<String, List<Consumer<String>>> messageListeners = new ConcurrentHashMap<>();
    private List<Consumer<List<Auction>>> auctionListListeners = new CopyOnWriteArrayList<>();
    private List<Consumer<List<User>>> userListListeners = new CopyOnWriteArrayList<>();

    // Header đang chờ để phân biệt loại List khi nhận từ Server
    private volatile String pendingListHeader = null;

    private ClientNetworkManager() {}

    // T18: Thread-safe Singleton với double-checked locking
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

    public boolean connect(String ip, int port) {
        try {
            socket = new Socket(ip, port);
            out = new ObjectOutputStream(socket.getOutputStream());
            out.flush();
            in = new ObjectInputStream(socket.getInputStream());

            startListeningThread();
            System.out.println("✅ Đã kết nối thành công tới Server!");
            return true;
        } catch (Exception e) {
            System.err.println("❌ Lỗi kết nối tới Server: " + e.getMessage()); // T12: log error
            return false;
        }
    }

    public void sendData(Object data) {
        try {
            if (out != null) {
                out.writeObject(data);
                out.flush();
            }
        } catch (Exception e) {
            System.err.println("❌ Lỗi gửi dữ liệu: " + e.getMessage()); // T12: log error
        }
    }

    // T7: ĐĂNG KÝ CALLBACK CHO STRING — Hỗ trợ NHIỀU listener cho cùng 1 command
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

    // T7: ĐĂNG KÝ CALLBACK CHO LIST OBJECT — Hỗ trợ NHIỀU listener
    public void addAuctionListListener(Consumer<List<Auction>> listener) {
        auctionListListeners.add(listener);
    }

    public void removeAuctionListListener(Consumer<List<Auction>> listener) {
        auctionListListeners.remove(listener);
    }

    /**
     * @deprecated Dùng {@link #addAuctionListListener(Consumer)} thay thế.
     * Phương thức này giữ lại để tương thích ngược, nhưng sẽ XÓA hết listener cũ trước khi thêm mới.
     */
    @Deprecated
    public void setAuctionListListener(Consumer<List<Auction>> listener) {
        auctionListListeners.clear();
        auctionListListeners.add(listener);
    }

    // T7: ĐĂNG KÝ CALLBACK CHO LIST<USER> — Hỗ trợ NHIỀU listener
    public void addUserListListener(Consumer<List<User>> listener) {
        userListListeners.add(listener);
    }

    public void removeUserListListener(Consumer<List<User>> listener) {
        userListListeners.remove(listener);
    }

    /**
     * @deprecated Dùng {@link #addUserListListener(Consumer)} thay thế.
     */
    @Deprecated
    public void setUserListListener(Consumer<List<User>> listener) {
        userListListeners.clear();
        userListListeners.add(listener);
    }

    @SuppressWarnings("unchecked")
    private void startListeningThread() {
        Thread listenerThread = new Thread(() -> {
            try {
                Object serverData;
                while ((serverData = in.readObject()) != null) {
                    if (serverData instanceof String) {
                        String message = (String) serverData;
                        String[] parts = message.split(Protocol.SEPARATOR);
                        String command = parts[0];

                        // Nếu là header báo hiệu danh sách sắp đến, lưu lại để phân loại
                        if (command.equals(Protocol.RES_AUCTION_LIST) || command.equals(Protocol.RES_USER_LIST)) {
                            pendingListHeader = command;
                        }

                        // T7: Gọi TẤT CẢ listener đã đăng ký cho command này
                        List<Consumer<String>> listeners = messageListeners.get(command);
                        if (listeners != null) {
                            for (Consumer<String> listener : listeners) {
                                try {
                                    listener.accept(message);
                                } catch (Exception e) {
                                    System.err.println("❌ Lỗi trong listener [" + command + "]: " + e.getMessage()); // T12
                                }
                            }
                        }
                    } else if (serverData instanceof List) {
                        // Phân loại List dựa trên header đã nhận trước đó
                        String header = pendingListHeader;
                        pendingListHeader = null; // Reset ngay sau khi dùng

                        if (Protocol.RES_USER_LIST.equals(header)) {
                            // T7: Thông báo TẤT CẢ user list listener
                            List<User> userList = (List<User>) serverData;
                            for (Consumer<List<User>> listener : userListListeners) {
                                try {
                                    listener.accept(userList);
                                } catch (Exception e) {
                                    System.err.println("❌ Lỗi trong userListListener: " + e.getMessage()); // T12
                                }
                            }
                        } else {
                            // T7: Thông báo TẤT CẢ auction list listener
                            List<Auction> auctionList = (List<Auction>) serverData;
                            for (Consumer<List<Auction>> listener : auctionListListeners) {
                                try {
                                    listener.accept(auctionList);
                                } catch (Exception e) {
                                    System.err.println("❌ Lỗi trong auctionListListener: " + e.getMessage()); // T12
                                }
                            }
                        }
                    }
                }
            } catch (Exception e) {
                System.err.println("❌ Mất kết nối tới Server: " + e.getMessage()); // T12: log chi tiết
            }
        });
        listenerThread.setDaemon(true);
        listenerThread.start();
    }
}