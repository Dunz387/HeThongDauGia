package model.item;
import model.user.Seller;

public class Arts extends Item {

    private String artist;
    private int creationYear;

    private boolean isAuthenticated;

    public Arts(String id, String name, String description, Seller owner, String artist, int creationYear) {
        super(id, name, description, owner);
        this.artist = artist;
        this.creationYear = creationYear;
        this.isAuthenticated = false;
    }


    public String getArtist() { return artist; }
    public void setArtist(String artist) { this.artist = artist; }

    public int getCreationYear() { return creationYear; }
    public void setCreationYear(int creationYear) { this.creationYear = creationYear; }

    public boolean isAuthenticated() { return isAuthenticated; }
    public void setAuthenticated(boolean authenticated) { isAuthenticated = authenticated; }

    @Override
    public String getDetails() {
        String authStatus = isAuthenticated ? "[Đã giám định]" : "[Chưa giám định]";
        return String.format("[Nghệ thuật] Tác giả: %s | Năm: %d %s", artist, creationYear, authStatus);
    }
}
