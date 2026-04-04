# Hệ thống đấu giá trực tuyến
## Các lớp chính
### Controller

### DAO

### Model

**<ins> Base class </ins>**

### **Entity (Abstract Class)**

| Loại | Tên | Access Modifier | Mô tả chi tiết |
| :--- | :--- | :--- | :--- |
| **Thuộc tính** | `ID` | `protected` | Tạo ID ngẫu nhiên. |
| **Thuộc tính** | `Createdat` | `protected` | Lấy thời gian thực (thời điểm tạo). |
| **Phương thức** | `getter` & `setter` | `public` *(mặc định)* | Các hàm get/set cho các thuộc tính tương ứng. |


<ins>**Item** </ins>

***Abtract class Item***

### **Product / Auction Item**

### **Bảng 1: Thuộc tính (Attributes)**

| Tên thuộc tính    | Mô tả chi tiết                                                 |
|:------------------|:---------------------------------------------------------------|
| **name**          | Tên sản phẩm.                                                  |
| **description**   | Mô tả chi tiết về sản phẩm.                                    |
| **startPrice**    | Giá khởi điểm của sản phẩm.                                    |
| **seller**        | Thông tin/đối tượng người bán.                                 |
| **buyNowPrice**   | Giá mua ngay (dành cho người muốn mua luôn không cần đấu giá). |
| **reversedPrice** | Giá tối thiểu để cuộc đấu giá được xem là thành công.          |
| **condition**     | Tình trạng của mặt hàng: `mới` hoặc `cũ`.                      |
| **quantity**      | Số lượng hàng hóa.                                             |
| **imageUrls**     | Danh sách các đường dẫn ảnh của mặt hàng.                      |

<br>

### **Bảng 2: Phương thức (Methods)**

| Tên phương thức | Mô tả chi tiết |
| :--- | :--- |
| `getter` & `setter` | Các hàm lấy và gán dữ liệu cho các thuộc tính trên. |
| `addImage` | Hàm dùng để thêm ảnh mới cho sản phẩm. |
| `removeImage` | Hàm dùng để xóa ảnh của sản phẩm. |


***Art***

***Electronic***

***Vehicle***