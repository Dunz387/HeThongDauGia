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

Có 2 cách chạy sản phẩm:

1. Chạy bản phát hành dành cho người dùng cuối bằng file `client.exe`, `client.app` hoặc gói `.deb` trong GitHub Release.
2. Chạy từ mã nguồn bằng JDK và Maven.

Với bản release zip trên Windows, máy người dùng không cần cài JVM/JDK riêng. File zip đã được đóng gói bằng `jpackage`, nên runtime Java cần thiết đã nằm trong thư mục ứng dụng.

Nếu chạy từ mã nguồn, cần cài:

- JDK 25
- Maven 3.9+ khuyến nghị
- Hệ điều hành: Windows, Linux hoặc macOS
- Môi trường desktop có hỗ trợ JavaFX UI

Kiểm tra môi trường khi chạy từ mã nguồn:

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

# Linux/Ubuntu, tạo file .deb trong target/dist
mvn -Ppackage-linux package
```

Ghi chú: profile `package-linux` tạo gói `.deb` cho client. Khi cần chạy cả Server và Client để phát triển hoặc demo nội bộ, nên chạy trực tiếp từ mã nguồn bằng Maven theo hướng dẫn bên dưới.

## Hướng Dẫn Chạy Sản Phẩm

### Cách 1.1: Chạy bản release bằng `client.exe` trên Windows

Cách này dành cho người dùng chỉ muốn mở ứng dụng, không cần cài Java, JDK, Maven hoặc JavaFX.

1. Vào mục **Releases** của repository.
2. Tải file zip ở tag release mới nhất.
3. Giải nén file zip ra một thư mục bất kỳ.
4. Mở thư mục vừa giải nén và chạy file `client.exe`.
5. Đăng ký tài khoản `SELLER` hoặc `BIDDER`, sau đó đăng nhập để sử dụng.

Lưu ý:

- Không chạy trực tiếp `client.exe` khi file vẫn còn nằm trong file zip. Cần giải nén trước.
- Không xóa các thư mục/file đi kèm `client.exe`, vì đó là runtime và thư viện mà ứng dụng cần để chạy.
- Nếu Windows SmartScreen cảnh báo, chọn **More info** rồi **Run anyway** nếu bạn chắc chắn file được tải từ release chính thức của repository.
- Bản `client.exe` mặc định kết nối tới server đã cấu hình sẵn trong ứng dụng. Nếu server đang tắt hoặc không truy cập được, client sẽ không đăng nhập/kết nối được.

### Cách 1.2: Chạy bản release bằng `client.app` trên macOS

Cách này áp dụng cho người dùng macOS muốn chạy trực tiếp bản đóng gói, không cần cài JVM/JDK riêng.

1. Vào mục **Releases** của repository.
2. Tải file zip ở tag release mới nhất.
3. Giải nén file zip ra một thư mục bất kỳ.
4. Mở thư mục vừa giải nén và chạy `client.app`.
5. Đăng ký tài khoản `SELLER` hoặc `BIDDER`, sau đó đăng nhập để sử dụng.

Lưu ý:

- Nếu macOS chặn ứng dụng do Gatekeeper, vào `System Settings` để cho phép mở ứng dụng hoặc chuột phải vào `client.app` rồi chọn `Open` ở lần chạy đầu tiên.
- Không xóa các file/thư mục đi kèm `client.app`, vì đó là runtime và thư viện mà ứng dụng cần để chạy.
- Bản `client.app` mặc định kết nối tới server đã cấu hình sẵn trong ứng dụng. Nếu server đang tắt hoặc không truy cập được, client sẽ không đăng nhập/kết nối được.

### Cách 1.3: Chạy bản release bằng gói `.deb` trên Ubuntu

Cách này áp dụng cho người dùng Ubuntu muốn cài và chạy trực tiếp bản đóng gói, không cần cài JDK/Maven riêng.

1. Vào mục **Releases** của repository.
2. Tải file `.deb` dành cho Linux/Ubuntu ở tag release mới nhất.
3. Mở terminal tại thư mục chứa file vừa tải.
4. Cài gói bằng lệnh:

```bash
sudo apt install ./HeThongDauGiaClient_1.0.0-1_amd64.deb
```

Nếu tên file `.deb` khác, thay tên file trong lệnh trên bằng đúng tên file đã tải.

Sau khi cài xong, có thể mở ứng dụng từ menu ứng dụng của Ubuntu hoặc chạy từ terminal:

```bash
HeThongDauGiaClient
```

Lưu ý:

- Bản `.deb` chỉ đóng gói client. Server cần đang chạy và client phải trỏ đúng địa chỉ server.
- Nếu `apt` báo thiếu thư viện hệ thống cho giao diện desktop, cập nhật package index rồi cài lại:

```bash
sudo apt update
sudo apt install ./HeThongDauGiaClient_1.0.0-1_amd64.deb
```

### Cách 2: Chạy từ mã nguồn khi phát triển

Thứ tự chạy:

1. Mở terminal thứ nhất và chạy Server.
2. Chờ Server báo đang lắng nghe cổng `8080`.
3. Mở terminal thứ hai và chạy Client.
4. Đăng ký tài khoản `SELLER` hoặc `BIDDER`, sau đó đăng nhập để sử dụng.

Chạy Server bằng Maven:

```bash
mvn org.codehaus.mojo:exec-maven-plugin:3.1.0:java -Dexec.mainClass=server.AuctionServer
```

Chạy Client JavaFX bằng Maven:

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

#### Hướng dẫn nhanh trên Ubuntu

1. Cài JDK 25 và Maven. Nếu Ubuntu chưa có sẵn gói JDK 25 trong repository mặc định, có thể cài JDK 25 từ Eclipse Temurin/Adoptium hoặc Oracle JDK, sau đó kiểm tra lại `JAVA_HOME`.

```bash
sudo apt update
sudo apt install maven git
java -version
mvn -version
```

2. Tải mã nguồn và vào thư mục dự án:

```bash
git clone <url-repository>
cd HeThongDauGia
```

Nếu đã có sẵn mã nguồn, chỉ cần mở terminal tại thư mục gốc dự án.

3. Build và chạy kiểm thử:

```bash
mvn clean compile
mvn test
```

4. Chạy Server ở terminal thứ nhất:

```bash
mvn org.codehaus.mojo:exec-maven-plugin:3.1.0:java -Dexec.mainClass=server.AuctionServer
```

5. Chạy Client JavaFX ở terminal thứ hai:

```bash
mvn javafx:run
```

6. Nếu muốn chạy bằng classpath sau khi đóng gói:

```bash
mvn package
java -cp "target/classes:target/libs/*" server.AuctionServer
java -cp "target/classes:target/libs/*" view.Launcher
```

7. Nếu muốn tạo gói cài đặt `.deb` trên Ubuntu:

```bash
mvn -Ppackage-linux package
ls target/dist
sudo apt install ./target/dist/*.deb
```

Lưu ý trên Ubuntu:

- Cần chạy Server trước Client.
- Nếu chạy Server local, đổi địa chỉ server trong `src/main/java/view/Main.java` từ `159.223.48.211` sang `127.0.0.1` trước khi chạy Client.
- Máy Ubuntu cần môi trường desktop để hiển thị JavaFX. Nếu chạy trên server không có giao diện đồ họa, chỉ nên chạy `server.AuctionServer`.

## Chức Năng Đã Hoàn Thành Theo Đề Bài

Theo các yêu cầu trong `2026-Bài-tập-lớn.pdf` và phần hiện thực trong mã nguồn, dự án đã hoàn thành các nhóm chức năng sau:

### 1. Tài khoản và phân quyền
- Đăng ký tài khoản theo vai trò `BIDDER` hoặc `SELLER`.
- Đăng nhập, đăng xuất và chặn đăng nhập trùng tài khoản trên nhiều client.
- Phân quyền người dùng theo `ADMIN`, `SELLER`, `BIDDER`.

### 2. Phiên đấu giá và đấu giá
- Seller tạo phiên đấu giá với tên sản phẩm, mô tả, loại sản phẩm, giá khởi điểm và thời lượng.
- Seller xem danh sách phiên/tài sản của mình.
- Seller sửa hoặc xóa phiên khi chưa có người đặt giá và đúng quyền sở hữu.
- Hỗ trợ nhiều loại sản phẩm: đồ điện tử, tác phẩm nghệ thuật, phương tiện.

### 3. Bidder xem danh sách phiên đấu giá.
- Bidder tham gia phòng đấu giá.

### 4. Đặt giá và đấu giá
- Bidder đặt giá trực tiếp, kiểm tra bước giá tối thiểu và số dư khả dụng.
- Hỗ trợ tự động đặt giá (`auto-bid`) với giá trần và bước tăng.
- Hủy auto-bid.
- Khóa/giải phóng số dư khi đặt giá, thanh toán khi thắng đấu giá.

### 5. Quản lý tài chính và kết thúc phiên đấu giá
- Nạp tiền cho Bidder.
- Rút tiền cho Seller.
- Tự động kết thúc phiên khi hết thời gian.
- Chuyển tiền cho Seller và chuyển quyền sở hữu sản phẩm cho người thắng.

### 6. Real-time Notification
- Broadcast giá mới theo thời gian thực tới các client đang online.
- Broadcast kết thúc phiên đấu giá.

### 7. Quản lý người dùng
- Theo dõi số người trong phòng đấu giá.
- Seller/Admin có thể đuổi người dùng khỏi phòng đấu giá.

### 8. Quản lý phiên đấu giá
- Admin xem danh sách phiên đấu giá.
- Admin xóa phiên đấu giá.

### 9. Quản lý tài khoản
- Admin khóa/mở khóa tài khoản và ép đăng xuất người dùng bị khóa.
- Admin cập nhật số dư người dùng khi hợp lệ.
- Admin xem dashboard và quản lý danh sách phiên đấu giá.
- Admin xóa phiên đấu giá.

### 10. Lưu trữ dữ liệu
- Lưu trữ dữ liệu người dùng, phiên đấu giá, lịch sử bid và thông báo bằng SQLite.
- Khởi tạo schema database tự động nếu chưa có bảng.

### 11. Xử lý trường hợp đặc biệt
- Xử lý đồng thời khi đặt giá bằng `ReentrantLock`.

### 12. Tích hợp kiến trúc
- Kiến trúc Client-Server.
- Áp dụng MVC.
- Tách giao thức Client/Server trong `shared.Protocol`.
- Thiết lập CI/CD cơ bản trên GitHub Action
- Unit test cho các chức năng của Bidder, Seller và tình huống đấu giá đồng thời.

### 13. Chức năng nâng cao
- Auto-bidding.
- Gia hạn thời gian thêm 30 giây khi có bid trong khoảng cuối phiên.
- Biểu đồ đường giá real-time.
- Người dùng có thể tham gia nhiều hơn 1 phiên đấu giá cùng lúc, có `lockBalance` để cân bằng số dư người dùng khi tham gia nhiều phòng đấu giá.
