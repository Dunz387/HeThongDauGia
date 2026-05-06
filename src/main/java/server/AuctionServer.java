package server;

import service.AuctionManager;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.CopyOnWriteArrayList;

public class AuctionServer {
    private static final int PORT = 8080;
    private List<ClientHandler> clients = new CopyOnWriteArrayList<>();
    private AuctionManager manager;

    public static void main(String[] args) {
        new AuctionServer().startServer();
    }

    public void startServer() {
        manager = AuctionManager.getInstance();

        // Luồng xử lý lệnh Terminal để không làm treo Server
        new Thread(() -> {
            Scanner s = new Scanner(System.in);
            System.out.println("Hệ thống Server đã sẵn sàng. Gõ 'exit' để tắt.");
            while (s.hasNextLine()) {
                if ("exit".equalsIgnoreCase(s.nextLine())) System.exit(0);
            }
        }).start();

        try (ServerSocket serverSocket = new ServerSocket()) {
            serverSocket.setReuseAddress(true);
            serverSocket.bind(new InetSocketAddress(PORT));
            System.out.println("[SERVER] Đang lắng nghe tại cổng " + PORT);

            while (true) {
                Socket socket = serverSocket.accept();
                System.out.println("Có kết nối mới: " + socket.getInetAddress());
                ClientHandler handler = new ClientHandler(socket, this, manager);
                clients.add(handler);
                new Thread(handler).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void removeClient(ClientHandler c) {
        clients.remove(c);
    }
}