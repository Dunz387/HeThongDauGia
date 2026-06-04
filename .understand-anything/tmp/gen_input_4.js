const fs = require('fs');
const root = 'D:\\\\1_Dung\\\\LapTrinhNangCao\\\\BaiTapLon\\\\HeThongDauGia';
const data = JSON.parse(fs.readFileSync(root + '\\\\.understand-anything\\\\intermediate\\\\batches.json', 'utf8'));
const batch = data.batches.find(b => b.batchIndex === 4);
const input = {
  projectRoot: root,
  batchFiles: batch.files,
  batchImportData: batch.batchImportData,
  neighborMap: batch.neighborMap
};
fs.mkdirSync(root + '\\\\.understand-anything\\\\tmp', {recursive: true});
fs.writeFileSync(root + '\\\\.understand-anything\\\\tmp\\\\ua-file-analyzer-input-4.json', JSON.stringify(input, null, 2));
console.log('Done creating input for batch 4. Number of files: ' + batch.files.length);
