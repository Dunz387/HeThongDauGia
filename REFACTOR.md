# Kế Hoạch Cải Thiện Và Refactor Dự Án

Tài liệu này tổng hợp các điểm cần cải thiện dựa trên phân tích bằng `code-review-graph`.
Mục tiêu là giảm rủi ro ở các điểm có nhiều phụ thuộc, tăng độ đúng của logic đấu giá,
và làm code dễ test/dễ bảo trì hơn.

## Tổng Quan

Dự án hiện có:

- 74 file được phân tích.
- 588 node code và 3271 quan hệ phụ thuộc.
- 17 code communities.
- Risk tổng quan thấp, nhưng có nhiều hotspot tập trung ở JavaFX controller, network, server command và domain đấu giá.

Những khu vực nên ưu tiên:

1. Domain logic đấu giá và balance.
2. Controller phòng đấu giá.
3. Server, handler và command.
4. Network manager và listener lifecycle.
5. DAO và transaction.
6. Test coverage cho các hotspot.

## 1. Tách Trách Nhiệm Trong Controller Đấu Giá

### File liên quan

- `src/main/java/view/controller/auction/InRoomController.java`
- `src/main/java/view/controller/auction/SellerInRoomController.java`

### Vấn đề

Hai controller này là hotspot lớn:

- `InRoomController` khoảng 396 dòng.
- `SellerInRoomController` khoảng 322 dòng.
- Các hàm như `setAuction`, `registerNetworkListeners`, `handleKickUser`, `handleAutoBidToggle` có nhiều kết nối tới các phần khác của hệ thống.

Controller có dấu hiệu đang đảm nhận nhiều việc:

- Cập nhật UI.
- Giữ state của phòng đấu giá.
- Đăng ký và xử lý network listener.
- Gọi command/network.
- Xử lý một phần nghiệp vụ đấu giá.

### Hướng cải thiện

- Tách logic cập nhật view sang `AuctionRoomViewModel` hoặc helper riêng.
- Tách logic network event sang service/listener riêng, ví dụ `AuctionRoomNotificationListener`.
- Tách logic auto-bid UI sang helper riêng, ví dụ `AutoBidUiController`.
- Controller chỉ nên:
  - bind UI,
  - nhận event từ người dùng,
  - gọi service/network,
  - render kết quả.

### Lợi ích

- Giảm rủi ro khi sửa UI phòng đấu giá.
- Dễ test logic mà không cần khởi động JavaFX.
- Giảm nguy cơ listener bị đăng ký lặp hoặc không được hủy khi rời phòng/logout.

## 2. Củng Cố Domain Logic Trong `Auction`

### File liên quan

- `src/main/java/model/auction/Auction.java`

### Vấn đề

`Auction` là class trung tâm của domain:

- Khoảng 420 dòng.
- Là bridge node quan trọng trong graph.
- Chứa các logic nhạy cảm như bid, auto-bid, bid history, current price, highest bidder và status.

Nếu logic ở đây sai, hệ thống có thể ghi nhận giá đấu giá sai, trừ tiền sai hoặc kết thúc phiên đấu giá sai.

### Khu vực cần rà soát

- `placeBid`
- `triggerAutoBidding`
- Xử lý `currentPrice`
- Xử lý `highestBidder`
- Xử lý `bidIncrement`
- Xử lý `bidHistory`
- Trạng thái auction: open, closed, concluded
- Gia hạn thời gian nếu có bid gần cuối
- Auto-bid hết hạn hoặc vượt giới hạn

### Invariant cần làm rõ

- Bid mới phải lớn hơn giá hiện tại theo đúng bước giá.
- Không cho bid khi auction đã đóng.
- Seller không được tự bid sản phẩm của mình nếu rule yêu cầu.
- Bidder không được bid vượt balance khả dụng.
- Balance lock, release và deduct phải nhất quán.
- Khi có bidder mới vượt giá, locked balance của bidder cũ phải được xử lý đúng.
- Auto-bid không được tạo vòng lặp hoặc đẩy giá sai.

### Hướng cải thiện

- Tách một phần logic auto-bid sang service riêng nếu `Auction` quá phình to.
- Định nghĩa rõ các exception/domain error cho bid không hợp lệ.
- Thêm unit test đầy đủ cho các case hợp lệ và biên.

## 3. Bổ Sung Test Cho Hotspot Chưa Có Coverage

### Hotspot cần ưu tiên

- `SellerInRoomController.setAuction`
- `AdminUserManagementController.initialize`
- `AdminDashboardController.initialize`
- `InRoomController.setAuction`
- `AuctionContextMenuHelper.showEditDialog`
- `CommandRegistry.initCommands`
- `Auction.triggerAutoBidding`
- `AuctionServer.startServer`

### Hướng test

Không nhất thiết phải test trực tiếp toàn bộ JavaFX controller ngay. Nên ưu tiên:

