package network;

import shared.Protocol;
import model.auction.Auction;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.List;

public class ClientNetworkManager {
    private static ClientNetworkManager instance;
    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;

    // SỬA LỖI: Thêm volatile để đồng bộ hóa giữa các luồng (Thread-safe)[cite: 15]
    private volatile String lastResponse = null;
    private volatile List<Auction> lastAuctionList = null;

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
            System.out.println("✅ Đã kết nối thành công đến Server!");
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public void sendData(Object data) {
        try {
            if (out != null) {
                out.writeObject(data);
                out.reset(); // Xóa cache để gửi dữ liệu mới nhất[cite: 15]
                out.flush();
            }
        } catch (Exception e) {
            System.out.println("Lỗi khi gửi dữ liệu: " + e.getMessage());
        }
    }

    private void startListeningThread() {
        Thread listenerThread = new Thread(() -> {
            try {
                Object inputObj;
                while ((inputObj = in.readObject()) != null) {
                    if (inputObj instanceof String) {
                        String message = (String) inputObj;
                        System.out.println("[Server trả về Lệnh]: " + message);
                        processIncomingMessage(message);
                    }
                    else if (inputObj instanceof List) {
                        // Nhận danh sách Auction từ Server[cite: 15]
                        System.out.println("[Server trả về Data]: Đã nhận List với " + ((List) inputObj).size() + " phần tử.");
                        this.lastAuctionList = (List<Auction>) inputObj;
                    }
                }
            } catch (Exception e) {
                System.out.println("Mất kết nối với Server.");
            }
        });
        listenerThread.setDaemon(true);
        listenerThread.start();
    }

    private void processIncomingMessage(String message) {
        String[] parts = message.split(Protocol.SEPARATOR);
        String command = parts[0];

        switch (command) {
            case Protocol.REQ_LOGIN:
            case Protocol.REQ_REGISTER:
            case Protocol.REQ_CREATE_ITEM: // Nhận phản hồi khi tạo hàng thành công[cite: 15]
            case Protocol.RES_AUCTION_LIST:
                this.lastResponse = message;
                break;
        }
    }

    public String getLastResponse() {
        String temp = lastResponse;
        lastResponse = null;
        return temp;
    }

    public List<Auction> getLastAuctionList() {
        List<Auction> temp = lastAuctionList;
        lastAuctionList = null; // Xóa sau khi lấy để tránh lấy trùng dữ liệu cũ[cite: 15]
        return temp;
    }
}