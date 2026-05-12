package view.utility;

import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.scene.control.TableColumn;
import model.auction.Auction;

import java.time.format.DateTimeFormatter;

/**
 * Cấu hình thống nhất 9 cột bảng Auction cho tất cả các view (SRP + OCP).
 * Thay vì mỗi controller tự cấu hình bảng, gọi 1 method duy nhất.
 * Khi cần thêm/sửa cột, chỉ cần sửa 1 file duy nhất.
 */
public class AuctionTableConfigurator {

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    /**
     * Cấu hình đầy đủ 9 cột bảng Auction thống nhất.
     * Dùng cho BaseMenu, AssertsList, RoomMenuChoice.
     */
    public static void configure(
            TableColumn<Auction, String> colId,
            TableColumn<Auction, String> colName,
            TableColumn<Auction, String> colType,
            TableColumn<Auction, Double> colPrice,
            TableColumn<Auction, Integer> colBidCount,
            TableColumn<Auction, String> colHighestBidder,
            TableColumn<Auction, String> colEndTime,
            TableColumn<Auction, String> colStatus,
            TableColumn<Auction, String> colSeller
    ) {
        if (colId != null)
            colId.setCellValueFactory(cellData ->
                    new SimpleStringProperty(cellData.getValue().getId()));

        if (colName != null)
            colName.setCellValueFactory(cellData ->
                    new SimpleStringProperty(cellData.getValue().getItem().getName()));

        if (colType != null)
            colType.setCellValueFactory(cellData ->
                    new SimpleStringProperty(
                            StatusDisplayHelper.formatItemType(
                                    cellData.getValue().getItem().getClass().getSimpleName())));

        if (colPrice != null)
            colPrice.setCellValueFactory(cellData ->
                    new SimpleDoubleProperty(cellData.getValue().getCurrentPrice()).asObject());

        if (colBidCount != null)
            colBidCount.setCellValueFactory(cellData ->
                    new SimpleIntegerProperty(cellData.getValue().getBidHistory().size()).asObject());

        if (colHighestBidder != null)
            colHighestBidder.setCellValueFactory(cellData -> {
                var bidder = cellData.getValue().getHighestBidder();
                return new SimpleStringProperty(bidder != null ? bidder.getUsername() : "Chưa có");
            });

        if (colEndTime != null)
            colEndTime.setCellValueFactory(cellData -> {
                var endTime = cellData.getValue().getEndTime();
                return new SimpleStringProperty(endTime != null ? endTime.format(TIME_FMT) : "—");
            });

        if (colStatus != null)
            colStatus.setCellValueFactory(cellData ->
                    new SimpleStringProperty(
                            StatusDisplayHelper.formatAuctionStatus(
                                    cellData.getValue().getStatus().name())));

        if (colSeller != null)
            colSeller.setCellValueFactory(cellData -> {
                var owner = cellData.getValue().getItem().getOwner();
                return new SimpleStringProperty(owner != null ? owner.getUsername() : "—");
            });
    }
}
