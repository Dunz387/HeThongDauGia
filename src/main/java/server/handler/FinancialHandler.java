package server.handler;

import model.user.Bidder;
import model.user.Seller;
import model.user.User;
import shared.Protocol;

import java.util.logging.Logger;

/**
 * Xử lý các request giao dịch tài chính: Deposit, Withdraw.
 * Tách từ ClientHandler để tuân thủ SRP.
 */
public class FinancialHandler {
    private static final Logger LOGGER = Logger.getLogger(FinancialHandler.class.getName());
    private final server.ClientHandler clientHandler;

    public FinancialHandler(server.ClientHandler clientHandler) {
        this.clientHandler = clientHandler;
    }

    private User getUser() { return clientHandler.getLoggedInUser(); }

    public void handleDeposit(String amountStr) {
        try {
            double amount = Double.parseDouble(amountStr);
            if (getUser() instanceof Bidder) {
                Bidder bidder = (Bidder) getUser();
                bidder.addBalance(amount);
                dao.UserDAO.updateUserBalance(bidder.getId(), bidder.getBalance());
                clientHandler.sendData(Protocol.REQ_DEPOSIT + Protocol.DELIMITER + Protocol.RES_SUCCESS);
                clientHandler.sendData(Protocol.RES_UPDATE_BALANCE + Protocol.DELIMITER + bidder.getAvailableBalance());
            } else {
                clientHandler.sendData(Protocol.REQ_DEPOSIT + Protocol.DELIMITER + Protocol.RES_FAIL + Protocol.DELIMITER + "Chỉ Bidder mới có thể nạp tiền!");
            }
        } catch (Exception e) {
            clientHandler.sendData(Protocol.REQ_DEPOSIT + Protocol.DELIMITER + Protocol.RES_FAIL + Protocol.DELIMITER + "Số tiền không hợp lệ.");
        }
    }

    public void handleWithdraw(String amountStr) {
        try {
            double amount = Double.parseDouble(amountStr);
            if (getUser() instanceof Seller) {
                Seller seller = (Seller) getUser();
                if (seller.deductBalance(amount)) {
                    dao.UserDAO.updateUserBalance(seller.getId(), seller.getBalance());
                    clientHandler.sendData(Protocol.REQ_WITHDRAW + Protocol.DELIMITER + Protocol.RES_SUCCESS);
                    clientHandler.sendData(Protocol.RES_UPDATE_BALANCE + Protocol.DELIMITER + seller.getBalance());
                } else {
                    clientHandler.sendData(Protocol.REQ_WITHDRAW + Protocol.DELIMITER + Protocol.RES_FAIL + Protocol.DELIMITER + "Số dư không đủ để rút!");
                }
            } else {
                clientHandler.sendData(Protocol.REQ_WITHDRAW + Protocol.DELIMITER + Protocol.RES_FAIL + Protocol.DELIMITER + "Chỉ Seller mới có thể rút tiền!");
            }
        } catch (Exception e) {
            clientHandler.sendData(Protocol.REQ_WITHDRAW + Protocol.DELIMITER + Protocol.RES_FAIL + Protocol.DELIMITER + "Số tiền không hợp lệ.");
        }
    }
}
