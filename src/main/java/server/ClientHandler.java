package server;

import exception.AuctionClosedException;
import exception.InvalidBidException;
import model.auction.Auction;
import model.user.Bidder;
import service.AuctionManager;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class ClientHandler implements Runnable {
    private Socket socket;
    private AuctionServer server;
    private Auction currentAuction;
    private AuctionManager manager;
    private Bidder myProfile;

    // ĐỔI SANG DÙNG OBJECT STREAMS
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
            // KHỞI TẠO LUỒNG OBJECT (Bắt buộc phải tạo Out trước và flush ngay)
            out = new ObjectOutputStream(socket.getOutputStream());
            out.flush();
            in = new ObjectInputStream(socket.getInputStream());

            // 1. Nhận tên từ Client (Client giờ sẽ gửi String object)
            String playerName = (String) in.readObject();
            if (playerName == null || playerName.trim().isEmpty()) {
                playerName = "Người lạ " + (int)(Math.random() * 100);
            }

            // 2. Tạo profile và ném vào Database
            this.myProfile = new Bidder("B-" + System.currentTimeMillis(), playerName, "123", 5000.0);
            manager.registerUser(myProfile);

            // 3. CHÀO SÂN BẰNG CÁCH GỬI NGUYÊN OBJECT AUCTION XUỐNG
            sendData("=== THÔNG TIN PHIÊN ĐẤU GIÁ ===");
            sendData(currentAuction); // Gửi thẳng object
            sendData("Nhập giá tiền bạn muốn đặt (VD: 36) hoặc 'exit': ");

            // 4. Lắng nghe người dùng đặt giá
            Object inputObj;
            while ((inputObj = in.readObject()) != null) {
                if (inputObj instanceof String) {
                    String input = ((String) inputObj).trim();
                    if (input.equalsIgnoreCase("exit")) break;

                    try {
                        double amount = Double.parseDouble(input);
                        String result = manager.processBid(myProfile, currentAuction, amount);
                        sendData(">> Kết quả: " + result);
                    } catch (NumberFormatException e) {
                        sendData("LỖI: Vui lòng chỉ nhập con số.");
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("Client ngắt kết nối.");
        } finally {
            try { socket.close(); } catch (Exception e) {}
            server.removeClient(this);
        }
    }

    // Hàm nhận mọi loại Object (String, Auction, ...) để gửi xuống Client
    public void sendData(Object data) {
        try {
            if (out != null) {
                out.writeObject(data);

                // Cần thiết để xóa cache, đảm bảo giá trị mới nhất của Object được gửi đi thay vì bản cũ
                out.reset();
                out.flush();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}