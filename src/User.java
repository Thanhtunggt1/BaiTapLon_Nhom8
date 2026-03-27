public abstract class User extends Entity {
    protected String username;
    protected String password;
    protected String role;

    public User(String id, String username, String password, String role) {
        super(id);
        this.username = username;
        this.password = password;
        this.role = role;
    }

    // Abstract method cho tính đa hình
    public abstract void printInfo();

    // Getters and Setters
    public String getUsername() { return username; }
}