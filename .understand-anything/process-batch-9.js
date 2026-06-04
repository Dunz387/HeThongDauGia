const fs = require('fs');
const path = require('path');

const inputPath = 'D:\\1_Dung\\LapTrinhNangCao\\BaiTapLon\\HeThongDauGia\\.understand-anything\\tmp\\ua-file-analyzer-input-9.json';
const resultsPath = 'D:\\1_Dung\\LapTrinhNangCao\\BaiTapLon\\HeThongDauGia\\.understand-anything\\tmp\\ua-file-extract-results-9.json';
const outDir = 'D:\\1_Dung\\LapTrinhNangCao\\BaiTapLon\\HeThongDauGia\\.understand-anything\\intermediate';

const inputData = JSON.parse(fs.readFileSync(inputPath, 'utf8'));
const resultsData = JSON.parse(fs.readFileSync(resultsPath, 'utf8'));

const { batchImportData = {}, neighborMap = {} } = inputData;

// Retrieve all nodes and edges unpartitioned first to count them
let allNodes = [];
let allEdges = [];

function getFileSummary(file) {
    const ext = path.extname(file.path).toLowerCase();
    const name = path.basename(file.path);
    const category = file.fileCategory;
    const isJava = file.language === 'java';
    
    if (isJava) {
        if (name.includes('Exception') || name.includes('Error')) return 'Định nghĩa ngoại lệ hoặc lỗi tùy chỉnh cho hệ thống.';
        if (name.includes('Test')) return 'Lớp kiểm thử tự động (unit test) cho các thành phần trong hệ thống.';
        if (name.includes('Controller')) return 'Trình điều khiển (controller) xử lý giao diện người dùng và tương tác.';
        if (name.includes('Service')) return 'Lớp dịch vụ (service) chứa logic nghiệp vụ cốt lõi.';
        if (name.includes('Repository') || name.includes('DAO')) return 'Lớp truy xuất dữ liệu từ cơ sở dữ liệu.';
        if (name.includes('Model') || file.path.includes('model')) return 'Mô hình dữ liệu (data model) đại diện cho các thực thể trong hệ thống.';
        if (name.includes('Util') || name.includes('Manager') || name.includes('Helper')) return 'Lớp tiện ích cung cấp các hàm hỗ trợ chung.';
        return 'Lớp Java chứa logic xử lý của ứng dụng.';
    }
    if (ext === '.fxml') return 'Tệp giao diện người dùng FXML định nghĩa cấu trúc màn hình.';
    if (ext === '.css') return 'Tệp định dạng CSS để trang trí và tạo kiểu cho giao diện FXML.';
    if (ext === '.xml' || ext === '.json' || ext === '.yml' || ext === '.yaml' || category === 'config') return 'Tệp cấu hình của hệ thống.';
    if (ext === '.md' || category === 'docs') return 'Tệp tài liệu mô tả thông tin, hướng dẫn hoặc quy ước của dự án.';
    if (ext === '.sql' || category === 'data') return 'Tệp dữ liệu hoặc truy vấn cơ sở dữ liệu.';
    return 'Tệp mã nguồn hoặc cấu hình của dự án.';
}

function getFileTags(file) {
    const ext = path.extname(file.path).toLowerCase();
    const name = path.basename(file.path);
    const isJava = file.language === 'java';
    
    const tags = [];
    if (isJava) {
        if (name.includes('Controller')) tags.push('controller', 'ui-handler');
        if (name.includes('Service')) tags.push('service', 'business-logic');
        if (name.includes('Model') || file.path.includes('model')) tags.push('data-model', 'entity');
        if (name.includes('Util') || name.includes('Manager')) tags.push('utility', 'helper');
        if (name.includes('Exception')) tags.push('exception', 'error-handling');
        if (tags.length === 0) tags.push('component', 'java-class');
    } else if (ext === '.fxml') {
        tags.push('ui-view', 'layout');
    } else if (ext === '.css') {
        tags.push('styling', 'theme');
    } else if (file.fileCategory === 'config') {
        tags.push('configuration', 'settings');
    } else if (file.fileCategory === 'docs') {
        tags.push('documentation', 'guide');
    } else {
        tags.push('file', 'source');
    }
    
    while (tags.length < 3) tags.push('module');
    return tags.slice(0, 5);
}

function getNodeType(file) {
    const cat = file.fileCategory;
    if (cat === 'code') return 'file';
    if (cat === 'config') return 'config';
    if (cat === 'docs') return 'document';
    if (cat === 'markup') return 'file';
    if (cat === 'data') {
        if (file.path.endsWith('.sql')) return 'table';
        if (file.path.endsWith('.graphql') || file.path.endsWith('.proto')) return 'schema';
        return 'file';
    }
    return 'file';
}

