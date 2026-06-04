const fs = require('fs');
const scan = JSON.parse(fs.readFileSync('D:/1_Dung/LapTrinhNangCao/BaiTapLon/HeThongDauGia/.understand-anything/tmp/ua-scan-files.json', 'utf8'));
const importMap = JSON.parse(fs.readFileSync('D:/1_Dung/LapTrinhNangCao/BaiTapLon/HeThongDauGia/.understand-anything/tmp/ua-import-map-output.json', 'utf8')).importMap;

const out = {
  name: 'HeThongDauGia',
  description: 'Hệ Thống Đấu Giá là ứng dụng đấu giá trực tuyến viết bằng Java, hoạt động theo mô hình Client/Server. Hệ thống cho phép quản lý phiên đấu giá, đặt giá theo thời gian thực và quản trị người dùng với giao diện JavaFX và cơ sở dữ liệu SQLite. Lưu ý: dự án này có hơn 100 file mã nguồn; hãy cân nhắc giới hạn phân tích trong một thư mục con để có kết quả nhanh hơn.',
  languages: ['css', 'fxml', 'java', 'json', 'markdown', 'xml', 'yaml'],
  frameworks: ['JavaFX', 'JUnit', 'SQLite'],
  files: scan.files,
  totalFiles: scan.totalFiles,
  filteredByIgnore: scan.filteredByIgnore,
  estimatedComplexity: scan.estimatedComplexity,
  importMap: importMap
};

fs.writeFileSync('D:/1_Dung/LapTrinhNangCao/BaiTapLon/HeThongDauGia/.understand-anything/intermediate/scan-result.json', JSON.stringify(out, null, 2), 'utf8');
