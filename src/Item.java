import java.util.List;
import java.util.ArrayList;
// Lớp trừu tượng Item (Subject thông báo cho Observers)
public abstract class Item extends Entity implements Subject {
    protected String name;
    protected String description;
    protected double startingPrice;
    protected double currentHighestBid;
    protected ItemStatus status;
    protected Seller seller;
    protected Bidder winner;

    private List<Observer> observers;
    private List<BidTransaction> bids;

    public Item(String id, String name, String description, double startingPrice, Seller seller) {
        super(id);
        this.name = name;
        this.description = description;
        this.startingPrice = startingPrice;
        this.currentHighestBid = startingPrice; // Ban đầu giá cao nhất là giá khởi điểm
        this.status = ItemStatus.OPEN;
        this.seller = seller;
        this.observers = new ArrayList<>();
        this.bids = new ArrayList<>();
    }

    public void addBid(BidTransaction bid) {
        this.bids.add(bid);
        this.currentHighestBid = bid.getAmount();
        this.winner = bid.getBidder();
        notifyObservers("Giá mới cho sản phẩm " + name + " là: " + currentHighestBid);
    }

    // Triển khai Subject (Observer Pattern)
    @Override
    public void addObserver(Observer o) { observers.add(o); }
    @Override
    public void removeObserver(Observer o) { observers.remove(o); }
    @Override
    public void notifyObservers(String message) {
        for (Observer o : observers) { o.update(message); }
    }

    public double getCurrentHighestBid() { return currentHighestBid; }
    public ItemStatus getStatus() { return status; }
    public void setStatus(ItemStatus status) { this.status = status; }

    public abstract void printInfo();
}