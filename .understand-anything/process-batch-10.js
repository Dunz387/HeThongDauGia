const fs = require('fs');

const batchIndex = 10;
const results = JSON.parse(fs.readFileSync('.understand-anything/tmp/ua-file-extract-results-10.json', 'utf8'));
const batches = JSON.parse(fs.readFileSync('.understand-anything/intermediate/batches.json', 'utf8'));
const batchData = batches.batches.find(b => b.batchIndex === batchIndex);

let nodes = [];
let edges = [];

const getNodeType = (fileCategory, filePath, lang) => {
    if (fileCategory === 'config') return 'config';
    if (fileCategory === 'docs') return 'document';
    if (fileCategory === 'infra') {
        if (filePath.includes('Dockerfile') || filePath.includes('docker-compose')) return 'service';
        if (filePath.includes('.github/') || filePath.includes('.gitlab')) return 'pipeline';
        return 'resource';
    }
    if (fileCategory === 'data') {
        if (filePath.endsWith('.sql')) return 'table';
        if (filePath.endsWith('.graphql') || filePath.endsWith('.proto') || filePath.endsWith('.prisma')) return 'schema';
        return 'endpoint';
    }
    return 'file'; // code, script, markup
};

const getSummaryAndTags = (fileCategory, filePath, metrics) => {
    let summary = '';
    let tags = [];
    if (filePath.endsWith('.java')) {
        summary = 'L?p ti?n ích ho?c c?u hình Java h? tr? các ch?c nang c? th? c?a ?ng d?ng.';
        tags = ['java', 'utility', 'helper'];
        if (filePath.includes('Notification')) {
            summary = 'H? tr? x? lý và qu?n lý thông báo cho h? th?ng d?u giá.';
            tags = ['notification', 'utility', 'ui-component'];
        } else if (filePath.includes('Validation')) {
            summary = 'Cung c?p các hàm ki?m tra và xác th?c d? li?u d?u vào c?a ngu?i dùng.';
            tags = ['validation', 'utility', 'security'];
        } else if (filePath.includes('Table') || filePath.includes('Cell')) {
            summary = 'L?p ti?n ích tùy ch?nh cách hi?n th? d? li?u trên b?ng giao di?n.';
            tags = ['table', 'ui-component', 'utility'];
        }
    } else if (filePath.endsWith('.fxml')) {
        summary = 'T?p giao di?n FXML d?nh nghia c?u trúc giao di?n ngu?i dùng cho ?ng d?ng JavaFX.';
        tags = ['fxml', 'ui-view', 'frontend'];
        if (filePath.includes('admin')) {
            summary = 'Giao di?n FXML dành cho b?ng di?u khi?n ho?c ch?c nang qu?n tr? viên.';
            tags = ['fxml', 'admin', 'ui-view', 'dashboard'];
        } else if (filePath.includes('auction') || filePath.includes('Room')) {
            summary = 'Giao di?n FXML hi?n th? phòng d?u giá và các tuong tác d?u giá.';
            tags = ['fxml', 'auction', 'ui-view', 'room'];
        } else if (filePath.includes('auth')) {
            summary = 'Giao di?n FXML cho ch?c nang xác th?c ngu?i dùng (dang nh?p/dang ký).';
            tags = ['fxml', 'auth', 'ui-view'];
        } else if (filePath.includes('seller')) {
            summary = 'Giao di?n FXML dành riêng cho các ch?c nang c?a ngu?i bán hàng.';
            tags = ['fxml', 'seller', 'ui-view'];
        }
    } else if (filePath.endsWith('.css')) {
        summary = 'T?p d?nh d?ng CSS d? tùy ch?nh giao di?n ngu?i dùng cho các thành ph?n JavaFX.';
        tags = ['css', 'styling', 'frontend'];
    } else if (filePath.endsWith('.png') || filePath.endsWith('.jpg') || filePath.endsWith('.jpeg')) {
        summary = 'T?p hình ?nh du?c s? d?ng làm tài nguyên d? h?a cho giao di?n ngu?i dùng.';
        tags = ['image', 'asset', 'resource'];
    } else if (filePath.endsWith('.MF')) {
        summary = 'T?p tin Manifest ch?a siêu d? li?u v? gói ?ng d?ng Java.';
        tags = ['manifest', 'configuration', 'build'];
    } else {
        summary = 'T?p mã ngu?n ho?c tài nguyên c?a d? án.';
        tags = ['file', 'resource'];
    }
    return { summary, tags };
};

