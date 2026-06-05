package server;

import model.auction.Auction;
import model.auction.AuctionObserver;
import model.user.Bidder;

import server.broadcast.ServerBroadcaster;
import server.event.AuctionEventPublisher;
import server.notification.ClientNotifier;
import server.room.RoomManager;

import service.auction.AuctionManager;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

public class AuctionServer implements AuctionObserver {
    private static final Logger LOGGER = Logger.getLogger(AuctionServer.class.getName());
    private static final int PORT = 8080;

    private final List<ClientHandler> clients = new CopyOnWriteArrayList<>();
    private final ExecutorService broadcastExecutor = Executors.newFixedThreadPool(4);
    private AuctionManager manager;
    private ServerBroadcaster broadcaster;
    private ClientNotifier clientNotifier;
    private RoomManager roomManager;
    private AuctionEventPublisher eventPublisher;
    private volatile boolean isRunning = true;

    public static void main(String[] args) {
        new AuctionServer().startServer();
    }

    public void startServer() {
        manager = AuctionManager.getInstance();
        broadcaster = new ServerBroadcaster(clients, manager, broadcastExecutor);
        clientNotifier = new ClientNotifier(clients);
        roomManager = new RoomManager(this::broadcast);
        eventPublisher = new AuctionEventPublisher(manager, broadcaster, clientNotifier);

        observeExistingAuctions();
        eventPublisher.registerAuctionFinishedCallback();
        startConsoleExitListener();
        listenForClients();
    }

    public void removeClient(ClientHandler client) {
        clients.remove(client);
    }

    public void broadcast(String message) {
        broadcaster.broadcast(message);
    }

    public void broadcastAuctionList() {
        broadcaster.broadcastAuctionList();
    }

    public void broadcastUserList() {
        broadcaster.broadcastUserList();
    }

    public void joinRoom(String auctionId, ClientHandler client) {
        roomManager.joinRoom(auctionId, client);
    }

    public void leaveRoom(String auctionId, ClientHandler client) {
        roomManager.leaveRoom(auctionId, client);
    }

    public void broadcastParticipantsCount(String auctionId) {
        roomManager.broadcastParticipantsCount(auctionId);
    }

    public void broadcastRoomKicked(String auctionId, String reason) {
        roomManager.broadcastRoomKicked(auctionId, reason);
    }

    public void kickUserFromRoom(String auctionId, String targetUsername) {
        roomManager.kickUserFromRoom(auctionId, targetUsername);
    }

    public void sendBalanceUpdateToUser(String userId) {
        clientNotifier.sendBalanceUpdateToUser(userId);
    }

    public boolean isUserLoggedIn(String userId) {
        return clientNotifier.isUserLoggedIn(userId);
    }

    public void forceLogoutUser(String targetUserId) {
        clientNotifier.forceLogoutUser(targetUserId);
    }

    @Override
    public void update(Auction auction, double newPrice, String topBidderName, Bidder previousBidder) {
        eventPublisher.publishNewBid(auction, newPrice, topBidderName, previousBidder);
    }

    @Override
    public void onTimeExtended(Auction auction, int addedSeconds) {
        eventPublisher.publishTimeExtended(auction, addedSeconds);
    }

    @Override
    public void onAutoBidExpired(Auction auction, Bidder bidder) {
        eventPublisher.publishAutoBidExpired(bidder);
    }

    private void observeExistingAuctions() {
        for (Auction auction : manager.getAllAuctions()) {
            auction.addObserver(this);
        }
    }

    // exit để tắt task server
    private void startConsoleExitListener() {
        new Thread(() -> {
            Scanner scanner = new Scanner(System.in);
            LOGGER.info("Hệ thống Server đã sẵn sàng. Gõ 'exit' để tắt.");
            while (scanner.hasNextLine()) {
                if ("exit".equalsIgnoreCase(scanner.nextLine())) {
                    System.exit(0);
                }
            }
        }).start();
    }

    private void listenForClients() {
        try (ServerSocket serverSocket = new ServerSocket()) {
            serverSocket.setReuseAddress(true);
            serverSocket.bind(new InetSocketAddress(PORT));
            LOGGER.info("[SERVER] Đang lắng nghe tại cổng " + PORT);

            while (isRunning) {
                Socket socket = serverSocket.accept();
                LOGGER.info("Có kết nối mới: " + socket.getInetAddress());
                ClientHandler handler = new ClientHandler(socket, this, manager);
                clients.add(handler);
                new Thread(handler).start();
            }
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Lỗi khi khởi chạy Server", e);
        }
    }
}
