public class Bidder extends User implements Observer {
    private double maxBid;
    private double increment;

    public Bidder(String id, String username, String password) {
        super(id, username, password, "BIDDER");
    }

    // Auto-bidding config
    public void setAutoBidConfig(double maxBid, double increment) {
        this.maxBid = maxBid;
        this.increment = increment;
    }

    public void placeBid(Item item, double amount) {
    }

    @Override
    public void update(String message) {
        System.out.println("Thông báo tới " + username + ": " + message);
    }

    @Override
    public void printInfo() {
        System.out.println("Bidder: " + username);
    }
}