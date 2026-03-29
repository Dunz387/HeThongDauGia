package model;

public class Seller extends User {

    public Seller(String username, String password, String email,
                  String fullName, String phone,
                  String address, String city, String country) {

        super(username, password, email, fullName, phone, address, city, country);
    }

    @Override
    public String getRole() {
        return "SELLER";
    }
}