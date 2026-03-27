public class Electronics extends Item {
    public Electronics(String id, String name, String description, double startingPrice, Seller seller) {
        super(id, name, description, startingPrice, seller);
    }
    @Override
    public void printInfo() { System.out.println("Electronics Item: " + name); }
}