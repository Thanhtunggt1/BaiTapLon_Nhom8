public class Main {
    public static void main(String[] args) {
        AuctionManager manager = AuctionManager.getInstance();

        Seller seller1 = new Seller("S01", "nguoiban_so1", "123456");
        Bidder bidderBob = new Bidder("B01", "Bob", "pass123");
        Bidder bidderAlice = new Bidder("B02", "Alice", "pass456");

        System.out.println("KHỞI TẠO HỆ THỐNG");
        seller1.printInfo();
        bidderBob.printInfo();
        bidderAlice.printInfo();

        Item laptop = ItemFactory.createItem("ELECTRONICS", "ITEM_001", "MacBook Pro M3", "Hàng mới nguyên seal", 1000.0, seller1);
        manager.addItem(laptop);
        System.out.println("\nSản phẩm '" + laptop.name + "' đã được lên sàn với giá khởi điểm: $" + laptop.startingPrice);

        laptop.addObserver(bidderBob);
        laptop.addObserver(bidderAlice);

        System.out.println("\nBẮT ĐẦU PHIÊN ĐẤU GIÁ");
        manager.startAuction("ITEM_001");

        try {
            System.out.println("\n[Hành động] Bob đặt giá $1200:");
            manager.handleBid(bidderBob, "ITEM_001", 1200.0);

            System.out.println("\n[Hành động] Alice đặt giá $1500:");
            manager.handleBid(bidderAlice, "ITEM_001", 1500.0);

            System.out.println("\n[Hành động] Bob cố tình đặt giá thấp hơn hiện tại ($1300):");
            manager.handleBid(bidderBob, "ITEM_001", 1300.0);

        } catch (Exception e) {
            System.out.println(">> LỖI HỆ THỐNG TỪ CHỐI: " + e.getMessage());
        }

        System.out.println("\nKẾT THÚC PHIÊN ĐẤU GIÁ");
        manager.endAuction("ITEM_001");
    }
}