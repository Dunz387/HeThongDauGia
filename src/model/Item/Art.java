package model.Item;

import model.Item.Item;
import model.UserType.Seller;

public class Art extends Item {

    private String artist;
    private int year;

    public Art(String name, String description, double startPrice,
               Seller seller,
               double buyNowPrice, double reservePrice,
               String condition, int quantity,
               String artist, int year) {

        super(name, description, startPrice, seller,
              buyNowPrice, reservePrice,
              condition, quantity);

        this.artist = artist;
        this.year = year;
    }

    public String getArtist() { return artist; }
    public int getYear() { return year; }

    @Override
    public String getItemType() {
        return "ART";
    }

    @Override
    public void printInfo() {
        super.printInfo();
        System.out.println("Artist: " + artist);
        System.out.println("Year: " + year);
    }
}
