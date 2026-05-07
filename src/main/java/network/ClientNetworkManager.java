package network;

import shared.Protocol;
import model.auction.Auction;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class ClientNetworkManager {
    private static ClientNetworkManager instance;
    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;

    // BẢN ĐỒ DANH BẠ: Lưu trữ các hàm Callback (Hành động sẽ thực hiện khi nhận được lệnh tương ứng)
    private Map<String, Consumer<String>> messageListeners = new ConcurrentHashMap<>();
    private Consumer<List<Auction>> auctionListListener = null;

    private ClientNetworkManager() {}

    public static ClientNetworkManager getInstance() {
        if (instance == null) instance = new ClientNetworkManager();
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
            e.printStackTrace();
        }
    }

    // ĐĂNG KÝ CALLBACK CHO STRING
    public void registerListener(String command, Consumer<String> listener) {
        messageListeners.put(command, listener);
    }

    // ĐĂNG KÝ CALLBACK CHO LIST OBJECT (Dành cho danh sách đấu giá)
    public void setAuctionListListener(Consumer<List<Auction>> listener) {
        this.auctionListListener = listener;
    }

    private void startListeningThread() {
        Thread listenerThread = new Thread(() -> {
            try {
                Object serverData;
                while ((serverData = in.readObject()) != null) {
                    if (serverData instanceof String) {
                        String message = (String) serverData;
                        String[] parts = message.split(Protocol.SEPARATOR);
                        String command = parts[0];

                        // Tra cứu danh bạ, nếu có Controller nào đang chờ lệnh này thì gọi nó
                        if (messageListeners.containsKey(command)) {
                            messageListeners.get(command).accept(message);
                        }
                    } else if (serverData instanceof List) {
                        if (auctionListListener != null) {
                            auctionListListener.accept((List<Auction>) serverData);
                        }
                    }
                }
            } catch (Exception e) {
                System.out.println("❌ Mất kết nối tới Server.");
            }
        });
        listenerThread.setDaemon(true);
        listenerThread.start();
    }
}