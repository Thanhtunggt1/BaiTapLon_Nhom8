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

    public abstract void printInfo();

    public String getUsername() { return username; }
}