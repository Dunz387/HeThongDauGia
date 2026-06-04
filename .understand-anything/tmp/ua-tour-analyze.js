const fs = require('fs');

function analyze(inputFile, outputFile) {
    const data = JSON.parse(fs.readFileSync(inputFile, 'utf-8'));
    const { nodes, edges, layers } = data;

    const fanIn = {};
    const fanOut = {};
    const adj = {};
    const nodeSummaryIndex = {};

    nodes.forEach(n => {
        fanIn[n.id] = 0;
        fanOut[n.id] = 0;
        adj[n.id] = [];
        nodeSummaryIndex[n.id] = { type: n.type, name: n.name, summary: n.summary };
    });

    edges.forEach(e => {
        if (fanIn[e.target] !== undefined) fanIn[e.target]++;
        if (fanOut[e.source] !== undefined) fanOut[e.source]++;
        if (adj[e.source] !== undefined) adj[e.source].push(e.target);
    });

    const topFanIn = [...nodes].sort((a, b) => fanIn[b.id] - fanIn[a.id]).slice(0, 20);
    const topFanOut = [...nodes].sort((a, b) => fanOut[b.id] - fanOut[a.id]).slice(0, 20);

    const fanOutArr = nodes.map(n => fanOut[n.id] || 0).sort((a, b) => a - b);
    const fanInArr = nodes.map(n => fanIn[n.id] || 0).sort((a, b) => a - b);
    
    const p90FanOut = fanOutArr[Math.floor(fanOutArr.length * 0.9)] || 0;
    const p25FanIn = fanInArr[Math.floor(fanInArr.length * 0.25)] || 0;

    const entryCandidates = nodes.map(n => {
        let score = 0;
        if (n.type === 'file') {
            const nameLower = n.name.toLowerCase();
            if (nameLower.match(/^(index|main|application|program|launcher|auctionserver)\./)) score += 3;
            if ((n.filePath || '').split(/[/\\]/).length <= 2) score += 1;
            if (fanOut[n.id] >= p90FanOut) score += 1;
            if (fanIn[n.id] <= p25FanIn) score += 1;
        } else if (n.type === 'document') {
            if (n.name.toLowerCase() === 'readme.md' && (n.filePath || '').split(/[/\\]/).length <= 2) score += 5;
            else if (n.name.toLowerCase().endsWith('.md') && (n.filePath || '').split(/[/\\]/).length <= 2) score += 2;
        }
        return { node: n, score };
    }).sort((a, b) => b.score - a.score).slice(0, 5);

    let bfsOrder = [];
    let bfsDepthMap = {};
    let bfsNodesByDepth = {};
    
    // Find top code entry point
    const codeEntries = entryCandidates.filter(c => c.node.type === 'file');
    if (codeEntries.length > 0) {
        const startNodeId = codeEntries[0].node.id;
        const queue = [{ id: startNodeId, depth: 0 }];
        const visited = new Set([startNodeId]);
        
        while (queue.length > 0) {
            const curr = queue.shift();
            bfsOrder.push(curr.id);
            bfsDepthMap[curr.id] = curr.depth;
            if (!bfsNodesByDepth[curr.depth]) bfsNodesByDepth[curr.depth] = [];
            bfsNodesByDepth[curr.depth].push(curr.id);
            
            (adj[curr.id] || []).forEach(neighbor => {
                if (!visited.has(neighbor)) {
                    visited.add(neighbor);
                    queue.push({ id: neighbor, depth: curr.depth + 1 });
                }
            });
        }
    }

    const nonCodeFiles = nodes.filter(n => n.type !== 'file').map(n => ({
        id: n.id,
        name: n.name,
        type: n.type,
        summary: n.summary
    }));

    // Tightly Coupled Clusters
    // Very naive approach: find bidirectionally connected nodes
    const clusters = [];
    const visitedClusterNodes = new Set();
    nodes.forEach(n => {
        if (visitedClusterNodes.has(n.id)) return;
        const bidir = [];
        adj[n.id].forEach(neighbor => {
            if (adj[neighbor] && adj[neighbor].includes(n.id)) {
                bidir.push(neighbor);
            }
        });
        if (bidir.length > 0) {
            const cluster = [n.id, ...bidir];
            clusters.push(cluster);
            cluster.forEach(c => visitedClusterNodes.add(c));
        }
    });

    const result = {
        topFanIn: topFanIn.map(n => n.id),
        topFanOut: topFanOut.map(n => n.id),
        entryCandidates: entryCandidates.map(c => c.node.id),
        bfsOrder,
        bfsDepthMap,
        bfsNodesByDepth,
        nonCodeFiles,
        clusters: clusters.slice(0, 10),
        layers: layers || [],
        nodeSummaryIndex
    };

    fs.writeFileSync(outputFile, JSON.stringify(result, null, 2));
}

const args = process.argv.slice(2);
analyze(args[0], args[1]);
