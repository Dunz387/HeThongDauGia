package service.auction;

import model.auction.Auction;
import model.item.Item;
import model.item.ItemBuilder;

import java.lang.reflect.Field;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class AuctionItemUpdater {
    private static final Logger LOGGER = Logger.getLogger(AuctionItemUpdater.class.getName());

    private AuctionItemUpdater() {
    }

    public static void updateItem(Auction auction, String newName, String newDesc, String newType) {
        auction.getItem().setName(newName);
        auction.getItem().setDescription(newDesc);

        if (!auction.getItem().getTypeString().equalsIgnoreCase(newType)) {
            replaceItem(auction, newName, newDesc, newType);
        }
    }

    private static void replaceItem(Auction auction, String newName, String newDesc, String newType) {
        Item newItem = new ItemBuilder()
                .setType(newType)
                .setId(auction.getItem().getId())
                .setName(newName)
                .setDescription(newDesc)
                .setOwner(auction.getItem().getOwner())
                .build();
        try {
            Field itemField = Auction.class.getDeclaredField("item");
            itemField.setAccessible(true);
            itemField.set(auction, newItem);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Lỗi đổi loại sản phẩm", e);
        }
    }
}
