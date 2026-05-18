# Hệ Thống Đấu Giá (Auction System)

Ứng dụng Java/JavaFX quản lý hệ thống đấu giá trực tuyến theo mô hình **Client-Server**, tuân thủ nguyên tắc **SOLID** và kiến trúc **MVC**.

## 📋 Kiến Trúc Dự Án

Dự án được tổ chức theo mô hình phân tầng rõ ràng:

| Tầng | Package | Trách nhiệm |
|------|---------|-------------|
| **Model** | `model/` | Dữ liệu nghiệp vụ: User, Auction, Item, BidTransaction |
| **View** | `view/` + `resources/view/` | Giao diện FXML + Controller |
| **Service** | `service/` | Logic nghiệp vụ: AuctionManager, UserService, AdminService |
| **DAO** | `dao/` | Truy cập cơ sở dữ liệu SQLite |
| **Network** | `network/` | Giao tiếp Client-Server qua Socket |
| **Server** | `server/` | Xử lý kết nối và phân phối request |
| **Shared** | `shared/` | Protocol, GlobalExceptionHandler |

## 🔍 Cấu Trúc Thư Mục Chi Tiết

```
HeThongDauGia/
├── pom.xml
├── README.md
├── auction.db
├── src/main/java/
│   ├── dao/                              # Data Access Layer
│   │   ├── DBConnection.java
│   │   ├── DatabaseManager.java
│   │   ├── UserDAO.java
│   │   ├── AuctionDAO.java
│   │   ├── AdminDAO.java
│   │   └── NotificationDAO.java
│   │
│   ├── model/                            # Domain Models
│   │   ├── base/Entity.java
│   │   ├── user/                         # User, Admin, Bidder, Seller, Role
│   │   ├── item/                         # Item, ItemFactory, Electronics, Arts, Vehicle
│   │   └── auction/                      # Auction, AuctionStatus, AuctionObserver, BidTransaction
│   │
│   ├── service/                          # Business Logic (SRP)
│   │   ├── AuctionManager.java           # Quản lý Auction: CRUD, bid, monitor
│   │   ├── UserService.java              # Quản lý User: auth, register
│   │   └── AdminService.java             # Quản lý Admin: ban, force update
│   │
│   ├── server/                           # Server Layer
│   │   ├── AuctionServer.java            # Entry point Server
│   │   ├── ClientHandler.java            # Router phân phối request
│   │   └── handler/                      # Request Handlers (SRP)
│   │       ├── AuthHandler.java          # Login, Register, Logout
│   │       ├── AuctionHandler.java       # CRUD Item, Bid, Room
│   │       ├── AdminHandler.java         # Admin operations
│   │       └── FinancialHandler.java     # Deposit, Withdraw
│   │
│   ├── network/                          # Client Network
│   │   ├── ClientNetworkManager.java     # Singleton Socket client
│   │   ├── SessionManager.java           # Quản lý phiên đăng nhập
│   │   └── NotificationManager.java      # Quản lý thông báo
│   │
│   ├── shared/                           # Shared Utilities
│   │   ├── Protocol.java                 # Giao thức truyền tin
│   │   └── GlobalExceptionHandler.java   # Xử lý ngoại lệ toàn cục
│   │
│   ├── exception/                        # Custom Exceptions
│   │   ├── AuctionClosedException.java
│   │   └── InvalidBidException.java
│   │
│   └── view/                             # View Layer (MVC)
│       ├── Main.java                     # Entry point Client (Application)
│       ├── controller/                   # Controllers (C trong MVC)
│       │   ├── auth/                     # LoginController, RegisterController
│       │   ├── menu/                     # BaseMenuController, AssetsListController, UserInfoController
│       │   ├── auction/                  # InRoomController, SellerInRoomController, RoomMenuChoiceController
│       │   ├── admin/                    # AdminDashboardController, AdminUserManagement, AdminAuctionManagement
│       │   └── seller/                   # CreateItemController, SellerAuctionListController
│       └── utility/                      # View Utilities
│           ├── AlertHelper.java
│           ├── AuctionNetworkHelper.java
│           ├── AuctionTableConfigurator.java
│           ├── NotificationMenuHandler.java
│           ├── SceneManager.java
│           ├── StatusDisplayHelper.java
│           ├── ValidationHelper.java
│           └── WindowManager.java
│
└── src/main/resources/view/              # FXML Files (V trong MVC)
    ├── styles.css
    ├── auth/                             # Login.fxml, Register.fxml
    ├── menu/                             # BaseMenu.fxml, AssetsList.fxml, UserInforView.fxml
    ├── auction/                          # InRoomView.fxml, SellerInRoomView.fxml, RoomMenuChoice.fxml
    ├── admin/                            # AdminDashboard.fxml, AdminUserManagement.fxml, AdminAuctionManagement.fxml
    └── seller/                           # CreateItem.fxml, SellerAuctionList.fxml
```

## 🏗️ Nguyên Tắc SOLID Đã Áp Dụng

| Nguyên tắc | Áp dụng |
|-----------|---------|
| **S** - Single Responsibility | AuctionManager chỉ quản lý Auction; UserService quản lý User; AdminService quản lý Admin; Mỗi Handler xử lý 1 nhóm request |
| **O** - Open/Closed | ItemFactory + StatusDisplayHelper mở rộng loại sản phẩm/trạng thái không cần sửa code cũ |
| **L** - Liskov Substitution | Bidder/Seller/Admin kế thừa User, hoán đổi tự do |
| **I** - Interface Segregation | AuctionObserver/AuctionSubject tách biệt interface nhỏ gọn |
| **D** - Dependency Inversion | ClientNetworkManager dùng listener pattern thay vì gọi thẳng View |

## 🛠️ Yêu Cầu

- **Java**: 21+
- **Maven**: 3.8.1+
- **JavaFX**: 21

## 📦 Build & Run

```bash
# Biên dịch
mvn clean compile

# Chạy Server
# Run class: server.AuctionServer

# Chạy Client
mvn javafx:run
# Hoặc run class: view.Main
```

## 📝 Ghi Chú

- Database: SQLite (`auction.db`) tự động khởi tạo schema
- Giao thức: Sử dụng delimiter `;;;` cho truyền tin Socket
- Observer Pattern: Server lắng nghe thay đổi giá real-time qua AuctionObserver
