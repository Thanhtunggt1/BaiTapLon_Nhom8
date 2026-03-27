import java.util.Date;
public class BidTransaction {
    private String id;
    private Bidder bidder;
    private Item item;
    private double amount;
    private Date timestamp;

    public BidTransaction(String id, Bidder bidder, Item item, double amount) {
        this.id = id;
        this.bidder = bidder;
        this.item = item;
        this.amount = amount;
        this.timestamp = new Date();
    }

    public double getAmount() { return amount; }
    public Bidder getBidder() { return bidder; }
}