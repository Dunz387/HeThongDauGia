package network;

import javafx.application.Platform;
import shared.Protocol;
import model.auction.Auction; // Nhớ import cái này nhé!

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.List; // Import List

public class ClientNetworkManager {
    private static ClientNetworkManager instance;
    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;

    private String lastResponse = null;

    // THÊM MỚI: Biến để hứng danh sách đấu giá
    private List<Auction> lastAuctionList = null;

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

    public void sendData(Object data) { // Đổi tham số thành Object để gửi được nhiều kiểu
        try {
            if (out != null) {
                out.writeObject(data);
                out.reset();
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

                    // NẾU LÀ CHUỖI LỆNH (String)
                    if (inputObj instanceof String) {
                        String message = (String) inputObj;
                        System.out.println("[Server trả về Lệnh]: " + message);
                        processIncomingMessage(message);
                    }
                    // NẾU LÀ DANH SÁCH (List) -> THÊM MỚI ĐOẠN NÀY
                    else if (inputObj instanceof List) {
                        System.out.println("[Server trả về Data]: Nhận được một List!");
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
            case Protocol.RES_AUCTION_LIST: // Báo hiệu chuẩn bị nhận List
                this.lastResponse = message;
                break;
        }
    }

    public String getLastResponse() {
        String temp = lastResponse;
        lastResponse = null;
        return temp;
    }

    // THÊM MỚI: Hàm cho Controller lấy danh sách
    public List<Auction> getLastAuctionList() {
        List<Auction> temp = lastAuctionList;
        lastAuctionList = null;
        return temp;
    }
}