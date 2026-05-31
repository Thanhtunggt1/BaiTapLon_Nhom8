# 🏛️ Hệ Thống Đấu Giá Trực Tuyến

Ứng dụng đấu giá trực tuyến theo mô hình Client–Server, hỗ trợ nhiều người dùng đồng thời. Người bán có thể đăng sản phẩm và tạo phiên đấu giá; người mua có thể đặt giá thủ công hoặc cài đặt auto-bid; quản trị viên có thể giám sát và can thiệp toàn bộ hệ thống.

---

## 📎 Tài liệu & Demo

- 📄 **Báo cáo PDF**: [Xem tại đây](https://drive.google.com/file/d/1ss415eY6kFJhu7jdfxVhOed0ns8aNDdm/view?usp=sharing)
- 🎬 **Video Demo**: [Xem tại đây](https://drive.google.com/file/d/1C85vLbSDAdghZcbtVMGfDBiuKuQUg9IC/view?usp=sharing)

---

## 🛠️ Công nghệ sử dụng

| Thành phần | Công nghệ |
|---|---|
| Ngôn ngữ | Java 17 |
| Giao diện | JavaFX 21.0.2 + FXML |
| Mạng | Java Socket (TCP) |
| Cơ sở dữ liệu | MySQL 8 |
| Serialization | Gson 2.10.1 |
| Build tool | Maven 3.x |
| Đóng gói | maven-shade-plugin 3.5.1 (Fat JAR) |

### Yêu cầu cài đặt

- **JDK 17** trở lên — [Tải tại đây](https://adoptium.net/)
- **MySQL 8** đang chạy tại `localhost:3306`, database tên `auction_db`
- **Maven 3.x** (hoặc dùng `mvnw` đi kèm project)

---

## 📁 Cấu trúc thư mục

```
BaiTapNhom8/
├── src/
│   ├── main/
│   │   ├── java/com/auction/
│   │   │   ├── Main.java                  # Entry point JavaFX
│   │   │   ├── Launcher.java              # Wrapper khởi động client
│   │   │   ├── gui/
│   │   │   │   ├── controller/            # Các controller màn hình
│   │   │   │   │   ├── LoginController
│   │   │   │   │   ├── MainController
│   │   │   │   │   ├── AuctionListController
│   │   │   │   │   ├── AuctionDetailController
│   │   │   │   │   ├── SellerController
│   │   │   │   │   └── AdminController
│   │   │   │   └── SessionManager.java
│   │   │   ├── model/
│   │   │   │   ├── entity/                # Auction, Bidder, Seller, Item...
│   │   │   │   └── enums/                 # AuctionStatus, ItemType
│   │   │   ├── network/
│   │   │   │   ├── NetworkClient.java     # Client kết nối TCP
│   │   │   │   ├── Message.java           # Gói tin JSON
│   │   │   │   ├── MessageType.java       # Enum loại tin nhắn
│   │   │   │   └── dto/                   # Data Transfer Objects
│   │   │   ├── server/
│   │   │   │   ├── AuctionServer.java     # Server TCP chính
│   │   │   │   ├── ClientHandler.java     # Xử lý từng client
│   │   │   │   ├── DataLoader.java        # Load dữ liệu từ DB vào RAM
│   │   │   │   ├── DatabaseConnection.java
│   │   │   │   ├── AuctionDAO.java
│   │   │   │   ├── UserDAO.java
│   │   │   │   ├── ItemDAO.java
│   │   │   │   └── BidTransactionDAO.java
│   │   │   ├── manager/
│   │   │   │   └── AuctionManager.java    # Singleton quản lý phiên đấu giá
│   │   │   ├── pattern/
│   │   │   │   ├── factory/ItemFactory.java
│   │   │   │   └── observer/              # Observer pattern
│   │   │   └── exception/                 # Custom exceptions
│   │   └── resources/com/auction/gui/
│   │       ├── login.fxml
│   │       ├── main.fxml
│   │       ├── auction_list.fxml
│   │       ├── auction_detail.fxml
│   │       ├── seller.fxml
│   │       ├── admin.fxml
│   │       └── style.css
│   └── test/                              # Unit tests
├── target/
│   ├── server.jar                         # ← Fat JAR chạy Server
│   └── client.jar                         # ← Fat JAR chạy Client
└── pom.xml
```

---

## 📦 Vị trí file JAR

Sau khi build, 2 file JAR nằm tại:

```
target/server.jar   →  Máy chủ đấu giá
target/client.jar   →  Giao diện người dùng
```

---

## 🚀 Hướng dẫn chạy

### Bước 1 — Build project

Mở terminal tại thư mục gốc project, chạy:

```bash
mvn clean package
```

Hoặc trong **IntelliJ IDEA**: Maven panel → bấm đúp **`package`**.

### Bước 2 — Khởi động Server (chạy 1 lần duy nhất)

```bash
java -jar target/server.jar
```

> Server lắng nghe tại cổng **9999**. Đảm bảo MySQL đang chạy trước khi khởi động server.

### Bước 3 — Chạy Client

Mở **cửa sổ terminal mới**, chạy:

```bash
java -jar target/client.jar
```

> Để chạy **nhiều client cùng lúc**, mở thêm cửa sổ terminal và lặp lại lệnh trên.

---

## ✅ Danh sách chức năng đã hoàn thành

### 👤 Xác thực người dùng
- [x] Đăng ký tài khoản (Bidder / Seller)
- [x] Đăng nhập / Đăng xuất
- [x] Phân quyền 3 vai trò: Admin, Seller, Bidder

### 🛍️ Seller — Người bán
- [x] Tạo sản phẩm (3 loại: Electronics, Art, Vehicle) kèm ảnh
- [x] Chỉnh sửa / Xóa sản phẩm (khi chưa trong phiên đấu giá)
- [x] Tạo phiên đấu giá cho sản phẩm
- [x] Kết thúc phiên đấu giá sớm

### 🔨 Bidder — Người đấu giá
- [x] Xem danh sách phiên đấu giá, lọc theo trạng thái
- [x] Xem chi tiết phiên đấu giá và lịch sử đặt giá
- [x] Đặt giá thủ công
- [x] Cài đặt Auto-bid (tự động đặt giá theo bước tăng đến mức tối đa)
- [x] Nạp tiền vào tài khoản (có xác thực mật khẩu)
- [x] Thanh toán phiên đấu giá thắng
- [x] Nhận cảnh báo nếu không thanh toán trong 12 giờ

### 🔧 Admin — Quản trị viên
- [x] Xem toàn bộ phiên đấu giá trong hệ thống
- [x] Hủy phiên đấu giá bất kỳ
- [x] Kết thúc phiên đấu giá sớm
- [x] Nâng cấp quyền người dùng lên Admin

### ⚙️ Hệ thống
- [x] Anti-snipe: tự động gia hạn 60 giây nếu có bid trong 30 giây cuối
- [x] Tự động đóng phiên khi hết giờ (scheduler 5 giây/lần)
- [x] Tự động hủy & phạt Bidder không thanh toán sau 12 giờ
- [x] Khóa tài khoản Bidder sau 3 lần vi phạm không thanh toán
- [x] Khóa chức năng nạp tiền 3 phút sau 3 lần nhập sai mật khẩu
- [x] Realtime cập nhật giá đến tất cả client qua BID_UPDATE broadcast
- [x] Observer pattern: Bidder nhận thông báo khi có cập nhật phiên
- [x] Factory pattern: tạo Item theo loại động
- [x] Singleton: AuctionManager, ItemFactory, NetworkClient
