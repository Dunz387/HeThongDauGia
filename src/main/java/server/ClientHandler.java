package server;

import model.auction.Auction;
import model.auction.AuctionStatus;
import model.user.Admin;
import model.user.Bidder;
import model.user.Seller;
import model.user.User;
import service.AuctionManager;
import shared.Protocol;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.List;

public class ClientHandler implements Runnable {
    private Socket socket;
    private AuctionServer server;
    private AuctionManager manager;
    private User loggedInUser = null;
    private ObjectOutputStream out;
    private ObjectInputStream in;

    public ClientHandler(Socket socket, AuctionServer server, AuctionManager manager) {
        this.socket = socket;
        this.server = server;
        this.manager = manager;
    }

    @Override
    public void run() {
        try {
            out = new ObjectOutputStream(socket.getOutputStream());
            out.flush();
            in = new ObjectInputStream(socket.getInputStream());

            Object obj;
            while ((obj = in.readObject()) != null) {
                if (obj instanceof String) {
                    String[] parts = ((String) obj).split(Protocol.SEPARATOR);
                    String cmd = parts[0];

                    switch (cmd) {
                        case Protocol.REQ_LOGIN:
                            if (parts.length >= 3) handleLogin(parts[1], parts[2]);
                            break;
                        case Protocol.REQ_REGISTER:
                            if (parts.length >= 3) handleRegister(parts[1], parts[2]);
                            break;
                        case Protocol.REQ_GET_AUCTIONS:
                            handleGetAuctions();
                            break;
                        case Protocol.REQ_CREATE_ITEM:
                            if (parts.length >= 5) handleCreateItem(parts[1], parts[2], parts[3], parts[4]);
                            break;
                        case Protocol.REQ_BID:
                            if (parts.length >= 3) handleBid(parts[1], parts[2]);
                            break;
                        case Protocol.REQ_GET_USERS:
                            handleGetUsers();
                            break;
                        case Protocol.REQ_BAN_USER:
                            if (parts.length >= 3) handleBanUser(parts[1], parts[2]);
                            break;
                        // THÊM MỚI: Xử lý xóa phiên đấu giá (Admin)
                        case Protocol.REQ_DELETE_AUCTION:
                            if (parts.length >= 2) handleDeleteAuction(parts[1]);
                            break;
                    }
                }
            }
        } catch (Exception e) {
            server.removeClient(this);
        }
    }

    // --- NHÓM LỆNH XÁC THỰC ---
    private void handleLogin(String u, String p) {
        loggedInUser = manager.authenticateUser(u, p);
        if (loggedInUser != null) {
            // Trả về: LOGIN;;;SUCCESS;;;ROLE
            sendData(Protocol.REQ_LOGIN + Protocol.DELIMITER + Protocol.RES_SUCCESS + Protocol.DELIMITER + loggedInUser.getRole());
        } else {
            sendData(Protocol.REQ_LOGIN + Protocol.DELIMITER + Protocol.RES_FAIL + Protocol.DELIMITER + "Tài khoản không tồn tại hoặc đã bị khóa!");
        }
    }

    private void handleRegister(String u, String p) {
        if (manager.registerNewUser(u, p)) {
            sendData(Protocol.REQ_REGISTER + Protocol.DELIMITER + Protocol.RES_SUCCESS);
        } else {
            sendData(Protocol.REQ_REGISTER + Protocol.DELIMITER + Protocol.RES_FAIL);
        }
    }

    // --- NHÓM LỆNH ĐẤU GIÁ ---
    private void handleGetAuctions() {
        List<Auction> list = manager.getAllAuctions();
        sendData(Protocol.RES_AUCTION_LIST);
        sendData(list);
    }

    private void handleCreateItem(String name, String priceStr, String durStr, String itemType) {
        try {
            double price = Double.parseDouble(priceStr);
            int dur = Integer.parseInt(durStr);
            Seller owner = (loggedInUser instanceof Seller) ? (Seller) loggedInUser : null;

            model.item.Item item = model.item.ItemFactory.createItem(
                    itemType,
                    "IT-" + System.currentTimeMillis(),
                    name,
                    "Mô tả sản phẩm",
                    owner,
                    "Thông tin thêm",
                    0
            );

            Auction auction = new Auction("AUC-" + System.currentTimeMillis(), item, price, 10.0, java.time.LocalDateTime.now().plusMinutes(dur));
            auction.setStatus(AuctionStatus.RUNNING);

            // Đăng ký Server làm Observer để bắn giá realtime sau này
            auction.addObserver(server);

            manager.registerAuction(auction);

            // 1. Phản hồi thành công cho người tạo
            sendData(Protocol.REQ_CREATE_ITEM + Protocol.DELIMITER + Protocol.RES_SUCCESS);

            // 2. Tự động cập nhật danh sách cho TẤT CẢ mọi người (Auto-refresh)
            server.broadcastAuctionList();

        } catch (Exception e) {
            sendData(Protocol.REQ_CREATE_ITEM + Protocol.DELIMITER + Protocol.RES_FAIL);
        }
    }

