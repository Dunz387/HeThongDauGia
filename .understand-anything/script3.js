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
      summary = 'T\u1ec7p \u0111\u1ecbnh d\u1ea1ng CSS chung, cung c\u1ea5p c\u00e1c bi\u1ebfn v\u00e0 phong c\u00e1ch m\u00e0u s\u1eafc, n\u1ec1n t\u1ea3ng cho to\u00e0n b\u1ed9 \u1ee9ng d\u1ee5ng.';
      tags = ['styling', 'theme', 'ui-component'];
    } else if (path.includes('chart.css')) {
      summary = '\u0110\u1ecbnh d\u1ea1ng CSS d\u00e0nh ri\u00eang cho c\u00e1c bi\u1ec3u \u0111\u1ed3, gi\u00fap hi\u1ec3n th\u1ecb d\u1eef li\u1ec7u th\u1ed1ng k\u00ea tr\u1ef1c quan.';
      tags = ['styling', 'chart', 'ui-component'];
    } else if (path.includes('menu.css')) {
      summary = 'T\u1ec7p CSS quy \u0111\u1ecbnh giao di\u1ec7n v\u00e0 hi\u1ec7u \u1ee9ng cho thanh \u0111i\u1ec1u h\u01b0\u1edbng v\u00e0 menu c\u1ee7a \u1ee9ng d\u1ee5ng.';
      tags = ['styling', 'navigation', 'ui-component'];
    } else if (path.includes('table-view.css')) {
      summary = '\u0110\u1ecbnh d\u1ea1ng ki\u1ec3u d\u00e1ng cho c\u00e1c b\u1ea3ng d\u1eef li\u1ec7u, c\u1ea5u h\u00ecnh giao di\u1ec7n d\u00f2ng v\u00e0 c\u1ed9t trong JavaFX.';
      tags = ['styling', 'table', 'ui-component'];
    }
  } else if (isPNG) {
    summary = 'S\u01a1 \u0111\u1ed3 UML t\u1ed5ng quan ki\u1ebfn tr\u00fac h\u1ec7 th\u1ed1ng, th\u1ec3 hi\u1ec7n c\u00e1c th\u00e0nh ph\u1ea7n v\u00e0 m\u1ed1i quan h\u1ec7 trong \u1ee9ng d\u1ee5ng.';
    tags = ['documentation', 'architecture', 'uml'];
  } else if (path.includes('AuctionConcurrencyTest.java')) {
    summary = 'L\u1edbp ki\u1ec3m th\u1eed \u0111a lu\u1ed3ng cho ch\u1ee9c n\u0103ng \u0111\u1ea5u gi\u00e1, \u0111\u1ea3m b\u1ea3o t\u00ednh nh\u1ea5t qu\u00e1n d\u1eef li\u1ec7u khi nhi\u1ec1u ng\u01b0\u1eddi d\u00f9ng tr\u1ea3 gi\u00e1 c\u00f9ng l\u00fac.';
    tags = ['test', 'concurrency', 'auction'];
  } else if (path.includes('BidderTest.java')) {
    summary = 'L\u1edbp ki\u1ec3m th\u1eed \u0111\u01a1n v\u1ecb cho c\u00e1c thao t\u00e1c c\u1ee7a ng\u01b0\u1eddi mua nh\u01b0 kh\u00f3a s\u1ed1 d\u01b0 v\u00e0 tr\u1eeb ti\u1ec1n.';
    tags = ['test', 'unit-test', 'bidder'];
  } else if (path.includes('SellerTest.java')) {
    summary = 'L\u1edbp ki\u1ec3m th\u1eed \u0111\u01a1n v\u1ecb cho ng\u01b0\u1eddi b\u00e1n, ki\u1ec3m tra t\u00ednh \u0111\u00fang \u0111\u1eafn c\u1ee7a ch\u1ee9c n\u0103ng nh\u1eadn ti\u1ec1n v\u00e0 tr\u1eeb ph\u00ed.';
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
        let clsSummary = `L\u1edbp ${cls.name} trong h\u1ec7 th\u1ed1ng.`;
        if (cls.name === 'AuctionConcurrencyTest') clsSummary = 'L\u1edbp ki\u1ec3m th\u1eed cho t\u00ednh n\u0103ng \u0111\u1ea5u gi\u00e1 \u0111\u1ed3ng th\u1eddi.';
        if (cls.name === 'BidderTest') clsSummary = 'L\u1edbp ki\u1ec3m th\u1eed logic t\u00e0i ch\u00ednh c\u1ee7a Bidder.';
        if (cls.name === 'SellerTest') clsSummary = 'L\u1edbp ki\u1ec3m th\u1eed logic t\u00e0i ch\u00ednh c\u1ee7a Seller.';
        
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
          summary: `Ph\u01b0\u01a1ng th\u1ee9c ${fn.name} x\u1eed l\u00fd logic ki\u1ec3m th\u1eed.`,
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