- Unit test cho `Auction`, `Bidder`, `Seller`, `AuctionManager`.
- Integration test cho server command:
  - get auctions,
  - place bid,
  - join room,
  - leave room,
  - register auto-bid,
  - ban user,
  - update user balance.
- Manual test checklist hoặc TestFX test cho:
  - login,
  - auction room,
  - seller room,
  - admin user management,
  - admin auction management.

### Test case nên có cho domain đấu giá

- Bid thành công với giá hợp lệ.
- Bid thất bại khi auction đã đóng.
- Bid thất bại khi giá thấp hơn bước giá.
- Bid thất bại khi balance không đủ.
- Auto-bid cạnh tranh giữa nhiều bidder.
- Auto-bid dừng khi vượt max amount.
- Bid gần cuối làm gia hạn thời gian nếu có rule này.
- Kết thúc auction chuyển tiền đúng.
- Bidder thua được release locked balance.

## 4. Làm Rõ Ranh Giới Server, Handler Và Command

### File liên quan

- `src/main/java/server/AuctionServer.java`
- `src/main/java/server/handler/AuctionHandler.java`
- `src/main/java/server/command/CommandRegistry.java`

### Flow quan trọng

- `handleGetUsers`
- `handleBanUser`
- `handleUpdateUserBalance`
- `handleDeleteAuction`
- `updateUserBalanceForce`
- `updateAuctionForce`
- `connect`
- `logout`
- `registerAutoBid`
- `joinRoom`
- `leaveRoom`
- `exitRoom`

### Vấn đề cần chú ý

Server và handler là boundary bảo vệ dữ liệu. Không nên tin client cho các thao tác nhạy cảm như:

- ban user,
- update balance,
- delete auction,
- force update auction,
- place bid,
- register auto-bid.

### Hướng cải thiện

- Mỗi command cần có request/response contract rõ ràng.
- Handler chỉ nên validate, điều phối và gọi service.
- Business logic nên nằm trong service/domain, không nằm quá nhiều trong handler.
- Server nên quản lý connection/session, không nên xử lý sâu business rule.
- Validate quyền admin ở server cho các command admin.
- Chuẩn hóa response lỗi:
  - `success`
  - `errorCode`
  - `message`
  - `payload`

## 5. Kiểm Soát `ClientNetworkManager`

### File liên quan

- `src/main/java/network/ClientNetworkManager.java`

### Vấn đề

`ClientNetworkManager` là điểm phụ thuộc quan trọng giữa UI và server. Nếu state kết nối hoặc listener lifecycle không chặt, UI có thể:

- nhận event trùng lặp,
- mất event,
- cập nhật UI sau khi đã rời phòng,
- bị lỗi thread khi network callback cập nhật JavaFX control.

### Hướng cải thiện

- Tách rõ các nhóm trách nhiệm:
  - `connect`
  - `disconnect`
  - `sendCommand`
  - `registerListener`
  - `unregisterListener`
- Có timeout/retry hợp lý cho command quan trọng.
- Clear listener khi logout, disconnect hoặc rời phòng.
- Đảm bảo callback cập nhật UI chạy qua JavaFX Application Thread.
- Tránh để controller đăng ký listener nhiều lần khi user vào/ra room lặp lại.

## 6. Giảm Logic Trong Các Hàm `initialize`

### File liên quan

- `src/main/java/view/controller/admin/AdminUserManagementController.java`
- `src/main/java/view/controller/admin/AdminDashboardController.java`
- `src/main/java/view/controller/admin/AdminAuctionManagementController.java`

### Vấn đề

Nhiều hàm `initialize` có degree cao. `initialize` trong JavaFX nên nhẹ và dễ đọc. Nếu hàm này chứa quá nhiều logic, việc sửa UI sẽ dễ gây regression.

### Hướng cải thiện

Tách `initialize` thành các hàm nhỏ:

- `setupTableColumns`
- `setupActions`
- `setupFilters`
- `setupBindings`
- `loadInitialData`
- `registerListeners`

Quy tắc nên theo:

- `initialize` chỉ điều phối setup.
- Không đặt business logic phức tạp trong `initialize`.
- Không gọi network ở nhiều chỗ rồi cập nhật state lung tung.
- Dialog/edit logic nên tách ra helper hoặc component riêng nếu dài.

## 7. Cải Thiện DAO Và Transaction Boundary

### Khu vực liên quan

- `src/main/java/dao`

### Vấn đề

Community `dao-dao` có cohesion thấp. Điều này gợi ý DAO có thể đang:

- lặp mapping object nhiều nơi,
- trộn query với business rule,
- thiếu transaction cho thao tác nhạy cảm,
- xử lý lỗi database chưa nhất quán.

### Khu vực cần rà soát

- Tạo auction.
- Place bid.
- Kết thúc auction.
- Cập nhật balance.
- Ban user.
- Delete auction.

### Hướng cải thiện