const getComplexity = (nonEmptyLines) => {
    if (nonEmptyLines < 50) return 'simple';
    if (nonEmptyLines <= 200) return 'moderate';
    return 'complex';
};

for (const file of results.results) {
    const { path, language, fileCategory, nonEmptyLines, functions = [], classes = [], exports = [], metrics = {} } = file;
    
    let type = getNodeType(fileCategory, path, language);
    let { summary, tags } = getSummaryAndTags(fileCategory, path, metrics);
    let complexity = getComplexity(nonEmptyLines);

    let fileNodeId = type + ':' + path;
    if (type === 'file') {
        fileNodeId = 'file:' + path;
    }
    
    let fileName = path.split('/').pop();

    nodes.push({
        id: fileNodeId,
        type: type,
        name: fileName,
        summary: summary,
        tags: tags,
        complexity: complexity,
        filePath: path
    });

    // functions
    for (const fn of functions) {
        if ((fn.endLine - fn.startLine + 1) >= 10 || exports.some(e => e.name === fn.name)) {
            let fnId = 'function:' + path + ':' + fn.name;
            nodes.push({
                id: fnId,
                type: 'function',
                name: fn.name,
                summary: 'Hàm ' + fn.name + ' th?c hi?n ch?c nang c? th? trong ' + fileName + '.',
                tags: ['function', 'logic'],
                complexity: getComplexity(fn.endLine - fn.startLine + 1),
                lineRange: [fn.startLine, fn.endLine]
            });
            edges.push({
                source: fileNodeId,
                target: fnId,
                type: 'contains',
                direction: 'forward',
                weight: 1.0
            });
            if (exports.some(e => e.name === fn.name)) {
                edges.push({
                    source: fileNodeId,
                    target: fnId,
                    type: 'exports',
                    direction: 'forward',
                    weight: 0.8
                });
            }
        }
    }

    // classes
    for (const cls of classes) {
        if ((cls.endLine - cls.startLine + 1) >= 20 || (cls.methods && cls.methods.length >= 2) || exports.some(e => e.name === cls.name)) {
            let clsId = 'class:' + path + ':' + cls.name;
            nodes.push({
                id: clsId,
                type: 'class',
                name: cls.name,
                summary: 'L?p ' + cls.name + ' dóng gói d? li?u và logic x? lý trong ' + fileName + '.',
                tags: ['class', 'oop'],
                complexity: getComplexity(cls.endLine - cls.startLine + 1),
                lineRange: [cls.startLine, cls.endLine]
            });
            edges.push({
                source: fileNodeId,
                target: clsId,
                type: 'contains',
                direction: 'forward',
                weight: 1.0
            });
            if (exports.some(e => e.name === cls.name)) {
                edges.push({
                    source: fileNodeId,
                    target: clsId,
                    type: 'exports',
                    direction: 'forward',
                    weight: 0.8
                });
            }
        }
    }

    // imports
    const fileImports = batchData.batchImportData[path] || [];
    for (const imp of fileImports) {
        edges.push({
            source: fileNodeId,
            target: 'file:' + imp,
            type: 'imports',
            direction: 'forward',
            weight: 0.7
        });
    }
}

const nodeCount = nodes.length;
const edgeCount = edges.length;

if (nodeCount <= 60 && edgeCount <= 120) {
    fs.writeFileSync('.understand-anything/intermediate/batch-' + batchIndex + '.json', JSON.stringify({nodes, edges}, null, 2));
    console.log('Wrote 1 part. Nodes: ' + nodeCount + ', Edges: ' + edgeCount);
} else {
    const parts = Math.ceil(Math.max(nodeCount / 60, edgeCount / 120));
    // Sort files by path for deterministic chunking
    const files = [...new Set(nodes.map(n => n.filePath).filter(Boolean))].sort();
    
    for (let i = 0; i < parts; i++) {
        const startIdx = Math.floor(i * files.length / parts);
        const endIdx = Math.floor((i + 1) * files.length / parts);
        const partFiles = files.slice(startIdx, endIdx);
        
        const partNodes = nodes.filter(n => partFiles.includes(n.filePath));
        const partNodeIds = new Set(partNodes.map(n => n.id));
        const partEdges = edges.filter(e => partNodeIds.has(e.source));
        
        fs.writeFileSync('.understand-anything/intermediate/batch-' + batchIndex + '-part-' + (i + 1) + '.json', JSON.stringify({nodes: partNodes, edges: partEdges}, null, 2));
        console.log('Wrote part ' + (i + 1) + '. Nodes: ' + partNodes.length + ', Edges: ' + partEdges.length);
    }
}