    private void handleBid(String auctionId, String amountStr) {
        try {
            if (!(loggedInUser instanceof Bidder)) {
                sendData(Protocol.REQ_BID + Protocol.DELIMITER + Protocol.RES_FAIL + Protocol.DELIMITER + "Chỉ người mua mới được đặt giá!");
                return;
            }
            double amount = Double.parseDouble(amountStr);
            Bidder bidder = (Bidder) loggedInUser;

            Auction auction = manager.getAuctionById(auctionId);
            if (auction == null) {
                sendData(Protocol.REQ_BID + Protocol.DELIMITER + Protocol.RES_FAIL + Protocol.DELIMITER + "Không tìm thấy sản phẩm!");
                return;
            }

            String result = manager.processBid(bidder, auction, amount);
            if (result.equals("Thành công!")) {
                sendData(Protocol.REQ_BID + Protocol.DELIMITER + Protocol.RES_SUCCESS);
            } else {
                sendData(Protocol.REQ_BID + Protocol.DELIMITER + Protocol.RES_FAIL + Protocol.DELIMITER + result);
            }
        } catch (Exception e) {
            sendData(Protocol.REQ_BID + Protocol.DELIMITER + Protocol.RES_FAIL + Protocol.DELIMITER + "Lỗi hệ thống đặt giá.");
        }
    }

    // --- NHÓM LỆNH ADMIN ---
    private void handleGetUsers() {
        if (loggedInUser instanceof Admin) {
            List<User> list = manager.getAllUsers();
            sendData(Protocol.RES_USER_LIST);
            sendData(list);
        } else {
            sendData(Protocol.REQ_GET_USERS + Protocol.DELIMITER + Protocol.RES_FAIL + Protocol.DELIMITER + "Quyền truy cập bị từ chối!");
        }
    }

    private void handleBanUser(String targetId, String statusStr) {
        if (loggedInUser instanceof Admin) {
            boolean isEnable = Boolean.parseBoolean(statusStr); // true = mở khóa, false = khóa
            if (manager.banUser(targetId, isEnable)) {
                sendData(Protocol.REQ_BAN_USER + Protocol.DELIMITER + Protocol.RES_SUCCESS);
                // THÊM MỚI: Broadcast danh sách User mới cho tất cả Admin (real-time)
                server.broadcastUserList();
            } else {
                sendData(Protocol.REQ_BAN_USER + Protocol.DELIMITER + Protocol.RES_FAIL + Protocol.DELIMITER + "Lỗi cập nhật trạng thái!");
            }
        } else {
            sendData(Protocol.REQ_BAN_USER + Protocol.DELIMITER + Protocol.RES_FAIL + Protocol.DELIMITER + "Chỉ Admin mới được thực hiện!");
        }
    }

    // THÊM MỚI: Xóa phiên đấu giá cưỡng chế (Admin)
    private void handleDeleteAuction(String auctionId) {
        if (loggedInUser instanceof Admin) {
            if (manager.deleteAuctionForce(auctionId)) {
                sendData(Protocol.REQ_DELETE_AUCTION + Protocol.DELIMITER + Protocol.RES_SUCCESS);
                // Broadcast danh sách đấu giá mới cho tất cả (real-time)
                server.broadcastAuctionList();
            } else {
                sendData(Protocol.REQ_DELETE_AUCTION + Protocol.DELIMITER + Protocol.RES_FAIL + Protocol.DELIMITER + "Không tìm thấy phiên đấu giá!");
            }
        } else {
            sendData(Protocol.REQ_DELETE_AUCTION + Protocol.DELIMITER + Protocol.RES_FAIL + Protocol.DELIMITER + "Chỉ Admin mới được thực hiện!");
        }
    }

    // --- HÀM GỬI DỮ LIỆU ---
    public void sendData(Object data) {
        try {
            if (out != null) {
                out.writeObject(data);
                out.reset(); // Xóa cache để dữ liệu sau không bị trùng lặp
                out.flush();
            }
        } catch (Exception e) {
            System.err.println("Lỗi gửi dữ liệu tới Client: " + e.getMessage());
        }
    }
}