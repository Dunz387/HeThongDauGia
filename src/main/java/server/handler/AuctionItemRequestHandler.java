package server.handler;

import model.auction.Auction;
import model.auction.AuctionStatus;
import model.item.Item;
import model.item.ItemBuilder;
import model.user.Admin;
import model.user.Seller;
import model.user.User;
import server.AuctionServer;
import server.ClientHandler;
import service.admin.AdminService;
import service.auction.AuctionManager;
import shared.Protocol;

import java.time.LocalDateTime;
import java.util.logging.Logger;

final class AuctionItemRequestHandler {
    private static final Logger LOGGER = Logger.getLogger(AuctionItemRequestHandler.class.getName());
    private final ClientHandler clientHandler;
    private final AuctionServer server;
    private final AuctionManager manager;

    AuctionItemRequestHandler(ClientHandler clientHandler, AuctionServer server, AuctionManager manager) {
        this.clientHandler = clientHandler;
        this.server = server;
        this.manager = manager;
    }

    void handleCreateItem(String name, String priceStr, String durStr, String itemType, String itemDesc) {
        if (!(getUser() instanceof Seller)) {
            failCreate("Chỉ người bán mới được tạo phiên đấu giá!");
            return;
        }

        try {
            int durationMinutes = Integer.parseInt(durStr);
            Auction auction = createAuction(name, Double.parseDouble(priceStr), durationMinutes, itemType, itemDesc);
            auction.addObserver(server);
            manager.registerAuction(auction);

            clientHandler.sendData(Protocol.REQ_CREATE_ITEM + Protocol.DELIMITER + Protocol.RES_SUCCESS);
            server.broadcastAuctionList();
            server.broadcast(Protocol.BROADCAST_AUCTION_START + Protocol.DELIMITER
                    + auction.getId() + Protocol.DELIMITER + durationMinutes);
            LOGGER.info(String.format("[BROADCAST] Phiên đấu giá bắt đầu: %s | Thời gian: %d phút",
                    auction.getId(), durationMinutes));
        } catch (Exception e) {
            clientHandler.sendData(Protocol.REQ_CREATE_ITEM + Protocol.DELIMITER + Protocol.RES_FAIL);
        }
    }

    void handleUpdateItem(String auctionId, String newName, String newDesc, String newType,
            String newPriceStr, String newDurStr) {
        try {
            if (getUser() instanceof Admin) {
                updateByAdmin(auctionId, newName, newDesc, newType, newPriceStr, newDurStr);
            } else if (getUser() instanceof Seller) {
                updateBySeller(auctionId, newName, newDesc, newType);
            } else {
                failUpdate("Bạn không có quyền thực hiện!");
            }
        } catch (Exception e) {
            failUpdate("Dữ liệu không hợp lệ.");
        }
    }

    void handleDeleteItem(String auctionId) {
        if (!(getUser() instanceof Seller)) {
            clientHandler.sendData(Protocol.REQ_DELETE_ITEM + Protocol.DELIMITER + Protocol.RES_FAIL
                    + Protocol.DELIMITER + "Chỉ người bán mới được xóa!");
            return;
        }

        if (manager.deleteAuctionBySeller(auctionId, getUser().getId())) {
            clientHandler.sendData(Protocol.REQ_DELETE_ITEM + Protocol.DELIMITER + Protocol.RES_SUCCESS);
            server.broadcastAuctionList();
        } else {
            clientHandler.sendData(Protocol.REQ_DELETE_ITEM + Protocol.DELIMITER + Protocol.RES_FAIL
                    + Protocol.DELIMITER + "Không thể xóa (đã có người đặt giá hoặc sai quyền).");
        }
    }

    private Auction createAuction(String name, double price, int durationMinutes, String itemType, String itemDesc) {
        Seller owner = (Seller) getUser();
        String description = itemDesc != null ? itemDesc : "Mô tả sản phẩm";
        Item item = new ItemBuilder()
                .setType(itemType)
                .setId("IT-" + System.currentTimeMillis())
                .setName(name)
                .setDescription(description)
                .setOwner(owner)
                .build();
        Auction auction = new Auction("AUC-" + System.currentTimeMillis(), item, price, 10.0,
                LocalDateTime.now().plusMinutes(durationMinutes));
        auction.setStatus(AuctionStatus.RUNNING);
        return auction;
    }

    private void updateByAdmin(String auctionId, String newName, String newDesc, String newType,
            String newPriceStr, String newDurStr) {
        boolean updated = AdminService.getInstance().updateAuctionForce(
                auctionId, newName, newDesc, newType, Double.parseDouble(newPriceStr), Integer.parseInt(newDurStr));
        if (updated) {
            updateSuccess();
        } else {
            failUpdate("Lỗi cập nhật (không tìm thấy phiên).");
        }
    }

    private void updateBySeller(String auctionId, String newName, String newDesc, String newType) {
        boolean updated = manager.updateAuctionBySeller(auctionId, getUser().getId(), newName, newDesc, newType);
        if (updated) {
            updateSuccess();
        } else {
            failUpdate("Không thể sửa (đã có người đặt giá hoặc sai quyền).");
        }
    }

    private void updateSuccess() {
        clientHandler.sendData(Protocol.REQ_UPDATE_ITEM + Protocol.DELIMITER + Protocol.RES_SUCCESS);
        server.broadcastAuctionList();
    }

    private void failCreate(String message) {
        clientHandler.sendData(Protocol.REQ_CREATE_ITEM + Protocol.DELIMITER + Protocol.RES_FAIL
                + Protocol.DELIMITER + message);
    }

    private void failUpdate(String message) {
        clientHandler.sendData(Protocol.REQ_UPDATE_ITEM + Protocol.DELIMITER + Protocol.RES_FAIL
                + Protocol.DELIMITER + message);
    }

    private User getUser() {
        return clientHandler.getLoggedInUser();
    }
}
