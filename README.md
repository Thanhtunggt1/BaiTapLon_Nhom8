# Phát triển hệ thống đấu giá trực tuyến
## Giới thiệu
Hệ thống đấu giá trực tuyến là một ứng dụng được phát triển bằng Java nhằm mô phỏng hoạt động đấu giá sản phẩm theo thời gian thực. Dự án hỗ trợ nhiều vai trò người dùng như quản trị viên, người bán và người tham gia đấu giá, giúp quản lý sản phẩm, phiên đấu giá và quá trình đặt giá một cách trực quan.
Ứng dụng được xây dựng theo hướng lập trình hướng đối tượng (OOP), sử dụng JavaFX để thiết kế giao diện người dùng và Maven để quản lý thư viện.

---

# Thành viên nhóm

| STT | Thành viên             |
| --- | ---------------------- | 
| 1   | Nguyễn Ngọc Minh Quang |
| 2   | Trần Thanh Tùng        |
| 3   | Nguyễn Quang Minh      |
| 4   | Ngô Hữu Ngọc Tiến      |

---

# Công nghệ sử dụng

* **Ngôn ngữ:** Java
* **GUI:** JavaFX
* **Build Tool:** Maven
* **Mô hình thiết kế:** OOP, MVC
* **IDE khuyến nghị:** IntelliJ IDEA / VS Code / Eclipse
* **Quản lý mã nguồn:** Git & GitHub

---

# Chức năng chính

## Đối với người dùng

* Đăng nhập / đăng ký tài khoản
* Xem danh sách phiên đấu giá
* Xem chi tiết sản phẩm
* Tham gia đấu giá
* Đặt giá trực tiếp
* Theo dõi trạng thái phiên đấu giá
* Xem lịch sử giao dịch

## Đối với người bán

* Tạo sản phẩm đấu giá
* Quản lý phiên đấu giá
* Theo dõi người tham gia
* Kết thúc phiên đấu giá

## Đối với quản trị viên

* Quản lý người dùng
* Quản lý hệ thống đấu giá
* Kiểm tra và xử lý dữ liệu
* Theo dõi hoạt động hệ thống

---

# Cấu trúc dự án

```bash
src/main/java/com/auction
│
├── exception/         # Xử lý ngoại lệ
├── gui/               # Giao diện JavaFX
│   └── controller/    # Bộ điều khiển giao diện
├── manager/           # Quản lý nghiệp vụ
├── model/
│   ├── entity/        # Các thực thể hệ thống
│   └── enums/         # Enum trạng thái
├── network/           # Xử lý mạng và DTO
├── repository/        # Lưu trữ dữ liệu
├── service/           # Logic xử lý nghiệp vụ
└── util/              # Tiện ích hỗ trợ
```
---

# Các lớp nổi bật

## Entity

* `Auction` – Quản lý phiên đấu giá
* `Item` – Thông tin sản phẩm
* `User` – Người dùng hệ thống
* `Seller` – Người bán
* `Bidder` – Người tham gia đấu giá
* `Admin` – Quản trị viên

## Exception

* `AuctionClosedException`
* `InvalidBidException`
* `InsufficientBalanceException`

## Controller

* `LoginController`
* `AuctionListController`
* `AuctionDetailController`
* `AdminController`
* `SellerController`

---

# Sơ đồ hoạt động hệ thống

1. Người dùng đăng nhập vào hệ thống.
2. Người bán tạo phiên đấu giá.
3. Người tham gia xem sản phẩm và đặt giá.
4. Hệ thống cập nhật giá theo thời gian thực.
5. Khi hết thời gian đấu giá, hệ thống xác định người thắng cuộc.
6. Lưu thông tin giao dịch và lịch sử đấu giá.

---

# Yêu cầu hệ thống

* Java JDK 17 trở lên
* Maven 3.8+
* JavaFX SDK

---

# Hướng dẫn cài đặt

## 1. Clone dự án

```bash
git clone https://github.com/your-repository/auction-system.git
```

## 2. Di chuyển vào thư mục dự án

```bash
cd BaiTapNhom8
```

## 3. Build project bằng Maven

```bash
mvn clean install
```

## 4. Chạy ứng dụng

```bash
mvn javafx:run
```

---

# Hướng dẫn sử dụng

## Đăng nhập

* Chạy ứng dụng
* Nhập tài khoản và mật khẩu
* Chọn vai trò phù hợp

## Tạo phiên đấu giá

1. Người bán chọn “Tạo đấu giá”
2. Nhập thông tin sản phẩm
3. Thiết lập giá khởi điểm và thời gian đấu giá
4. Xác nhận tạo phiên đấu giá

## Tham gia đấu giá

1. Chọn phiên đấu giá đang hoạt động
2. Nhập mức giá muốn đấu
3. Hệ thống kiểm tra tính hợp lệ
4. Cập nhật giá cao nhất mới

---

# Đặc điểm nổi bật

* Giao diện trực quan bằng JavaFX
* Áp dụng mô hình MVC rõ ràng
* Dễ mở rộng và bảo trì
* Hỗ trợ nhiều loại sản phẩm
* Có xử lý ngoại lệ đầy đủ
* Tổ chức mã nguồn khoa học

---

# Kiểm thử

Dự án hỗ trợ kiểm thử bằng JUnit:

```bash
mvn test
```

---

# Định hướng phát triển

* Kết nối cơ sở dữ liệu MySQL
* Hỗ trợ thanh toán trực tuyến
* Gửi thông báo thời gian thực
* Triển khai hệ thống client-server
* Bổ sung tính năng Auto Bid
* Xây dựng API REST

---
# Đóng góp

Mọi đóng góp đều được hoan nghênh.

## Quy trình đóng góp

1. Fork repository
2. Tạo branch mới
3. Commit thay đổi
4. Push lên GitHub
5. Tạo Pull Request

---

# Giấy phép

Dự án phục vụ mục đích học tập và nghiên cứu.

---

# Liên hệ

Nếu có thắc mắc hoặc góp ý, vui lòng liên hệ nhóm phát triển thông qua GitHub hoặc email.
