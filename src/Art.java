public class Art extends Item {
    public Art(String id, String name, String description, double startingPrice, Seller seller) {
        super(id, name, description, startingPrice, seller);
    }
    @Override
    public void printInfo() { System.out.println("Art Item: " + name); }
}