package server.handler;

import model.auction.Auction;
import model.auction.AuctionStatus;
import model.user.Bidder;
import model.user.Seller;
import model.user.User;
import service.AuctionManager;
import shared.Protocol;
import server.AuctionServer;

import java.util.List;
import java.util.logging.Logger;

/**
 * Xử lý các request đấu giá: CRUD Item, Bid, AutoBid, JoinRoom, LeaveRoom. Tách
 * từ ClientHandler để tuân thủ SRP.
 */
public class AuctionHandler {
    private static final Logger LOGGER = Logger.getLogger(AuctionHandler.class.getName());
    private final server.ClientHandler clientHandler;
    private final AuctionServer server;
    private final AuctionManager manager;

    public AuctionHandler(server.ClientHandler clientHandler, AuctionServer server, AuctionManager manager) {
        this.clientHandler = clientHandler;
        this.server = server;
        this.manager = manager;
    }

    private User getUser() {
        return clientHandler.getLoggedInUser();
    }

    public void handleGetAuctions() {
        List<Auction> list = manager.getAllAuctions();
        clientHandler.sendData(Protocol.RES_AUCTION_LIST);
        clientHandler.sendData(list);
    }

    public void handleCreateItem(String name, String priceStr, String durStr, String itemType, String itemDesc) {
        if (!(getUser() instanceof Seller)) {
            clientHandler.sendData(Protocol.REQ_CREATE_ITEM + Protocol.DELIMITER + Protocol.RES_FAIL
                    + Protocol.DELIMITER + "Chỉ người bán mới được tạo phiên đấu giá!");
            return;
        }
        try {
            double price = Double.parseDouble(priceStr);
            int dur = Integer.parseInt(durStr);
            Seller owner = (Seller) getUser();
            String desc = (itemDesc != null) ? itemDesc : "Mô tả sản phẩm";

            model.item.Item item = model.item.ItemFactory.createItem(itemType, "IT-" + System.currentTimeMillis(), name,
                    desc, owner, "Thông tin thêm", 0);
            Auction auction = new Auction("AUC-" + System.currentTimeMillis(), item, price, 10.0,
                    java.time.LocalDateTime.now().plusMinutes(dur));
            auction.setStatus(AuctionStatus.RUNNING);
            auction.addObserver(server);
            manager.registerAuction(auction);

            clientHandler.sendData(Protocol.REQ_CREATE_ITEM + Protocol.DELIMITER + Protocol.RES_SUCCESS);
            server.broadcastAuctionList();

            String startMessage = Protocol.BROADCAST_AUCTION_START + Protocol.DELIMITER + auction.getId()
                    + Protocol.DELIMITER + dur;
            server.broadcast(startMessage);
            LOGGER.info(String.format("📢 [BROADCAST] Phiên đấu giá bắt đầu: %s | Thời gian: %d phút", auction.getId(),
                    dur));
        } catch (Exception e) {
            clientHandler.sendData(Protocol.REQ_CREATE_ITEM + Protocol.DELIMITER + Protocol.RES_FAIL);
        }
    }

    public void handleUpdateItem(String auctionId, String newName, String newDesc, String newType, String newPriceStr,
            String newDurStr) {
        try {
            if (getUser() instanceof model.user.Admin) {
                double newPrice = Double.parseDouble(newPriceStr);
                int newDur = Integer.parseInt(newDurStr);
                if (service.AdminService.getInstance().updateAuctionForce(auctionId, newName, newDesc, newType,
                        newPrice, newDur)) {
                    clientHandler.sendData(Protocol.REQ_UPDATE_ITEM + Protocol.DELIMITER + Protocol.RES_SUCCESS);
                    server.broadcastAuctionList();
                } else {
                    clientHandler.sendData(Protocol.REQ_UPDATE_ITEM + Protocol.DELIMITER + Protocol.RES_FAIL
                            + Protocol.DELIMITER + "Lỗi cập nhật (không tìm thấy phiên).");
                }
            } else if (getUser() instanceof Seller) {
                if (manager.updateAuctionBySeller(auctionId, getUser().getId(), newName, newDesc, newType)) {
                    clientHandler.sendData(Protocol.REQ_UPDATE_ITEM + Protocol.DELIMITER + Protocol.RES_SUCCESS);
                    server.broadcastAuctionList();
                } else {
                    clientHandler.sendData(Protocol.REQ_UPDATE_ITEM + Protocol.DELIMITER + Protocol.RES_FAIL
                            + Protocol.DELIMITER + "Không thể sửa (đã có người đặt giá hoặc sai quyền).");
                }
            } else {
                clientHandler.sendData(Protocol.REQ_UPDATE_ITEM + Protocol.DELIMITER + Protocol.RES_FAIL
                        + Protocol.DELIMITER + "Bạn không có quyền thực hiện!");
            }
        } catch (Exception e) {
            clientHandler.sendData(Protocol.REQ_UPDATE_ITEM + Protocol.DELIMITER + Protocol.RES_FAIL
                    + Protocol.DELIMITER + "Dữ liệu không hợp lệ.");
        }
    }

