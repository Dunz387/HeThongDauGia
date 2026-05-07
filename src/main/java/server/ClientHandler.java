package server;

import model.auction.Auction;
import model.auction.AuctionStatus;
import model.user.Bidder;
import model.user.User;
import model.user.Seller;
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
                    }
                }
            }
        } catch (Exception e) {
            server.removeClient(this);
        }
    }

    private void handleBid(String auctionId, String amountStr) {
        try {
            if (!(loggedInUser instanceof Bidder)) {
                sendData(Protocol.REQ_BID + Protocol.DELIMITER + Protocol.RES_FAIL + Protocol.DELIMITER + "Chỉ người mua (Bidder) mới được đặt giá!");
                return;
            }
            double amount = Double.parseDouble(amountStr);
            Bidder bidder = (Bidder) loggedInUser;

            Auction auction = manager.getAuctionById(auctionId);
            if (auction == null) {
                sendData(Protocol.REQ_BID + Protocol.DELIMITER + Protocol.RES_FAIL + Protocol.DELIMITER + "Không tìm thấy phiên đấu giá!");
                return;
            }

            String result = manager.processBid(bidder, auction, amount);
            if (result.equals("Thành công!")) {
                sendData(Protocol.REQ_BID + Protocol.DELIMITER + Protocol.RES_SUCCESS);
            } else {
                sendData(Protocol.REQ_BID + Protocol.DELIMITER + Protocol.RES_FAIL + Protocol.DELIMITER + result);
            }
        } catch (Exception e) {
            sendData(Protocol.REQ_BID + Protocol.DELIMITER + Protocol.RES_FAIL + Protocol.DELIMITER + "Lỗi xử lý đặt giá.");
        }
    }

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

            // Bắt Server lắng nghe luôn cái phiên mới tạo này
            auction.addObserver(server);

            manager.registerAuction(auction);

            // Báo thành công cho người vừa tạo
            sendData(Protocol.REQ_CREATE_ITEM + Protocol.DELIMITER + Protocol.RES_SUCCESS);

            // THÊM DÒNG NÀY: Ra lệnh cho Server phát sóng danh sách cập nhật cho TẤT CẢ mọi người
            server.broadcastAuctionList();

        } catch (Exception e) {
            sendData(Protocol.REQ_CREATE_ITEM + Protocol.DELIMITER + Protocol.RES_FAIL);
        }
    }

    private void handleLogin(String u, String p) {
        loggedInUser = manager.authenticateUser(u, p);
        if (loggedInUser != null) {
            sendData(Protocol.REQ_LOGIN + Protocol.DELIMITER + Protocol.RES_SUCCESS + Protocol.DELIMITER + loggedInUser.getRole());
        } else {
            sendData(Protocol.REQ_LOGIN + Protocol.DELIMITER + Protocol.RES_FAIL);
        }
    }

    private void handleRegister(String u, String p) {
        if (manager.registerNewUser(u, p)) {
            sendData(Protocol.REQ_REGISTER + Protocol.DELIMITER + Protocol.RES_SUCCESS);
        } else {
            sendData(Protocol.REQ_REGISTER + Protocol.DELIMITER + Protocol.RES_FAIL);
        }
    }

    public void sendData(Object data) {
        try {
            if (out != null) {
                out.writeObject(data);
                out.reset();
                out.flush();
            }
        } catch (Exception e) {}
    }
}