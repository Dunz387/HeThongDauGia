# Hệ thống đấu giá trực tuyến
## Các lớp chính
### Controller

### DAO

### Model

**<ins> Base class </ins>**

**Entity: Abtract class**

***Thuộc tính: protected***

ID - Tạo ID ngẫu nhiên

Createdat - Created at - Lấy thời gian thực

***Phương thức:***

getter và setter cho các thuộc tính

<ins>**Item** </ins>

***Abtract class Item***

***Thuộc tính:***

name: tên sản phẩm

description: mô tả sản phẩm

startPrice: giá khởi điểm

seller : gọi người bán

buyNowPrice: giá mua ngay

reversedPrice: giá tối thiểu để đấu giá thành công

condition: điều kiện hàng - <ins> mới </ins> hoặc <ins>cũ</ins>

quantity: số lượng hàng

imageUrls: Ảnh của mặt hàng

***Phương thức:***

getter và setter cho các thuộc tính

addImage và removeImage: thêm và xóa ảnh



***Art***

***Electronic***

***Vehicle***