    public void handleDeleteItem(String auctionId) {
        if (getUser() instanceof Seller) {
            if (manager.deleteAuctionBySeller(auctionId, getUser().getId())) {
                clientHandler.sendData(Protocol.REQ_DELETE_ITEM + Protocol.DELIMITER + Protocol.RES_SUCCESS);
                server.broadcastAuctionList();
            } else {
                clientHandler.sendData(Protocol.REQ_DELETE_ITEM + Protocol.DELIMITER + Protocol.RES_FAIL
                        + Protocol.DELIMITER + "Không thể xóa (đã có người đặt giá hoặc sai quyền).");
            }
        } else {
            clientHandler.sendData(Protocol.REQ_DELETE_ITEM + Protocol.DELIMITER + Protocol.RES_FAIL
                    + Protocol.DELIMITER + "Chỉ người bán mới được xóa!");
        }
    }

    public void handleBid(String auctionId, String amountStr) {
        try {
            if (!(getUser() instanceof Bidder)) {
                clientHandler.sendData(Protocol.REQ_BID + Protocol.DELIMITER + Protocol.RES_FAIL + Protocol.DELIMITER
                        + "Chỉ người mua mới được đặt giá!");
                return;
            }
            double amount = Double.parseDouble(amountStr);
            Bidder bidder = (Bidder) getUser();
            Auction auction = manager.getAuctionById(auctionId);
            if (auction == null) {
                clientHandler.sendData(Protocol.REQ_BID + Protocol.DELIMITER + Protocol.RES_FAIL + Protocol.DELIMITER
                        + "Không tìm thấy sản phẩm!");
                return;
            }
            String result = manager.processBid(bidder, auction, amount);
            if (result.equals("Thành công!")) {
                clientHandler.sendData(Protocol.REQ_BID + Protocol.DELIMITER + Protocol.RES_SUCCESS);
                clientHandler.sendData(Protocol.RES_UPDATE_BALANCE + Protocol.DELIMITER + bidder.getAvailableBalance());
            } else {
                clientHandler.sendData(
                        Protocol.REQ_BID + Protocol.DELIMITER + Protocol.RES_FAIL + Protocol.DELIMITER + result);
            }
        } catch (Exception e) {
            clientHandler.sendData(Protocol.REQ_BID + Protocol.DELIMITER + Protocol.RES_FAIL + Protocol.DELIMITER
                    + "Lỗi hệ thống đặt giá.");
        }
    }

    public void handleAutoBid(String auctionId, String maxBidStr, String incrementStr) {
        try {
            if (!(getUser() instanceof Bidder)) {
                clientHandler.sendData(Protocol.REQ_AUTOBID + Protocol.DELIMITER + Protocol.RES_FAIL
                        + Protocol.DELIMITER + "Chỉ người mua mới được đặt auto-bid!");
                return;
            }
            Bidder bidder = (Bidder) getUser();
            Auction auction = manager.getAuctionById(auctionId);
            if (auction == null) {
                clientHandler.sendData(Protocol.REQ_AUTOBID + Protocol.DELIMITER + Protocol.RES_FAIL
                        + Protocol.DELIMITER + "Không tìm thấy sản phẩm!");
                return;
            }
            if ("CANCEL".equalsIgnoreCase(maxBidStr)) {
                auction.cancelAutoBid(bidder);
                clientHandler.sendData(Protocol.REQ_AUTOBID + Protocol.DELIMITER + Protocol.RES_SUCCESS
                        + Protocol.DELIMITER + "CANCEL");
                return;
            }
            double maxBid = Double.parseDouble(maxBidStr);
            double increment = 0.0;
            if (incrementStr != null && !incrementStr.trim().isEmpty()) {
                increment = Double.parseDouble(incrementStr);
            }
            auction.registerAutoBid(bidder, maxBid, increment);
            clientHandler.sendData(
                    Protocol.REQ_AUTOBID + Protocol.DELIMITER + Protocol.RES_SUCCESS + Protocol.DELIMITER + "REGISTER");
        } catch (Exception e) {
            clientHandler.sendData(Protocol.REQ_AUTOBID + Protocol.DELIMITER + Protocol.RES_FAIL + Protocol.DELIMITER
                    + "Lỗi hệ thống auto-bid.");
        }
    }

    public void handleJoinRoom(String auctionId) {
        if (!clientHandler.getCurrentRoomIds().contains(auctionId)) {
            clientHandler.getCurrentRoomIds().add(auctionId);
            server.joinRoom(auctionId, clientHandler);
            LOGGER.info(String.format("👤 User %s đã vào phòng %s",
                    (getUser() != null ? getUser().getUsername() : "Guest"), auctionId));
        }
    }

    public void handleLeaveRoom(String auctionId) {
        if (clientHandler.getCurrentRoomIds().contains(auctionId)) {
            server.leaveRoom(auctionId, clientHandler);
            clientHandler.getCurrentRoomIds().remove(auctionId);
            LOGGER.info(String.format("👤 User %s đã rời phòng %s",
                    (getUser() != null ? getUser().getUsername() : "Guest"), auctionId));
        }
    }
}
