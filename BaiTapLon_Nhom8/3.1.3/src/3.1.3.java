import java.util.*;

// 🧱 1. Bid (lượt đặt giá)
class Bid {
    private String userId;
    private double amount;
    private long time;

    public Bid(String userId, double amount) {
        this.userId = userId;
        this.amount = amount;
        this.time = System.currentTimeMillis();
    }

    public String getUserId() {
        return userId;
    }

    public double getAmount() {
        return amount;
    }

    public long getTime() {
        return time;
    }
}

// 🧱 2. Auction (phiên đấu giá)
class Auction {
    private String productName;
    private double currentPrice;
    private long endTime;
    private List<Bid> bids;

    public Auction(String productName, double startPrice, long durationMillis) {
        this.productName = productName;
        this.currentPrice = startPrice;
        this.endTime = System.currentTimeMillis() + durationMillis;
        this.bids = new ArrayList<>();
    }

    public synchronized boolean placeBid(String userId, double amount) {
        if (System.currentTimeMillis() > endTime) {
            return false;
        }

        if (amount <= currentPrice) {
            return false;
        }

        Bid bid = new Bid(userId, amount);
        bids.add(bid);
        currentPrice = amount;
        return true;
    }

    public synchronized Bid getHighestBid() {
        if (bids.isEmpty()) return null;
        return bids.get(bids.size() - 1);
    }

    public boolean isFinished() {
        return System.currentTimeMillis() > endTime;
    }

    public void showInfo() {
        System.out.println("\nSản phẩm: " + productName);
        System.out.println("Giá hiện tại: " + currentPrice);
    }
}

// 🚀 3. Main (chạy chương trình + Thread)
class Main {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);

        Auction auction = new Auction("iPhone", 1000, 20000); // 20 giây

        // 🧵 Thread đếm thời gian
        Thread timerThread = new Thread(() -> {
            while (!auction.isFinished()) {
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {}
            }

            System.out.println("\n=== HẾT THỜI GIAN ===");

            Bid winner = auction.getHighestBid();
            if (winner != null) {
                System.out.println("Người thắng: " + winner.getUserId());
                System.out.println("Giá: " + winner.getAmount());
            } else {
                System.out.println("Không có ai đặt giá");
            }

            System.exit(0); // dừng luôn
        });

        timerThread.start();

        // 🧵 Luồng nhập (main thread)
        while (true) {
            auction.showInfo();

            System.out.print("Nhập userId: ");
            String userId = sc.nextLine();

            System.out.print("Nhập giá muốn đặt: ");
            double amount = sc.nextDouble();
            sc.nextLine(); // fix lỗi enter

            boolean success = auction.placeBid(userId, amount);

            if (success) {
                System.out.println("Đặt giá thành công!");
            } else {
                System.out.println("Đặt giá thất bại!");
            }

            System.out.println("----------------------");
        }
    }
}