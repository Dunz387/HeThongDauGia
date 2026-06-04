package server.room;

import model.user.User;
import server.ClientHandler;
import shared.Protocol;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.logging.Logger;

public final class RoomManager {
    private static final Logger LOGGER = Logger.getLogger(RoomManager.class.getName());
    private final Map<String, Set<ClientHandler>> roomParticipants = new ConcurrentHashMap<>();
    private final Consumer<String> broadcaster;

    public RoomManager(Consumer<String> broadcaster) {
        this.broadcaster = broadcaster;
    }

    public void joinRoom(String auctionId, ClientHandler client) {
        roomParticipants.computeIfAbsent(auctionId, k -> Collections.synchronizedSet(new HashSet<>()))
                .add(client);
        broadcastParticipantsCount(auctionId);
    }

    public void leaveRoom(String auctionId, ClientHandler client) {
        Set<ClientHandler> participants = roomParticipants.get(auctionId);
        if (participants == null) {
            return;
        }

        participants.remove(client);
        if (participants.isEmpty()) {
            roomParticipants.remove(auctionId);
        } else {
            broadcastParticipantsCount(auctionId);
        }
    }

    public void broadcastParticipantsCount(String auctionId) {
        Set<ClientHandler> participants = roomParticipants.get(auctionId);
        int count = participants != null ? participants.size() : 0;
        StringBuilder message = new StringBuilder(Protocol.BROADCAST_PARTICIPANTS)
                .append(Protocol.DELIMITER).append(auctionId)
                .append(Protocol.DELIMITER).append(count);

        if (participants != null) {
            for (ClientHandler client : participants) {
                User user = client.getLoggedInUser();
                if (user != null) {
                    message.append(Protocol.DELIMITER).append(user.getUsername());
                }
            }
        }
        broadcaster.accept(message.toString());
    }

    public void broadcastRoomKicked(String auctionId, String reason) {
        Set<ClientHandler> participants = roomParticipants.get(auctionId);
        if (participants == null) {
            return;
        }

        String message = Protocol.BROADCAST_ROOM_KICKED + Protocol.DELIMITER + auctionId
                + Protocol.DELIMITER + reason;
        for (ClientHandler client : participants) {
            client.sendData(message);
        }
    }

    public void kickUserFromRoom(String auctionId, String targetUsername) {
        Set<ClientHandler> participants = roomParticipants.get(auctionId);
        if (participants == null) {
            return;
        }

        for (ClientHandler client : participants) {
            User user = client.getLoggedInUser();
            if (user != null && targetUsername.equals(user.getUsername())) {
                String message = Protocol.BROADCAST_ROOM_KICKED + Protocol.DELIMITER + auctionId
                        + Protocol.DELIMITER + "Bạn đã bị quản trị viên đuổi khỏi phòng!";
                client.sendData(message);
                LOGGER.info("Bị đuổi: " + user.getUsername() + " khỏi phòng " + auctionId);
                break;
            }
        }
    }
}