function getComplexity(lines) {
    if (lines < 50) return 'simple';
    if (lines < 200) return 'moderate';
    return 'complex';
}

resultsData.results.forEach(file => {
    const fileId = `${getNodeType(file)}:${file.path}`;
    const fileComplexity = getComplexity(file.nonEmptyLines);
    
    allNodes.push({
        id: fileId,
        type: getNodeType(file),
        name: path.basename(file.path),
        summary: getFileSummary(file),
        tags: getFileTags(file),
        complexity: fileComplexity,
        filePath: file.path
    });

    const exportedItems = new Set((file.exports || []).map(e => e.name));
    
    if (file.classes) {
        file.classes.forEach(cls => {
            const lines = cls.endLine - cls.startLine;
            if (lines >= 20 || (cls.methods && cls.methods.length >= 2) || exportedItems.has(cls.name)) {
                const classId = `class:${file.path}:${cls.name}`;
                allNodes.push({
                    id: classId,
                    type: 'class',
                    name: cls.name,
                    summary: `Lớp ${cls.name} chứa cấu trúc và hành vi nghiệp vụ.`,
                    tags: ['java-class', 'oop-component', 'structure'],
                    complexity: getComplexity(lines),
                    filePath: file.path,
                    lineRange: [cls.startLine, cls.endLine]
                });
                
                allEdges.push({
                    source: fileId,
                    target: classId,
                    type: 'contains',
                    direction: 'forward',
                    weight: 1.0
                });
                
                if (exportedItems.has(cls.name)) {
                    allEdges.push({
                        source: fileId,
                        target: classId,
                        type: 'exports',
                        direction: 'forward',
                        weight: 0.8
                    });
                }
            }
        });
    }

    if (file.functions) {
        file.functions.forEach(func => {
            const lines = func.endLine - func.startLine;
            if (lines >= 10 || exportedItems.has(func.name)) {
                const funcId = `function:${file.path}:${func.name}`;
                allNodes.push({
                    id: funcId,
                    type: 'function',
                    name: func.name,
                    summary: `Hàm ${func.name} thực hiện logic xử lý cụ thể.`,
                    tags: ['function', 'method', 'logic'],
                    complexity: getComplexity(lines),
                    filePath: file.path,
                    lineRange: [func.startLine, func.endLine]
                });
                
                allEdges.push({
                    source: fileId,
                    target: funcId,
                    type: 'contains',
                    direction: 'forward',
                    weight: 1.0
                });
                
                if (exportedItems.has(func.name)) {
                    allEdges.push({
                        source: fileId,
                        target: funcId,
                        type: 'exports',
                        direction: 'forward',
                        weight: 0.8
                    });
                }
            }
        });
    }

    if (batchImportData[file.path]) {
        batchImportData[file.path].forEach(importedPath => {
            allEdges.push({
                source: fileId,
                target: `file:${importedPath}`,
                type: 'imports',
                direction: 'forward',
                weight: 0.7
            });
        });
    }
});

const nodeCount = allNodes.length;
const edgeCount = allEdges.length;

let parts = 1;
if (nodeCount > 60 || edgeCount > 120) {
    parts = Math.ceil(Math.max(nodeCount / 60, edgeCount / 120));
}

if (parts === 1) {
    fs.writeFileSync(path.join(outDir, 'batch-9.json'), JSON.stringify({ nodes: allNodes, edges: allEdges }, null, 2));
    console.log(`Wrote batch-9.json with ${nodeCount} nodes and ${edgeCount} edges.`);
} else {
    // Sort files alphabetically
    const files = [...resultsData.results].map(r => r.path).sort();
    const chunkSize = Math.ceil(files.length / parts);
    
    for (let i = 0; i < parts; i++) {
        const chunkFiles = new Set(files.slice(i * chunkSize, (i + 1) * chunkSize));
        
        const chunkNodes = allNodes.filter(n => chunkFiles.has(n.filePath));
        const chunkNodeIds = new Set(chunkNodes.map(n => n.id));
        
        const chunkEdges = allEdges.filter(e => chunkNodeIds.has(e.source));
        
        const partFile = path.join(outDir, `batch-9-part-${i + 1}.json`);
        fs.writeFileSync(partFile, JSON.stringify({ nodes: chunkNodes, edges: chunkEdges }, null, 2));
        console.log(`Wrote ${path.basename(partFile)} with ${chunkNodes.length} nodes and ${chunkEdges.length} edges.`);
    }
}
