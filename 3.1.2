import java.time.LocalDateTime;
import java.util.ArrayList;

enum ItemType {
    ELECTRONICS,
    ART,
    VEHICLE,
    OTHER
}
class Item {
    private String id;
    private String name;
    private String description;
    private double startingPrice;
    private double currentMaxBid;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private ItemType type;
    private String sellerUsername; // Để biết sản phẩm này của ai [cite: 36]

    public Item(String id, String name, String description, double startingPrice,
                LocalDateTime startTime, LocalDateTime endTime, ItemType type, String sellerUsername) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.startingPrice = startingPrice;
        this.currentMaxBid = startingPrice; // Mặc định giá cao nhất lúc đầu là giá khởi điểm [cite: 42]
        this.startTime = startTime;
        this.endTime = endTime;
        this.type = type;
        this.sellerUsername = sellerUsername;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public double getStartingPrice() { return startingPrice; }
    public double getCurrentMaxBid() { return currentMaxBid; }
    public LocalDateTime getStartTime() { return startTime; }
    public LocalDateTime getEndTime() { return endTime; }
    public ItemType getType() { return type; }
    public String getSellerUsername() { return sellerUsername; }

    public void setName(String name) { this.name = name; }
    public void setDescription(String description) { this.description = description; }
    public void setCurrentMaxBid(double currentMaxBid) { this.currentMaxBid = currentMaxBid; }
    public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }
}
class ProductManager {
    private ArrayList<Item> items;

    public ProductManager() {
        this.items = new ArrayList<>();
    }

    // 1. Thêm sản phẩm mới
    public void addItem(Item item) {
        items.add(item);
    }

    // 2. Xóa sản phẩm theo ID
    // Lưu ý: Chỉ nên cho phép xóa nếu phiên đấu giá chưa bắt đầu
    public boolean removeItem(String id) {
        for (Item item : items) {
            if (item.getId().equals(id)) {
                if (LocalDateTime.now().isBefore(item.getStartTime())) {
                    items.remove(item);
                    return true;
                }
            }
        }
        return false;
    }

    // 3. Sửa thông tin sản phẩm
    public boolean updateItem(String id, String newName, String newDesc, LocalDateTime newEnd) {
        for (Item item : items) {
            if (item.getId().equals(id)) {
                // Chỉ cho sửa nếu chưa có ai đặt giá hoặc chưa kết thúc
                item.setName(newName);
                item.setDescription(newDesc);
                item.setEndTime(newEnd);
                return true;
            }
        }
        return false;
    }

    // 4. Lấy danh sách sản phẩm đang diễn ra
    public ArrayList<Item> getActiveItems() {
        ArrayList<Item> activeItems = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        for (Item item : items) {
            if (now.isAfter(item.getStartTime()) && now.isBefore(item.getEndTime())) {
                activeItems.add(item);
            }
        }
        return activeItems;
    }

    // tìm theo tên của sản phẩm
    public ArrayList<Item> searchByName(String keyword) {
        ArrayList<Item> result = new ArrayList<>();
        for (Item item : items) {
            if (item.getName().toLowerCase().contains(keyword.toLowerCase())) {
                result.add(item);
            }
        }
        return result;
    }

    public ArrayList<Item> getAllItems() {
        return items;
    }
}
