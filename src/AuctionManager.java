import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
// Design Pattern: Singleton (Quản lý trung tâm)
public class AuctionManager {
    private static AuctionManager instance;
    private Map<String, Item> itemsMap;

    private AuctionManager() {
        itemsMap = new HashMap<>();
    }

    // Thread-safe Singleton
    public static synchronized AuctionManager getInstance() {
        if (instance == null) {
            instance = new AuctionManager();
        }
        return instance;
    }

    public void addItem(Item item) {
        itemsMap.put(item.getId(), item);
    }

    public Item getItem(String id) {
        return itemsMap.get(id);
    }

    // Xử lý logic đặt giá (cần xử lý đồng bộ - Concurrency)
    public synchronized boolean handleBid(Bidder bidder, String itemId, double amount) throws Exception {
        Item item = itemsMap.get(itemId);
        if (item == null) throw new Exception("Sản phẩm không tồn tại.");
        if (item.getStatus() != ItemStatus.RUNNING) throw new Exception("Phiên đấu giá chưa mở hoặc đã kết thúc.");

        if (amount <= item.getCurrentHighestBid()) {
            throw new Exception("Giá đặt phải cao hơn giá hiện tại.");
        }

        // Tạo giao dịch và cập nhật
        BidTransaction transaction = new BidTransaction(UUID.randomUUID().toString(), bidder, item, amount);
        item.addBid(transaction);

        return true;
    }

    public void startAuction(String itemId) {
        Item item = itemsMap.get(itemId);
        if (item != null) item.setStatus(ItemStatus.RUNNING);
    }

    public void endAuction(String itemId) {
        Item item = itemsMap.get(itemId);
        if (item != null) {
            item.setStatus(ItemStatus.FINISHED);
            item.notifyObservers("Phiên đấu giá " + item.name + " đã kết thúc.");
        }
    }
}