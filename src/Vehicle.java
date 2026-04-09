public class Vehicle extends Item {
    public Vehicle(String id, String name, String description, double startingPrice, Seller seller) {
        super(id, name, description, startingPrice, seller);
    }
    @Override
    public void printInfo() { System.out.println("Vehicle Item: " + name); }
}