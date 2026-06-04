const fs = require('fs');
const batchesPath = 'D:\\1_Dung\\LapTrinhNangCao\\BaiTapLon\\HeThongDauGia\\.understand-anything\\intermediate\\batches.json';
const data = JSON.parse(fs.readFileSync(batchesPath, 'utf8'));
const batch = data.batches.find(b => b.batchIndex === 3);

const input = {
  projectRoot: 'D:\\1_Dung\\LapTrinhNangCao\\BaiTapLon\\HeThongDauGia',
  batchFiles: batch.files,
  batchImportData: batch.batchImportData
};

fs.writeFileSync('D:\\1_Dung\\LapTrinhNangCao\\BaiTapLon\\HeThongDauGia\\.understand-anything\\tmp\\ua-file-analyzer-input-3.json', JSON.stringify(input, null, 2));
