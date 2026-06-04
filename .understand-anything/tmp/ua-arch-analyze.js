const fs = require('fs');

function main() {
  const args = process.argv.slice(2);
  if (args.length < 2) {
    console.error("Usage: node ua-arch-analyze.js <input.json> <output.json>");
    process.exit(1);
  }

  const inputPath = args[0];
  const outputPath = args[1];

  let data;
  try {
    data = JSON.parse(fs.readFileSync(inputPath, 'utf8'));
  } catch (err) {
    console.error("Error reading input file:", err.message);
    process.exit(1);
  }

  const { fileNodes, allEdges, importEdges } = data;

  // A. Directory Grouping
  const dirGroups = {};
  fileNodes.forEach(node => {
    let filePath = node.filePath || (node.id && node.id.replace(/^[^:]+:/, ''));
    if (!filePath) return;
    
    // Normalize path separators
    filePath = filePath.replace(/\\/g, '/');
    let dir = filePath.includes('/') ? filePath.substring(0, filePath.lastIndexOf('/')) : '.';
    
    // Attempt to group by src/main/java/xyz etc if Java
    if (filePath.startsWith('src/main/java/')) {
       let rel = filePath.substring('src/main/java/'.length);
       let parts = rel.split('/');
       if (parts.length > 2) {
           dir = 'src/main/java/' + parts.slice(0, -1).join('/'); // Package level
       } else if (parts.length > 1) {
           dir = 'src/main/java/' + parts[0];
       }
    } else if (filePath.startsWith('src/main/resources/')) {
       dir = 'src/main/resources';
    } else if (filePath.startsWith('src/test/')) {
       dir = 'src/test';
    }
    
    if (!dirGroups[dir]) dirGroups[dir] = [];
    dirGroups[dir].push(node.id);
  });

  // B. Node Type Grouping
  const typeGroups = {};
  fileNodes.forEach(node => {
    const t = node.type || 'file';
    if (!typeGroups[t]) typeGroups[t] = [];
    typeGroups[t].push(node.id);
  });

  // Helper map for node -> dir
  const nodeToDir = {};
  for (const [dir, nodes] of Object.entries(dirGroups)) {
    for (const n of nodes) {
      nodeToDir[n] = dir;
    }
  }

  // C. Import Adjacency Matrix & D, E, F
  const interGroupImports = {};
  const dirFanOut = {};
  const dirFanIn = {};

  importEdges.forEach(edge => {
    const srcDir = nodeToDir[edge.source];
    const tgtDir = nodeToDir[edge.target];
    if (srcDir && tgtDir) {
      if (!interGroupImports[srcDir]) interGroupImports[srcDir] = {};
      interGroupImports[srcDir][tgtDir] = (interGroupImports[srcDir][tgtDir] || 0) + 1;

      if (srcDir !== tgtDir) {
        if (!dirFanOut[srcDir]) dirFanOut[srcDir] = new Set();
        dirFanOut[srcDir].add(tgtDir);

        if (!dirFanIn[tgtDir]) dirFanIn[tgtDir] = new Set();
        dirFanIn[tgtDir].add(srcDir);
      }
    }
  });

  const results = {
    dirGroups,
    typeGroups,
    interGroupImports,
    dirFanOut: Object.fromEntries(Object.entries(dirFanOut).map(([k, v]) => [k, Array.from(v)])),
    dirFanIn: Object.fromEntries(Object.entries(dirFanIn).map(([k, v]) => [k, Array.from(v)]))
  };

  try {
    fs.writeFileSync(outputPath, JSON.stringify(results, null, 2), 'utf8');
  } catch (err) {
    console.error("Error writing output file:", err.message);
    process.exit(1);
  }
}

main();
