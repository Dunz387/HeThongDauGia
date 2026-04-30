package server;

import exception.AuctionClosedException;
import exception.InvalidBidException;
import model.auction.Auction;
import model.user.Bidder;
import service.AuctionManager;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class ClientHandler implements Runnable {
    private Socket socket;
    private AuctionServer server;
    private Auction currentAuction;
    private AuctionManager manager;
    private Bidder myProfile;
    private PrintWriter out;
    private BufferedReader in;

    public ClientHandler(Socket socket, AuctionServer server, Auction auction, AuctionManager manager) {
        this.socket = socket;
        this.server = server;
        this.currentAuction = auction;
        this.manager = manager;
    }

    @Override
    public void run() {
        try {
            // Chuyển sang dùng PrintWriter và BufferedReader cho chuẩn chuỗi Text
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            // 1. Nhận tên từ Client
            String playerName = in.readLine();
            if (playerName == null || playerName.trim().isEmpty()) {
                playerName = "Người lạ " + (int)(Math.random() * 100);
            }

            // 2. Tạo profile và ném vào Database qua Manager
            this.myProfile = new Bidder("B-" + System.currentTimeMillis(), playerName, "123", 5000.0);
            manager.registerUser(myProfile);

            // 3. Gửi thông tin chào sân (Gửi dạng chuỗi Text thay vì gửi nguyên Object)
            sendData("=== THÔNG TIN PHIÊN ĐẤU GIÁ ===");
            sendData("Sản phẩm: " + currentAuction.getItem().getName());
            sendData("Giá hiện tại: $" + currentAuction.getCurrentPrice());
            sendData("Trạng thái: " + currentAuction.getStatus());
            sendData("-------------------------------");
            sendData("Nhập giá tiền bạn muốn đặt (VD: 36) hoặc 'exit': ");

            // 4. Lắng nghe người dùng đặt giá
            String input;
            while ((input = in.readLine()) != null) {
                input = input.trim();
                if (input.equalsIgnoreCase("exit")) break;

                try {
                    double amount = Double.parseDouble(input);
                    // Dùng Manager để xử lý Bid (nó sẽ lưu thẳng xuống Database)
                    String result = manager.processBid(myProfile, currentAuction, amount);
                    sendData(">> Kết quả: " + result);
                } catch (NumberFormatException e) {
                    sendData(" LỖI: Vui lòng chỉ nhập con số (Ví dụ: 1600).");
                }
            }
        } catch (Exception e) {
            server.removeClient(this);
        } finally {
            try { socket.close(); } catch (Exception e) {}
            server.removeClient(this);
        }
    }

    // Hàm gửi tin nhắn dạng Text
    public void sendData(String message) {
        if (out != null) {
            out.println(message);
        }
    }
}