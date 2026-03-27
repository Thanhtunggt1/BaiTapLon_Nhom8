// Lớp Seller (đăng sản phẩm)
public class Seller extends User {
    public Seller(String id, String username, String password) {
        super(id, username, password, "SELLER");
    }

    public void addItem(Item item) { /* Logic thêm SP */ }

    @Override
    public void printInfo() { System.out.println("Seller: " + username); }
}
