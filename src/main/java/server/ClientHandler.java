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
                            if (parts.length >= 4) handleCreateItem(parts[1], parts[2], parts[3]);
                            break;
                    }
                }
            }
        } catch (Exception e) {
            server.removeClient(this);
        }
    }

    private void handleGetAuctions() {
        List<Auction> list = manager.getAllAuctions();
        sendData(Protocol.RES_AUCTION_LIST);
        sendData(list);
    }

    private void handleCreateItem(String name, String priceStr, String durStr) {
        try {
            double price = Double.parseDouble(priceStr);
            int dur = Integer.parseInt(durStr);
            Seller owner = (loggedInUser instanceof Seller) ? (Seller) loggedInUser : null;

            // Tạo sản phẩm và phiên đấu giá mới
            model.item.Item item = new model.item.Arts("IT-" + System.currentTimeMillis(), name, "Mô tả", owner, "Ẩn danh", 2024);
            Auction auction = new Auction("AUC-" + System.currentTimeMillis(), item, price, 10.0, java.time.LocalDateTime.now().plusMinutes(dur));

            // Thiết lập trạng thái RUNNING để Client có thể thấy ngay
            auction.setStatus(AuctionStatus.RUNNING);
            manager.registerAuction(auction);

            sendData(Protocol.REQ_CREATE_ITEM + Protocol.DELIMITER + Protocol.RES_SUCCESS);
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
                out.reset(); // Xóa bộ nhớ đệm để gửi dữ liệu mới nhất
                out.flush();
            }
        } catch (Exception e) {}
    }
}