# Hệ Thống Đấu Giá

Ứng dụng đấu giá trực tuyến viết bằng Java, chạy theo mô hình Client/Server. Hệ thống cho phép người bán tạo và quản lý phiên đấu giá, người mua tham gia phòng đấu giá và đặt giá theo thời gian thực, quản trị viên theo dõi và điều hành người dùng/phòng đấu giá.

Phạm vi hiện tại tập trung vào ứng dụng desktop JavaFX, server socket TCP, lưu trữ SQLite cục bộ và các chức năng nghiệp vụ đấu giá theo đề bài `2026-Bài-tập-lớn.pdf`.

## Công Nghệ Sử Dụng

- Ngôn ngữ: Java
- Giao diện: JavaFX, FXML, CSS
- Build tool: Maven
- Cơ sở dữ liệu: SQLite, file `auction.db`
- Giao tiếp mạng: Java Socket, `ObjectInputStream`/`ObjectOutputStream`
- Thư viện chính: OpenJFX 25, SQLite JDBC, Gson, SLF4J Simple, JUnit 5
- Kiến trúc: MVC kết hợp các tầng Model, View, Service, DAO, Network, Server
- Mẫu thiết kế/kỹ thuật: Singleton, Factory, Observer, xử lý đặt giá đồng thời bằng `ReentrantLock`, bộ theo dõi phiên đấu giá bằng scheduler

## Môi Trường Chạy Và Yêu Cầu Cài Đặt

Yêu cầu cài đặt:

- JDK 25
- Maven 3.9+ khuyến nghị
- Hệ điều hành: Windows, Linux hoặc macOS
- Môi trường desktop có hỗ trợ JavaFX UI

Kiểm tra môi trường:

```bash
java -version
mvn -version
```

Dự án đang cấu hình trong `pom.xml`:

- `maven.compiler.source=25`
- `maven.compiler.target=25`
- `javafx.version=25`
- Client main class: `view.Launcher`
- Server main class: `server.AuctionServer`

## Cấu Trúc Thư Mục

```text
.
|-- 2026-Bài-tập-lớn.pdf          # Đề bài
|-- auction.db                    # SQLite database
|-- pom.xml                       # Cấu hình Maven
|-- src
|   |-- main
|   |   |-- java
|   |   |   |-- dao               # Truy cập SQLite: user, auction, admin, notification
|   |   |   |-- exception         # Exception riêng cho đấu giá
|   |   |   |-- model             # Entity, User, Item, Auction, BidTransaction
|   |   |   |-- network           # ClientNetworkManager, SessionManager, notification
|   |   |   |-- server            # AuctionServer, ClientHandler, request handlers
|   |   |   |-- service           # UserService, AuctionManager, AdminService
|   |   |   |-- shared            # Protocol và global exception handler
|   |   |   `-- view              # JavaFX app, controllers, UI utilities
|   |   `-- resources
|   |       `-- view              # FXML, CSS, ảnh giao diện
|   `-- test
|       `-- java                  # JUnit tests cho user và concurrent bidding
`-- target                        # Kết quả build Maven
```

## Các Module Chính

- `model`: mô hình hóa người dùng (`Admin`, `Seller`, `Bidder`), tài sản (`Electronics`, `Arts`, `Vehicle`) và phiên đấu giá.
- `dao`: tạo bảng, đọc/ghi SQLite và cập nhật dữ liệu.
- `service`: xử lý nghiệp vụ đăng ký/đăng nhập, đấu giá, kết thúc phiên, quản trị.
- `server`: lắng nghe cổng `8080`, nhận request từ client và phân phối sang handler phù hợp.
- `network`: quản lý kết nối socket phía client, listener nhận broadcast và cập nhật UI.
- `view`: ứng dụng JavaFX, FXML controller và các helper cho bảng, menu, phòng đấu giá, alert.
- `shared`: định nghĩa protocol Client/Server, delimiter `;;;` và các mã request/response/broadcast.

## Câu Lệnh Build Và Kiểm Thử

Tất cả lệnh dưới đây chạy tại thư mục gốc dự án.

Build/compile trên Windows, Linux và macOS:

```bash
mvn clean compile
```

Chạy test:

```bash
mvn test
```

Đóng gói JAR và copy dependency:

```bash
mvn package
```

Đóng gói app-image bằng `jpackage`:

```bash
# Windows
mvn -Ppackage-windows package

# macOS
mvn -Ppackage-mac package
```