- Gom các thao tác liên quan tiền/trạng thái vào transaction.
- Khi conclude auction, các bước sau nên atomic:
  - cập nhật auction status,
  - ghi winner/highest bidder,
  - deduct locked balance của winner,
  - release locked balance của bidder thua,
  - cộng tiền cho seller nếu có.
- Chuẩn hóa mapper object từ ResultSet.
- Không để business rule phân tán trong DAO.

## 8. Tách Utility UI Có Quá Nhiều Vai Trò

### File liên quan

- `src/main/java/view/utility/AuctionContextMenuHelper.java`
- `src/main/java/view/utility/AuctionTableConfigurator.java`
- `src/main/java/view/utility/AuctionRoomHelper.java`
- `src/main/java/view/utility/NotificationFilterHelper.java`
- `src/main/java/view/utility/SceneManager.java`

### Vấn đề

Utility tốt khi nó chỉ làm một việc rõ ràng. Nhưng nếu helper biết quá nhiều về controller, network, dialog và model, nó sẽ trở thành controller phụ.

### Hướng cải thiện

- `AuctionTableConfigurator` chỉ setup table, column và cell factory.
- `AuctionContextMenuHelper` chỉ tạo menu và gọi callback.
- `SceneManager` chỉ chuyển scene và truyền context tối thiểu.
- Dialog create/edit auction nên tách riêng nếu logic dài.
- Utility không nên tự ý gọi network nếu có thể đẩy callback ra ngoài.

## 9. Quản Lý User State Và Balance Chặt Hơn

### Khu vực liên quan

- `src/main/java/model/user`
- `src/main/java/server/handler`
- `src/main/java/service`

### Điểm cần rà soát

- User bị ban có còn join room được không.
- User bị ban có còn bid được không.
- Admin update balance khi user đang có locked balance thì xử lý thế nào.
- Balance âm có bị chặn ở mọi entry point không.
- Khi bidder thua, locked balance có được release không.
- Khi bidder thắng, locked balance có được deduct/payment đúng không.

### Hướng cải thiện

- Định nghĩa rõ các state của user: active, banned, logged out.
- Kiểm tra banned user ở server-side command, không chỉ chặn trên UI.
- Viết test cho balance available/locked/total.
- Không cho phép update balance tạo ra state vô lý.

## 10. Chuẩn Hóa Error Handling

### Vấn đề

Hệ thống có nhiều boundary:

- UI
- network
- server command
- handler
- service
- DAO
- domain

Nếu mỗi tầng xử lý lỗi một kiểu, UI sẽ khó hiển thị thông báo đúng và test cũng khó ổn định.

### Hướng cải thiện

- Tạo error code thống nhất cho command response.
- Domain nên ném exception có ý nghĩa, ví dụ:
  - `InvalidBidException`
  - `AuctionClosedException`
  - `InsufficientBalanceException`
  - `UnauthorizedActionException`
- Handler map exception thành response lỗi rõ ràng.
- UI chỉ cần đọc `errorCode` và `message` để hiển thị.

## 11. Ưu Tiên Refactor Theo Giai Đoạn

### Giai đoạn 1: Bảo vệ domain đấu giá

- Thêm test cho `Auction.placeBid`.
- Thêm test cho `Auction.triggerAutoBidding`.
- Thêm test cho balance lock/release/deduct.
- Rà soát invariant trong `Auction`.

### Giai đoạn 2: Làm chặt server command

- Chuẩn hóa command response.
- Validate quyền admin ở server.
- Thêm integration test cho bid, auto-bid, join/leave room, ban user, update balance.

### Giai đoạn 3: Giảm độ phình controller

- Tách listener/network event khỏi `InRoomController`.
- Tách listener/network event khỏi `SellerInRoomController`.
- Tách các hàm `initialize` lớn trong admin controller.

### Giai đoạn 4: Cải thiện DAO và transaction

- Rà soát các thao tác liên quan tiền.
- Đảm bảo conclude auction là atomic.
- Chuẩn hóa mapper và xử lý lỗi database.

### Giai đoạn 5: Cải thiện UI utility và lifecycle

- Tách utility theo đúng vai trò.
- Clear listener khi logout/disconnect/leave room.
- Đảm bảo network callback cập nhật UI đúng JavaFX thread.

## Checklist Nhanh

- [ ] Test domain `Auction` đầy đủ.
- [ ] Test balance lock/release/deduct.
- [ ] Test auto-bid cạnh tranh nhiều bidder.
- [ ] Validate admin command trên server.
- [ ] Chuẩn hóa command response.
- [ ] Tách network listener khỏi room controller.
- [ ] Clear listener khi leave room/logout.
- [ ] Tách `initialize` trong admin controller.
- [ ] Kiểm tra transaction khi conclude auction.
- [ ] Chuẩn hóa DAO mapper.
- [ ] Tách utility UI có quá nhiều vai trò.
- [ ] Đảm bảo UI update từ network callback chạy trên JavaFX thread.
