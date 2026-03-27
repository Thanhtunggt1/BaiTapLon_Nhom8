public class Main {
    public static void main(String[] args) {
        // 1. Khởi tạo trình quản lý đấu giá (Singleton)
        AuctionManager manager = AuctionManager.getInstance();

        // 2. Tạo người dùng (Seller và Bidder)
        Seller seller1 = new Seller("S01", "nguoiban_so1", "123456");
        Bidder bidderBob = new Bidder("B01", "Bob", "pass123");
        Bidder bidderAlice = new Bidder("B02", "Alice", "pass456");

        System.out.println("--- KHỞI TẠO HỆ THỐNG ---");
        seller1.printInfo();
        bidderBob.printInfo();
        bidderAlice.printInfo();

        // 3. Seller thêm sản phẩm vào hệ thống (Dùng Factory Method)
        Item laptop = ItemFactory.createItem("ELECTRONICS", "ITEM_001", "MacBook Pro M3", "Hàng mới nguyên seal", 1000.0, seller1);
        manager.addItem(laptop);
        System.out.println("\nSản phẩm '" + laptop.name + "' đã được lên sàn với giá khởi điểm: $" + laptop.startingPrice);

        // 4. Các Bidder "đăng ký" theo dõi sản phẩm (Observer Pattern)
        // Khi có giá mới, Bob và Alice sẽ tự động nhận được thông báo
        laptop.addObserver(bidderBob);
        laptop.addObserver(bidderAlice);

        // 5. Mở phiên đấu giá
        System.out.println("\n--- BẮT ĐẦU PHIÊN ĐẤU GIÁ ---");
        manager.startAuction("ITEM_001");

        // 6. Bắt đầu trả giá (Test các logic ngoại lệ)
        try {
            System.out.println("\n[Hành động] Bob đặt giá $1200:");
            manager.handleBid(bidderBob, "ITEM_001", 1200.0);

            System.out.println("\n[Hành động] Alice đặt giá $1500:");
            manager.handleBid(bidderAlice, "ITEM_001", 1500.0);

            System.out.println("\n[Hành động] Bob cố tình đặt giá thấp hơn hiện tại ($1300):");
            manager.handleBid(bidderBob, "ITEM_001", 1300.0);

        } catch (Exception e) {
            // Demo Xử lý lỗi & ngoại lệ (Yêu cầu 3.1.5 của bài tập)
            System.out.println(">> LỖI HỆ THỐNG TỪ CHỐI: " + e.getMessage());
        }

        // 7. Kết thúc phiên đấu giá
        System.out.println("\n--- KẾT THÚC PHIÊN ĐẤU GIÁ ---");
        manager.endAuction("ITEM_001");
    }
}