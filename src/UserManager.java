import java.util.ArrayList;
public class UserManager {
    private ArrayList<User> users;

    //constructor
    public UserManager() {
        users = new ArrayList<>();
    }

    //register
    public boolean register(String username, String password, Role role) {
        for (User u : users) {
            if(u.getUsername().equals(username)) {
                return false;
            }
        }

        //tao user moi
        User newUser = new User(username, password, role);
        users.add(newUser);
        return true;
    }

    //log in
    public User login(String username, String password) {
        for (User u : users) {
            if(u.getUsername().equals(username) && u.getPassword().equals(password)) {
                return u; //log in successfully
            }
        }
        return null; // not correct
    }

    //User get lists

    public ArrayList<User> getUsers() {
        return users;
    }
}