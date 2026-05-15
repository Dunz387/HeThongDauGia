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

    public User getLoggedInUser() {
        return loggedInUser;
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
                            if (parts.length >= 4) handleRegister(parts[1], parts[2], parts[3]);
                            else if (parts.length >= 3) handleRegister(parts[1], parts[2], "BIDDER");
                            break;
                        case Protocol.REQ_GET_AUCTIONS:
                            handleGetAuctions();
                            break;
                        case Protocol.REQ_CREATE_ITEM:
                            if (parts.length >= 6) handleCreateItem(parts[1], parts[2], parts[3], parts[4], parts[5]);
                            break;
                        case Protocol.REQ_UPDATE_ITEM:
                            if (parts.length >= 7) handleUpdateItem(parts[1], parts[2], parts[3], parts[4], parts[5], parts[6]);
                            break;
                        case Protocol.REQ_DELETE_ITEM:
                            if (parts.length >= 2) handleDeleteItem(parts[1]);
                            break;
                        case Protocol.REQ_BID:
                            if (parts.length >= 3) handleBid(parts[1], parts[2]);
                            break;
                        case Protocol.REQ_AUTOBID:
                            if (parts.length >= 4) handleAutoBid(parts[1], parts[2], parts[3]);
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
                        // THÊM MỚI: Giao dịch tài chính
                        case Protocol.REQ_DEPOSIT:
                            if (parts.length >= 2) handleDeposit(parts[1]);
                            break;
                        case Protocol.REQ_WITHDRAW:
                            if (parts.length >= 2) handleWithdraw(parts[1]);
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
            // Trả về: LOGIN;;;SUCCESS;;;ROLE;;;userId;;;username;;;balance
            double balance = 0.0;
            if (loggedInUser instanceof Bidder) balance = ((Bidder) loggedInUser).getAvailableBalance();
            else if (loggedInUser instanceof Seller) balance = ((Seller) loggedInUser).getBalance();

            sendData(Protocol.REQ_LOGIN + Protocol.DELIMITER + Protocol.RES_SUCCESS
                    + Protocol.DELIMITER + loggedInUser.getRole()
                    + Protocol.DELIMITER + loggedInUser.getId()
                    + Protocol.DELIMITER + loggedInUser.getUsername()
                    + Protocol.DELIMITER + balance);
        } else {
            sendData(Protocol.REQ_LOGIN + Protocol.DELIMITER + Protocol.RES_FAIL + Protocol.DELIMITER + "Tài khoản không tồn tại hoặc đã bị khóa!");
        }
    }

    private void handleRegister(String u, String p, String role) {
        if (manager.registerNewUser(u, p, role)) {
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

    private void handleCreateItem(String name, String priceStr, String durStr, String itemType, String itemDesc) {
        if (!(loggedInUser instanceof Seller)) {
            sendData(Protocol.REQ_CREATE_ITEM + Protocol.DELIMITER + Protocol.RES_FAIL + Protocol.DELIMITER + "Chỉ người bán mới được tạo phiên đấu giá!");
            return;
        }
        
        try {
            double price = Double.parseDouble(priceStr);
            int dur = Integer.parseInt(durStr);
            Seller owner = (Seller) loggedInUser;
            String desc = (itemDesc != null) ? itemDesc : "Mô tả sản phẩm";

            model.item.Item item = model.item.ItemFactory.createItem(
                    itemType,
                    "IT-" + System.currentTimeMillis(),
                    name,
                    desc,
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

            // 3. Broadcast AUCTION_START cho tất cả Client để bắt đầu timer
            String startMessage = Protocol.BROADCAST_AUCTION_START + Protocol.DELIMITER
                    + auction.getId() + Protocol.DELIMITER + dur;
            server.broadcast(startMessage);
            System.out.println("📢 [BROADCAST] Phiên đấu giá bắt đầu: " + auction.getId() + " | Thời gian: " + dur + " phút");

        } catch (Exception e) {
            sendData(Protocol.REQ_CREATE_ITEM + Protocol.DELIMITER + Protocol.RES_FAIL);
        }
    }

    private void handleUpdateItem(String auctionId, String newName, String newDesc, String newType, String newPriceStr, String newDurStr) {
        try {
            if (loggedInUser instanceof Admin) {
                double newPrice = Double.parseDouble(newPriceStr);
                int newDur = Integer.parseInt(newDurStr);
                if (manager.updateAuctionForce(auctionId, newName, newDesc, newType, newPrice, newDur)) {
                    sendData(Protocol.REQ_UPDATE_ITEM + Protocol.DELIMITER + Protocol.RES_SUCCESS);
                    server.broadcastAuctionList();
                } else {
                    sendData(Protocol.REQ_UPDATE_ITEM + Protocol.DELIMITER + Protocol.RES_FAIL + Protocol.DELIMITER + "Lỗi cập nhật (không tìm thấy phiên).");
                }
            } else if (loggedInUser instanceof Seller) {
                if (manager.updateAuctionBySeller(auctionId, loggedInUser.getId(), newName, newDesc, newType)) {
                    sendData(Protocol.REQ_UPDATE_ITEM + Protocol.DELIMITER + Protocol.RES_SUCCESS);
                    server.broadcastAuctionList();
                } else {
                    sendData(Protocol.REQ_UPDATE_ITEM + Protocol.DELIMITER + Protocol.RES_FAIL + Protocol.DELIMITER + "Không thể sửa (đã có người đặt giá hoặc sai quyền).");
                }
            } else {
                sendData(Protocol.REQ_UPDATE_ITEM + Protocol.DELIMITER + Protocol.RES_FAIL + Protocol.DELIMITER + "Bạn không có quyền thực hiện!");
            }
        } catch (Exception e) {
            sendData(Protocol.REQ_UPDATE_ITEM + Protocol.DELIMITER + Protocol.RES_FAIL + Protocol.DELIMITER + "Dữ liệu không hợp lệ.");
        }
    }

    private void handleDeleteItem(String auctionId) {
        if (loggedInUser instanceof Seller) {
            if (manager.deleteAuctionBySeller(auctionId, loggedInUser.getId())) {
                sendData(Protocol.REQ_DELETE_ITEM + Protocol.DELIMITER + Protocol.RES_SUCCESS);
                server.broadcastAuctionList(); // Cập nhật lại UI cho mọi người
            } else {
                sendData(Protocol.REQ_DELETE_ITEM + Protocol.DELIMITER + Protocol.RES_FAIL + Protocol.DELIMITER + "Không thể xóa (đã có người đặt giá hoặc sai quyền).");
            }
        } else {
            sendData(Protocol.REQ_DELETE_ITEM + Protocol.DELIMITER + Protocol.RES_FAIL + Protocol.DELIMITER + "Chỉ người bán mới được xóa!");
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
                // Gửi cập nhật số dư mới nhất cho chính người vừa đặt giá (Số dư khả dụng)
                sendData(Protocol.RES_UPDATE_BALANCE + Protocol.DELIMITER + bidder.getAvailableBalance());
            } else {
                sendData(Protocol.REQ_BID + Protocol.DELIMITER + Protocol.RES_FAIL + Protocol.DELIMITER + result);
            }
        } catch (Exception e) {
            sendData(Protocol.REQ_BID + Protocol.DELIMITER + Protocol.RES_FAIL + Protocol.DELIMITER + "Lỗi hệ thống đặt giá.");
        }
    }

    private void handleAutoBid(String auctionId, String maxBidStr, String incrementStr) {
        try {
            if (!(loggedInUser instanceof Bidder)) {
                sendData(Protocol.REQ_AUTOBID + Protocol.DELIMITER + Protocol.RES_FAIL + Protocol.DELIMITER + "Chỉ người mua mới được đặt auto-bid!");
                return;
            }
            double maxBid = Double.parseDouble(maxBidStr);
            double increment = Double.parseDouble(incrementStr);
            Bidder bidder = (Bidder) loggedInUser;

            Auction auction = manager.getAuctionById(auctionId);
            if (auction == null) {
                sendData(Protocol.REQ_AUTOBID + Protocol.DELIMITER + Protocol.RES_FAIL + Protocol.DELIMITER + "Không tìm thấy sản phẩm!");
                return;
            }

            auction.registerAutoBid(bidder, maxBid, increment);
            sendData(Protocol.REQ_AUTOBID + Protocol.DELIMITER + Protocol.RES_SUCCESS);
            
        } catch (Exception e) {
            sendData(Protocol.REQ_AUTOBID + Protocol.DELIMITER + Protocol.RES_FAIL + Protocol.DELIMITER + "Lỗi hệ thống auto-bid.");
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

    // THÊM MỚI: Giao dịch tài chính
    private void handleDeposit(String amountStr) {
        try {
            double amount = Double.parseDouble(amountStr);
            if (loggedInUser instanceof Bidder) {
                Bidder bidder = (Bidder) loggedInUser;
                bidder.addBalance(amount);
                dao.UserDAO.updateUserBalance(bidder.getId(), bidder.getBalance());
                sendData(Protocol.REQ_DEPOSIT + Protocol.DELIMITER + Protocol.RES_SUCCESS);
                sendData(Protocol.RES_UPDATE_BALANCE + Protocol.DELIMITER + bidder.getAvailableBalance());
            } else {
                sendData(Protocol.REQ_DEPOSIT + Protocol.DELIMITER + Protocol.RES_FAIL + Protocol.DELIMITER + "Chỉ Bidder mới có thể nạp tiền!");
            }
        } catch (Exception e) {
            sendData(Protocol.REQ_DEPOSIT + Protocol.DELIMITER + Protocol.RES_FAIL + Protocol.DELIMITER + "Số tiền không hợp lệ.");
        }
    }

    private void handleWithdraw(String amountStr) {
        try {
            double amount = Double.parseDouble(amountStr);
            if (loggedInUser instanceof Seller) {
                Seller seller = (Seller) loggedInUser;
                if (seller.deductBalance(amount)) {
                    dao.UserDAO.updateUserBalance(seller.getId(), seller.getBalance());
                    sendData(Protocol.REQ_WITHDRAW + Protocol.DELIMITER + Protocol.RES_SUCCESS);
                    sendData(Protocol.RES_UPDATE_BALANCE + Protocol.DELIMITER + seller.getBalance());
                } else {
                    sendData(Protocol.REQ_WITHDRAW + Protocol.DELIMITER + Protocol.RES_FAIL + Protocol.DELIMITER + "Số dư không đủ để rút!");
                }
            } else {
                sendData(Protocol.REQ_WITHDRAW + Protocol.DELIMITER + Protocol.RES_FAIL + Protocol.DELIMITER + "Chỉ Seller mới có thể rút tiền!");
            }
        } catch (Exception e) {
            sendData(Protocol.REQ_WITHDRAW + Protocol.DELIMITER + Protocol.RES_FAIL + Protocol.DELIMITER + "Số tiền không hợp lệ.");
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