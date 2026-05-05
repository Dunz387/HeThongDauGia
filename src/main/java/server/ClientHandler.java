package server;

import exception.AuctionClosedException;
import exception.InvalidBidException;
import model.auction.Auction;
import model.user.Bidder;
import model.user.User; // Đã thêm import
import service.AuctionManager;
import shared.Protocol; // Đã thêm import

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class ClientHandler implements Runnable {
    private Socket socket;
    private AuctionServer server;
    private Auction currentAuction;
    private AuctionManager manager;

    // ĐỔI TỪ: Bidder myProfile SANG User loggedInUser để quản lý Xác thực[cite: 21]
    private User loggedInUser = null;

    private ObjectOutputStream out;
    private ObjectInputStream in;

    public ClientHandler(Socket socket, AuctionServer server, Auction auction, AuctionManager manager) {
        this.socket = socket;
        this.server = server;
        this.currentAuction = auction;
        this.manager = manager;
    }

    @Override
    public void run() {
        try {
            // Khởi tạo luồng Object[cite: 21]
            out = new ObjectOutputStream(socket.getOutputStream());
            out.flush();
            in = new ObjectInputStream(socket.getInputStream());

            // --- ĐÃ XÓA BỎ: Logic tự động tạo profile ảo[cite: 21] ---
            // --- THÊM MỚI: Bộ định tuyến Router lắng nghe lệnh Protocol ---

            Object inputObj;
            while ((inputObj = in.readObject()) != null) {
                if (inputObj instanceof String) {
                    String rawData = ((String) inputObj).trim();
                    if (rawData.equalsIgnoreCase("exit")) break; //[cite: 21]

                    // Cắt chuỗi lệnh gửi lên
                    String[] parts = rawData.split(Protocol.SEPARATOR);
                    String command = parts[0];

                    switch (command) {
                        case Protocol.REQ_LOGIN:
                            if (parts.length >= 3) handleLogin(parts[1], parts[2]);
                            break;

                        case Protocol.REQ_REGISTER:
                            if (parts.length >= 3) handleRegister(parts[1], parts[2]);
                            break;

                        case Protocol.REQ_BID:
                            // Kiểm tra bảo mật: Chưa đăng nhập thì không cho đấu giá
                            if (loggedInUser != null && loggedInUser instanceof Bidder) {
                                handleBid(parts[1]);
                            } else {
                                sendData(Protocol.REQ_LOGIN + Protocol.DELIMITER + Protocol.RES_FAIL + Protocol.DELIMITER + "Vui lòng đăng nhập với tư cách người mua trước khi đấu giá!");
                            }
                            break;

                        default:
                            System.out.println("Lệnh không xác định: " + command);
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("Client ngắt kết nối: " + e.getMessage());
        } finally {
            try { socket.close(); } catch (Exception e) {}
            server.removeClient(this);
        }
    }

    // --- CÁC HÀM XỬ LÝ LOGIC ---

    private void handleLogin(String username, String password) {
        // Lưu ý: Bạn cần viết hàm authenticateUser() trong class AuctionManager
        User user = manager.authenticateUser(username, password);

        if (user != null) {
            this.loggedInUser = user; // Gắn thẻ luồng này đã thuộc về user
            // Trả về: LOGIN|SUCCESS|BIDDER
            sendData(Protocol.REQ_LOGIN + Protocol.DELIMITER + Protocol.RES_SUCCESS + Protocol.DELIMITER + user.getRole());
        } else {
            sendData(Protocol.REQ_LOGIN + Protocol.DELIMITER + Protocol.RES_FAIL + Protocol.DELIMITER + "Sai tên đăng nhập hoặc mật khẩu");
        }
    }

    private void handleRegister(String username, String password) {
        // Lưu ý: Bạn cần viết hàm registerNewUser() trong class AuctionManager
        boolean success = manager.registerNewUser(username, password);

        if (success) {
            sendData(Protocol.REQ_REGISTER + Protocol.DELIMITER + Protocol.RES_SUCCESS);
        } else {
            sendData(Protocol.REQ_REGISTER + Protocol.DELIMITER + Protocol.RES_FAIL + Protocol.DELIMITER + "Tài khoản đã tồn tại");
        }
    }

    private void handleBid(String amountStr) {
        try {
            double amount = Double.parseDouble(amountStr);
            String result = manager.processBid((Bidder) loggedInUser, currentAuction, amount); //[cite: 21]
            sendData(">> Kết quả: " + result); //[cite: 21]
        } catch (NumberFormatException e) {
            sendData("LỖI: Vui lòng chỉ nhập con số."); //[cite: 21]
        }
    }

    // Hàm nhận mọi loại Object giữ nguyên[cite: 21]
    public void sendData(Object data) {
        try {
            if (out != null) {
                out.writeObject(data);
                out.reset();
                out.flush();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}