Ghi chú: hiện `pom.xml` chưa có profile `jpackage` riêng cho Linux. Trên Linux nên chạy trực tiếp bằng Maven hoặc bổ sung profile Linux nếu cần đóng gói app-image.

## Hướng Dẫn Chạy Server/Client

Thứ tự chạy cụ thể:

1. Mở terminal thứ nhất và chạy Server.
2. Chờ Server báo đang lắng nghe cổng `8080`.
3. Mở terminal thứ hai và chạy Client.
4. Đăng ký tài khoản `SELLER` hoặc `BIDDER`, sau đó đăng nhập để sử dụng.

Chạy Server bằng Maven, dùng được trên Windows/Linux/macOS:

```bash
mvn org.codehaus.mojo:exec-maven-plugin:3.1.0:java -Dexec.mainClass=server.AuctionServer
```

Chạy Client JavaFX bằng Maven, dùng được trên Windows/Linux/macOS:

```bash
mvn javafx:run
```

Lệnh thay thế sau khi đã `mvn package`, nếu muốn chạy bằng `java -cp`:

```powershell
# Windows PowerShell
java -cp "target/classes;target/libs/*" server.AuctionServer
java -cp "target/classes;target/libs/*" view.Launcher
```

```bash
# Linux/macOS
java -cp "target/classes:target/libs/*" server.AuctionServer
java -cp "target/classes:target/libs/*" view.Launcher
```

Lưu ý quan trọng: client hiện đang kết nối tới địa chỉ `159.223.48.211:8080` trong `src/main/java/view/Main.java`. Nếu muốn chạy Server trên máy local, cần đổi địa chỉ này thành `127.0.0.1` hoặc IP máy đang chạy Server, rồi chạy Client sau Server.

## Chức Năng Đã Hoàn Thành Theo Đề Bài

Theo các yêu cầu trong `2026-Bài-tập-lớn.pdf` và phần hiện thực trong mã nguồn, dự án đã hoàn thành các nhóm chức năng sau:

- Đăng ký tài khoản theo vai trò `BIDDER` hoặc `SELLER`.
- Đăng nhập, đăng xuất và chặn đăng nhập trùng tài khoản trên nhiều client.
- Phân quyền người dùng theo `ADMIN`, `SELLER`, `BIDDER`.
- Seller tạo phiên đấu giá với tên sản phẩm, mô tả, loại sản phẩm, giá khởi điểm và thời lượng.
- Seller xem danh sách phiên/tài sản của mình.
- Seller sửa hoặc xóa phiên khi chưa có người đặt giá và đúng quyền sở hữu.
- Hỗ trợ nhiều loại sản phẩm: đồ điện tử, tác phẩm nghệ thuật, phương tiện.
- Bidder xem danh sách phiên đấu giá.
- Bidder tham gia phòng đấu giá.
- Bidder đặt giá trực tiếp, kiểm tra bước giá tối thiểu và số dư khả dụng.
- Hỗ trợ tự động đặt giá (`auto-bid`) với giá trần và bước tăng.
- Hủy auto-bid.
- Khóa/giải phóng số dư khi đặt giá, thanh toán khi thắng đấu giá.
- Nạp tiền cho Bidder.
- Rút tiền cho Seller.
- Tự động kết thúc phiên khi hết thời gian.
- Chuyển tiền cho Seller và chuyển quyền sở hữu sản phẩm cho người thắng.
- Broadcast giá mới theo thời gian thực tới các client đang online.
- Broadcast kết thúc phiên đấu giá.
- Gia hạn thời gian thêm 30 giây khi có bid trong khoảng cuối phiên.
- Theo dõi số người trong phòng đấu giá.
- Seller/Admin có thể đuổi người dùng khỏi phòng đấu giá.
- Admin xem danh sách người dùng.
- Admin khóa/mở khóa tài khoản và ép đăng xuất người dùng bị khóa.
- Admin cập nhật số dư người dùng khi hợp lệ.
- Admin xem dashboard và quản lý danh sách phiên đấu giá.
- Admin xóa phiên đấu giá.
- Lưu trữ dữ liệu người dùng, phiên đấu giá, lịch sử bid và thông báo bằng SQLite.
- Khởi tạo schema database tự động nếu chưa có bảng.
- Tách giao thức Client/Server trong `shared.Protocol`.
- Xử lý đồng thời khi đặt giá bằng `ReentrantLock`.
- Có unit test cho Bidder, Seller và tình huống đặt giá đồng thời.