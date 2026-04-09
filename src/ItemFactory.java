import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
public class ItemFactory {
    public static Item createItem(String type, String id, String name, String description, double startingPrice, Seller seller) {
        switch (type.toUpperCase()) {
            case "ELECTRONICS":
                return new Electronics(id, name, description, startingPrice, seller);
            case "ART":
                return new Art(id, name, description, startingPrice, seller);
            case "VEHICLE":
                return new Vehicle(id, name, description, startingPrice, seller);
            default:
                throw new IllegalArgumentException("Loại sản phẩm không hợp lệ");
        }
    }
}