package server;

import exception.AuctionClosedException;
import exception.InvalidBidException;
import model.auction.Auction;
import model.user.Bidder;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class ClientHandler implements Runnable {
    private Socket socket;
    private AuctionServer server;
    private Auction currentAuction;
    private Bidder myProfile;
    private ObjectOutputStream out;

    public ClientHandler(Socket socket, AuctionServer server, Auction auction) {
        this.socket = socket;
        this.server = server;
        this.currentAuction = auction;
        String id = "B" + (int)(Math.random() * 100);
        this.myProfile = new Bidder(id, "User_" + id, "123", 5000.0);
    }

    @Override
    public void run() {
        try {
            out = new ObjectOutputStream(socket.getOutputStream());
            ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
            Object firstData = in.readObject();
            String playerName = "Người lạ";
            if (firstData instanceof String) {
                playerName = (String) firstData;
            }

            // Khởi tạo Bidder với tên thật vừa nhận được
            this.myProfile = new Bidder("B-" + System.currentTimeMillis(), playerName, "123", 5000.0);
            sendData(currentAuction);

            while (true) {
                Object request = in.readObject();
                if (request instanceof String) {
                    String input = ((String) request).trim();
                    try {
                        // TỰ ĐỘNG HIỂU LÀ TIỀN NẾU NHẬP SỐ
                        double amount = Double.parseDouble(input);
                        currentAuction.placeBid(myProfile, amount);
                        sendData(" Đặt giá $" + amount + " thành công!");
                    } catch (NumberFormatException e) {
                        sendData(" LỖI: Vui lòng chỉ nhập con số (Ví dụ: 1600).");
                    } catch (AuctionClosedException | InvalidBidException e) {
                        sendData(" LỖI: " + e.getMessage());
                    }
                }
            }
        } catch (Exception e) { server.removeClient(this); }
    }

    public void sendData(Object data) {
        try { out.reset(); out.writeObject(data); out.flush(); } catch (Exception e) {}
    }
}
