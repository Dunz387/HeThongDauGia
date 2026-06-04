const fs = require('fs');
const path = require('path');

const inputPath = 'ua-file-analyzer-input-7.json';
const resultsPath = 'ua-file-extract-results-7.json';

const inputData = JSON.parse(fs.readFileSync(inputPath, 'utf8'));
const extractData = JSON.parse(fs.readFileSync(resultsPath, 'utf8'));
const batchImportData = inputData.batchImportData;

const summaryMap = {
  ".gitignore": "Cấu hình bỏ qua các tệp không cần thiết khi quản lý mã nguồn bằng Git.",
  ".understand-anything/.understandignore": "Cấu hình bỏ qua các tệp khi quét dự án bằng công cụ Understand Anything.",
  ".understand-anything/tmp/scan-project-patched.mjs": "Tập lệnh JavaScript quét mã nguồn dự án, xác định ngôn ngữ và phân loại tệp.",
  "2026-Bài-tập-lớn.pdf": "Tài liệu mô tả yêu cầu bài tập lớn năm 2026 cho hệ thống đấu giá.",
  "auction.db": "Cơ sở dữ liệu SQLite lưu trữ thông tin người dùng, phiên đấu giá và giao dịch.",
  "HeThongDauGia.iml": "Tệp cấu hình dự án IntelliJ IDEA cho Hệ thống đấu giá.",
  "src/main/java/dao/AdminDAO.java": "Lớp DAO xử lý các tác vụ của quản trị viên như cập nhật trạng thái người dùng.",
  "src/main/java/dao/AuctionDAO.java": "Lớp DAO cho các phiên đấu giá, bao gồm lưu trữ, cập nhật và tải lịch sử.",
  "src/main/java/dao/BaseDAO.java": "Lớp cơ sở cung cấp các phương thức dùng chung (executeUpdate, executeQuery) cho các lớp DAO.",
  "src/main/java/dao/DatabaseManager.java": "Trình quản lý cơ sở dữ liệu chịu trách nhiệm khởi tạo bảng và thực thi các câu lệnh SQL ban đầu.",
  "src/main/java/dao/DBConnection.java": "Lớp kết nối cơ sở dữ liệu SQLite, quản lý vòng đời và cấu hình kết nối.",
  "src/main/java/dao/NotificationDAO.java": "Lớp DAO quản lý, lưu trữ và truy xuất các thông báo trong hệ thống.",
  "src/main/java/dao/UserDAO.java": "Lớp xử lý thao tác với bảng người dùng, hỗ trợ xác thực và quản lý hồ sơ người dùng.",
  "src/main/java/exception/AuctionClosedException.java": "Ngoại lệ được ném ra khi cố gắng thực hiện hành động trên một phiên đấu giá đã đóng.",
  "src/main/java/exception/InvalidBidException.java": "Ngoại lệ được ném ra khi mức giá đặt cược không hợp lệ.",
  "src/main/java/model/auction/Auction.java": "Mô hình dữ liệu đại diện cho một phiên đấu giá, chứa thông tin vật phẩm, giá và trạng thái.",
  "src/main/java/model/auction/AuctionObserver.java": "Giao diện Observer định nghĩa phương thức cập nhật cho người theo dõi phiên đấu giá.",
  "src/main/java/model/auction/AuctionStatus.java": "Enum định nghĩa các trạng thái của một phiên đấu giá (ví dụ: OPEN, CLOSED).",
  "src/main/java/model/auction/AuctionSubject.java": "Giao diện Subject định nghĩa các phương thức quản lý người theo dõi và phát thông báo.",
  "src/main/java/model/auction/BidTransaction.java": "Mô hình lưu trữ thông tin về một giao dịch đặt giá trong phiên đấu giá.",
  "src/main/java/model/base/Entity.java": "Lớp cơ sở trừu tượng đại diện cho một thực thể, cung cấp thuộc tính ID cơ bản.",
  "src/main/java/model/item/Arts.java": "Mô hình dữ liệu đại diện cho các tác phẩm nghệ thuật tham gia đấu giá.",
  "src/main/java/model/item/Electronics.java": "Mô hình dữ liệu đại diện cho các mặt hàng điện tử tham gia đấu giá.",
  "src/main/java/model/item/Item.java": "Lớp trừu tượng đại diện cho một vật phẩm cơ bản có thể được đem ra đấu giá.",
  "src/main/java/model/item/ItemBuilder.java": "Lớp sử dụng mẫu thiết kế Builder dùng để khởi tạo đối tượng vật phẩm một cách linh hoạt."
};

function getViTags(filePath, category) {
  const tags = [];
  const p = filePath.toLowerCase();
  
  if (p.includes('dao/')) tags.push('database', 'service');
  else if (p.includes('model/')) tags.push('data-model');
  else if (p.includes('exception/')) tags.push('error-handling', 'utility');
  else if (p.endsWith('.java')) tags.push('component', 'utility');
  
  if (category === 'config' || p.endsWith('.gitignore') || p.endsWith('.understandignore') || p.endsWith('.iml')) {
    tags.push('configuration');
  }
  if (category === 'document' || p.endsWith('.pdf')) tags.push('documentation');
  if (category === 'data' || p.endsWith('.db')) tags.push('database', 'infrastructure');

  // Đảm bảo đủ 3-5 tags
  if (!tags.length) tags.push('module', 'file');
  while (tags.length < 3) tags.push('component');
  return tags.slice(0, 5);
}

function getComplexity(metrics, lines) {
  if (!lines || lines < 50) return 'simple';
  if (lines <= 200) return 'moderate';
  return 'complex';
}

