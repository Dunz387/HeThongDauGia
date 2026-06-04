const fs = require('fs');

const inputStr = fs.readFileSync('D:/1_Dung/LapTrinhNangCao/BaiTapLon/HeThongDauGia/.understand-anything/tmp/ua-file-analyzer-input-11.json', 'utf8');
const input = JSON.parse(inputStr);

const resultsStr = fs.readFileSync('D:/1_Dung/LapTrinhNangCao/BaiTapLon/HeThongDauGia/.understand-anything/tmp/ua-file-extract-results-11.json', 'utf8');
const results = JSON.parse(resultsStr).results;

const nodes = [];
const edges = [];

const fileTypes = {
  'code': 'file',
  'config': 'config',
  'docs': 'document',
  'markup': 'file',
  'script': 'file',
  'data': 'schema',
  'infra': 'service'
};

results.forEach(res => {
  const path = res.path;
  const isCSS = path.endsWith('.css');
  const isPNG = path.endsWith('.png');
  let type = fileTypes[res.fileCategory] || 'file';
  
  if (isPNG) type = 'document';
  
  let summary = '';
  let tags = [];
  
  if (isCSS) {
    if (path.includes('theme.css')) {
      summary = 'Tệp định dạng CSS chung, cung cấp các biến và phong cách màu sắc, nền tảng cho toàn bộ ứng dụng.';
      tags = ['styling', 'theme', 'ui-component'];
    } else if (path.includes('chart.css')) {
      summary = 'Định dạng CSS dành riêng cho các biểu đồ, giúp hiển thị dữ liệu thống kê trực quan.';
      tags = ['styling', 'chart', 'ui-component'];
    } else if (path.includes('menu.css')) {
      summary = 'Tệp CSS quy định giao diện và hiệu ứng cho thanh điều hướng và menu của ứng dụng.';
      tags = ['styling', 'navigation', 'ui-component'];
    } else if (path.includes('table-view.css')) {
      summary = 'Định dạng kiểu dáng cho các bảng dữ liệu, cấu hình giao diện dòng và cột trong JavaFX.';
      tags = ['styling', 'table', 'ui-component'];
    }
  } else if (isPNG) {
    summary = 'Sơ đồ UML tổng quan kiến trúc hệ thống, thể hiện các thành phần và mối quan hệ trong ứng dụng.';
    tags = ['documentation', 'architecture', 'uml'];
  } else if (path.includes('AuctionConcurrencyTest.java')) {
    summary = 'Lớp kiểm thử đa luồng cho chức năng đấu giá, đảm bảo tính nhất quán dữ liệu khi nhiều người dùng trả giá cùng lúc.';
    tags = ['test', 'concurrency', 'auction'];
  } else if (path.includes('BidderTest.java')) {
    summary = 'Lớp kiểm thử đơn vị cho các thao tác của người mua như khóa số dư và trừ tiền.';
    tags = ['test', 'unit-test', 'bidder'];
  } else if (path.includes('SellerTest.java')) {
    summary = 'Lớp kiểm thử đơn vị cho người bán, kiểm tra tính đúng đắn của chức năng nhận tiền và trừ phí.';
    tags = ['test', 'unit-test', 'seller'];
  }
  
  let complexity = 'simple';
  if (res.nonEmptyLines > 200) complexity = 'complex';
  else if (res.nonEmptyLines >= 50) complexity = 'moderate';
  
  if (isPNG) complexity = 'simple';
  
  // File Node
  nodes.push({
    id: type === 'document' ? `document:${path}` : `file:${path}`,
    type: type,
    name: path.split('/').pop(),
    summary: summary,
    tags: tags,
    complexity: complexity,
    filePath: path
  });
  
  // Functions and Classes (if any)
  if (res.classes && Array.isArray(res.classes)) {
    res.classes.forEach(cls => {
      // Create node for classes with 2+ methods or 20+ lines
      const classLines = cls.endLine - cls.startLine + 1;
      if (classLines >= 20 || (cls.methods && cls.methods.length >= 2)) {
        let clsSummary = `Lớp ${cls.name} trong hệ thống.`;
        if (cls.name === 'AuctionConcurrencyTest') clsSummary = 'Lớp kiểm thử cho tính năng đấu giá đồng thời.';
        if (cls.name === 'BidderTest') clsSummary = 'Lớp kiểm thử logic tài chính của Bidder.';
        if (cls.name === 'SellerTest') clsSummary = 'Lớp kiểm thử logic tài chính của Seller.';
        
        nodes.push({
          id: `class:${path}:${cls.name}`,
          type: 'class',
          name: cls.name,
          summary: clsSummary,
          tags: ['test', 'class', 'unit-test'],
          complexity: 'moderate',
          filePath: path,
          lineRange: [cls.startLine, cls.endLine]
        });
        
        edges.push({
          source: `file:${path}`,
          target: `class:${path}:${cls.name}`,
          type: 'contains',
          direction: 'forward',
          weight: 1.0
        });
      }
    });
  }
  
  if (res.functions && Array.isArray(res.functions)) {
    res.functions.forEach(fn => {
      // 10+ lines
      const lines = fn.endLine - fn.startLine + 1;
      if (lines >= 10) {
        nodes.push({
          id: `function:${path}:${fn.name}`,
          type: 'function',
          name: fn.name,
          summary: `Phương thức ${fn.name} xử lý logic kiểm thử.`,
          tags: ['test', 'method'],
          complexity: 'simple',
          filePath: path,
          lineRange: [fn.startLine, fn.endLine]
        });
        
        edges.push({
          source: `file:${path}`,
          target: `function:${path}:${fn.name}`,
          type: 'contains',
          direction: 'forward',
          weight: 1.0
        });
      }
    });
  }
  
  // imports edges
  const imports = input.batchImportData[path] || [];
  imports.forEach(imp => {
    edges.push({
      source: `file:${path}`,
      target: `file:${imp}`,
      type: 'imports',
      direction: 'forward',
      weight: 0.7
    });
  });
  
});

// tested_by edges inferred from name
const testTargets = {
  'src/test/java/model/auction/AuctionConcurrencyTest.java': 'src/main/java/model/auction/Auction.java',
  'src/test/java/model/user/BidderTest.java': 'src/main/java/model/user/Bidder.java',
  'src/test/java/model/user/SellerTest.java': 'src/main/java/model/user/Seller.java'
};

Object.entries(testTargets).forEach(([testPath, prodPath]) => {
  edges.push({
    source: `file:${prodPath}`,
    target: `file:${testPath}`,
    type: 'tested_by',
    direction: 'forward',
    weight: 0.5
  });
});

fs.writeFileSync('D:/1_Dung/LapTrinhNangCao/BaiTapLon/HeThongDauGia/.understand-anything/intermediate/batch-11.json', JSON.stringify({ nodes, edges }, null, 2));
console.log(`Wrote batch-11.json with ${nodes.length} nodes and ${edges.length} edges`);