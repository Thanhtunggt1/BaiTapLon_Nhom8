public class Seller extends User {
    public Seller(String id, String username, String password) {
        super(id, username, password, "SELLER");
    }

    public void addItem(Item item) {}

    @Override
    public void printInfo() { System.out.println("Seller: " + username); }
}
