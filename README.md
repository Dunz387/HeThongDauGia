# Hệ Thống Đấu Giá (Auction System)

Một ứng dụng Java JavaFX dùng để quản lý hệ thống đấu giá trực tuyến.

## 📋 Cấu Trúc Dự Án

Dự án tuân theo cấu trúc tiêu chuẩn của Maven:

```
HeThongDauGia/
├── pom.xml                          # Cấu hình Maven
├── src/
│   ├── main/
│   │   ├── java/                    # Mã nguồn Java
│   │   │   ├── client/              # Client components
│   │   │   ├── dao/                 # Data Access Objects
│   │   │   ├── exception/           # Custom exceptions
│   │   │   ├── model/               # Data models
│   │   │   ├── server/              # Server components
│   │   │   ├── service/             # Business logic services
│   │   │   └── view/                # UI components (JavaFX)
│   │   └── resources/               # Resource files
│   │       └── view/                # FXML files
│   └── test/
│       ├── java/                    # Test code
│       └── resources/               # Test resources
├── target/                          # Build output (auto-generated)
└── .gitignore                       # Git ignore rules
```

## 🛠️ Yêu Cầu

- **Java**: Version 25+
- **Maven**: 3.8.1+
- **JavaFX**: 26

## 📦 Build & Run

### Biên dịch dự án
```bash
mvn clean compile
```

### Chạy ứng dụng
```bash
mvn javafx:run
```

### Đóng gói JAR
```bash
mvn clean package
```

### Chạy tests
```bash
mvn test
```

## 📚 Các Module Chính

- **client/**: Giao tiếp với server
- **dao/**: Quản lý cơ sở dữ liệu
- **model/**: Các lớp model (Auction, User, Item, etc.)
- **server/**: Logic server và xử lý kết nối
- **service/**: Các dịch vụ kinh doanh
- **view/**: Giao diện người dùng JavaFX

## 🔍 Cấu Trúc Gói (Package Structure)

```
com.hethongdaugia
├── client
├── dao
├── exception
├── model
│   ├── auction
│   ├── base
│   ├── item
│   └── user
├── server
├── service
└── view
    ├── AdminUI
    ├── AuthenticationUI
    ├── BaseMenuUI
    ├── BidderUI
    └── SellerUI
```

## 🚀 Chạy Ứng Dụng

Chương trình sẽ khởi động màn hình Login:
```bash
mvn javafx:run
```

Hoặc dùng IDE (IntelliJ IDEA, Visual Studio Code, Eclipse):
- Nhấp phải vào project → Maven → Reload projects
- Chọn main class: `view.AuthenticationUI.LoginView.Login`
- Run

## 📝 Ghi Chú

- Các file FXML được lưu trong `src/main/resources/view/`
- Tests được lưu trong `src/test/java/`
- Để thêm dependencies, chỉnh sửa section `<dependencies>` trong `pom.xml`

## 🔗 Liên Kết Hữu Ích

- [Maven Documentation](https://maven.apache.org/)
- [JavaFX Documentation](https://gluonhq.com/products/javafx/)
- [Maven JavaFX Plugin](https://github.com/openjfx/javafx-maven-plugin)
