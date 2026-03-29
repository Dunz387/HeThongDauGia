package dao;

import model.User;
public class UserDAO extends EntityDAO<User>{
    //Tìm theo tên
    public User findbyUsername (String username){
        if (username == null) return null;

        for (User user: entities) {
            if (username.equalsIgnoreCase(user.getUsername())) {
                return user;
            }
        }
        return null;
    }
    public User findByEmail (String email){
        if (email == null) return null;

        for (User user: entities) {
            if (email.equalsIgnoreCase(user.getEmail())){
                return user;
            }
        }
        return null;
    }
    public User login(String username, String password){
        User user = findbyUsername(username);

        if (user != null && user.getPassword().equals(password)){
            return user;
        }
        return null;
    }
}