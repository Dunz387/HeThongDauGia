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

    // --- CÁC HÒM THƯ RIÊNG BIỆT ĐỂ KHÔNG BỊ GHI ĐÈ LUỒNG ---
    private volatile String lastAuthResponse = null;       // Dành cho Đăng nhập / Đăng ký
    private volatile String lastCreateItemResponse = null; // Dành riêng cho Đăng bán
    private volatile String lastBidResponse = null;        // Dành riêng cho Đặt giá (Bid)
    private volatile List<Auction> lastAuctionList = null; // Dành cho Cập nhật danh sách
    // -------------------------------------------------------

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
                out.flush();
            }
        } catch (Exception e) {
            System.out.println("Lỗi gửi dữ liệu: " + e.getMessage());
        }
    }

    private void startListeningThread() {
        Thread listenerThread = new Thread(() -> {
            try {
                Object inputObj;
                while ((inputObj = in.readObject()) != null) {
                    if (inputObj instanceof String) {
                        String message = (String) inputObj;
                        processIncomingMessage(message);
                    }
                    else if (inputObj instanceof List) {
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

        // PHÂN LOẠI TIN NHẮN VÀO ĐÚNG HÒM THƯ
        switch (command) {
            case Protocol.REQ_LOGIN:
            case Protocol.REQ_REGISTER:
                this.lastAuthResponse = message;
                break;
            case Protocol.REQ_CREATE_ITEM:
                this.lastCreateItemResponse = message; // Hứng kết quả Đăng bán
                break;
            case Protocol.REQ_BID:
                this.lastBidResponse = message;        // Hứng kết quả Đặt giá
                break;
        }
    }

    // --- CÁC HÀM LẤY KẾT QUẢ CHO GIAO DIỆN (UI) ---
    public String getLastAuthResponse() {
        String temp = lastAuthResponse;
        lastAuthResponse = null;
        return temp;
    }

    public String getLastCreateItemResponse() {
        String temp = lastCreateItemResponse;
        lastCreateItemResponse = null;
        return temp;
    }

    public String getLastBidResponse() {
        String temp = lastBidResponse;
        lastBidResponse = null;
        return temp;
    }

    public List<Auction> getLastAuctionList() {
        List<Auction> temp = lastAuctionList;
        lastAuctionList = null;
        return temp;
    }

    // Giữ lại hàm này tạm thời để các màn hình cũ chưa sửa không bị lỗi đỏ code
    public String getLastResponse() {
        return getLastAuthResponse();
    }
}