function getTypePrefix(fileCategory, filePath) {
  const p = filePath.toLowerCase();
  if (fileCategory === 'config' || p.endsWith('.gitignore') || p.endsWith('.understandignore') || p.endsWith('.iml')) return 'config';
  if (fileCategory === 'document' || p.endsWith('.pdf')) return 'document';
  if (fileCategory === 'data' || p.endsWith('.db') || p.endsWith('.sql')) return 'table';
  if (fileCategory === 'infra') return 'resource';
  return 'file';
}

let nodes = [];
let edges = [];

extractData.results.forEach(res => {
  const filePath = res.path;
  const category = res.fileCategory;
  const metrics = res.metrics || {};
  const totalLines = res.totalLines || 0;

  const nodeType = getTypePrefix(category, filePath);
  const fileId = `${nodeType}:${filePath}`;

  let summary = summaryMap[filePath] || "Tệp hệ thống đấu giá.";
  
  let languageNotes = undefined;
  if (filePath.endsWith('.java') && res.classes && res.classes.length > 0) {
      languageNotes = "Sử dụng tính năng hướng đối tượng tiêu chuẩn của Java.";
  }

  nodes.push({
    id: fileId,
    type: nodeType,
    name: path.basename(filePath),
    summary: summary,
    tags: getViTags(filePath, category),
    complexity: getComplexity(metrics, totalLines),
    filePath: filePath,
    languageNotes: languageNotes
  });

  // Functions
  if (res.functions) {
    res.functions.forEach(fn => {
      const fnLines = (fn.endLine - fn.startLine) + 1;
      const isExported = res.exports && res.exports.some(e => e.name === fn.name);
      if (fnLines >= 10 || isExported) {
        const fnId = `function:${filePath}:${fn.name}`;
        nodes.push({
          id: fnId,
          type: 'function',
          name: fn.name,
          summary: `Hàm ${fn.name} xử lý logic nghiệp vụ cho ${path.basename(filePath)}.`,
          tags: ['utility', 'function'],
          complexity: getComplexity({}, fnLines),
          filePath: filePath,
          lineRange: [fn.startLine, fn.endLine]
        });
        
        edges.push({
          source: fileId,
          target: fnId,
          type: 'contains',
          direction: 'forward',
          weight: 1.0
        });

        if (isExported) {
          edges.push({
            source: fileId,
            target: fnId,
            type: 'exports',
            direction: 'forward',
            weight: 0.8
          });
        }
      }
    });
  }

  // Classes
  if (res.classes) {
    res.classes.forEach(cls => {
      const clsLines = (cls.endLine - cls.startLine) + 1;
      const methodCount = cls.methods ? cls.methods.length : 0;
      const isExported = res.exports && res.exports.some(e => e.name === cls.name);
      
      if (clsLines >= 20 || methodCount >= 2 || isExported) {
        const clsId = `class:${filePath}:${cls.name}`;
        nodes.push({
          id: clsId,
          type: 'class',
          name: cls.name,
          summary: `Lớp ${cls.name} đại diện cho thành phần cốt lõi trong mô hình ứng dụng.`,
          tags: ['component', 'class'],
          complexity: getComplexity({}, clsLines),
          filePath: filePath,
          lineRange: [cls.startLine, cls.endLine]
        });

        edges.push({
          source: fileId,
          target: clsId,
          type: 'contains',
          direction: 'forward',
          weight: 1.0
        });

        if (isExported) {
          edges.push({
            source: fileId,
            target: clsId,
            type: 'exports',
            direction: 'forward',
            weight: 0.8
          });
        }
      }
    });
  }

  // Imports
  if (batchImportData && batchImportData[filePath]) {
    batchImportData[filePath].forEach(importedPath => {
      const impCategory = 'code'; 
      const impNodeType = getTypePrefix(impCategory, importedPath);
      edges.push({
        source: fileId,
        target: `${impNodeType}:${importedPath}`,
        type: 'imports',
        direction: 'forward',
        weight: 0.7
      });
    });
  }
});

const outDir = 'D:/1_Dung/LapTrinhNangCao/BaiTapLon/HeThongDauGia/.understand-anything/intermediate';

let nodeCount = nodes.length;
let edgeCount = edges.length;

if (nodeCount <= 60 && edgeCount <= 120) {
  fs.writeFileSync(path.join(outDir, 'batch-7.json'), JSON.stringify({ nodes, edges }, null, 2));
  console.log(`Wrote batch-7.json with ${nodeCount} nodes and ${edgeCount} edges.`);
} else {
  const parts = Math.ceil(Math.max(nodeCount / 60, edgeCount / 120));
  
  const filesList = Array.from(new Set(nodes.map(n => n.filePath).filter(Boolean))).sort();
  
  for (let k = 1; k <= parts; k++) {
    const startIdx = Math.floor((k - 1) * filesList.length / parts);
    const endIdx = Math.floor(k * filesList.length / parts);
    const partFiles = new Set(filesList.slice(startIdx, endIdx));
    
    const partNodes = nodes.filter(n => partFiles.has(n.filePath));
    const partNodeIds = new Set(partNodes.map(n => n.id));
    
    const partEdges = edges.filter(e => partNodeIds.has(e.source));
    
    fs.writeFileSync(path.join(outDir, `batch-7-part-${k}.json`), JSON.stringify({ nodes: partNodes, edges: partEdges }, null, 2));
    console.log(`Wrote batch-7-part-${k}.json with ${partNodes.length} nodes and ${partEdges.length} edges.`);
  }
}
