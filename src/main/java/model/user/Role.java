package model.user;

public enum Role {
    ADMIN("Quản trị viên"),
    SELLER("Người bán"),
    BIDDER("Người mua");

    private final String displayName;

    Role(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
