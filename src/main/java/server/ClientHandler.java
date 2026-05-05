package server;

import exception.AuctionClosedException;
import exception.InvalidBidException;
import model.auction.Auction;
import model.user.Bidder;
import model.user.User;
import service.AuctionManager;
import shared.Protocol;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.List; // Đã thêm import

public class ClientHandler implements Runnable {
    private Socket socket;
    private AuctionServer server;
    private Auction currentAuction;
    private AuctionManager manager;

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
            out = new ObjectOutputStream(socket.getOutputStream());
            out.flush();
            in = new ObjectInputStream(socket.getInputStream());

            Object inputObj;
            while ((inputObj = in.readObject()) != null) {
                if (inputObj instanceof String) {
                    String rawData = ((String) inputObj).trim();
                    if (rawData.equalsIgnoreCase("exit")) break;

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
                            if (loggedInUser != null && loggedInUser instanceof Bidder) {
                                handleBid(parts[1]);
                            } else {
                                sendData(Protocol.REQ_LOGIN + Protocol.DELIMITER + Protocol.RES_FAIL + Protocol.DELIMITER + "Vui lòng đăng nhập với tư cách người mua trước khi đấu giá!");
                            }
                            break;

                        // THÊM MỚI: Xử lý yêu cầu lấy danh sách đấu giá
                        case Protocol.REQ_GET_AUCTIONS:
                            handleGetAuctions();
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
        User user = manager.authenticateUser(username, password);

        if (user != null) {
            this.loggedInUser = user;
            sendData(Protocol.REQ_LOGIN + Protocol.DELIMITER + Protocol.RES_SUCCESS + Protocol.DELIMITER + user.getRole());
        } else {
            sendData(Protocol.REQ_LOGIN + Protocol.DELIMITER + Protocol.RES_FAIL + Protocol.DELIMITER + "Sai tên đăng nhập hoặc mật khẩu");
        }
    }

    private void handleRegister(String username, String password) {
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
            String result = manager.processBid((Bidder) loggedInUser, currentAuction, amount);
            sendData(">> Kết quả: " + result);
        } catch (NumberFormatException e) {
            sendData("LỖI: Vui lòng chỉ nhập con số.");
        }
    }

    // THÊM MỚI: Hàm trả về danh sách phiên đấu giá
    private void handleGetAuctions() {
        List<Auction> runningAuctions = manager.getRunningAuctions();
        sendData(Protocol.RES_AUCTION_LIST);
        sendData(runningAuctions); // Gửi nguyên List<Auction>
    }

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