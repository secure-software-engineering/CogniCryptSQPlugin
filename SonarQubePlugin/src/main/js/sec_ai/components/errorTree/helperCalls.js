export const buildReactFlowElements = (flatList, highlighted = { nodes: [], edges: [], viewMode: 'all' }, onJumpToIssue) => {
    const nodes = [];
    const edges = [];

    const graph = {};
    const reverseGraph = {};
    const idMap = new Map();

    // Build graph maps
    flatList.forEach(item => {
        const id = item.hashcode;
        idMap.set(id, item);
        graph[id] = item.subsequentErrors || [];
        (item.subsequentErrors || []).forEach(child => {
            if (!reverseGraph[child]) reverseGraph[child] = [];
            reverseGraph[child].push(id);
        });
    });

    // BFS to assign levels (depth from roots)
    const levels = {};
    const queue = [];

    for (let id of idMap.keys()) {
        if (!reverseGraph[id]) {
            levels[id] = 0;
            queue.push(id);
        }
    }

    while (queue.length) {
        const current = queue.shift();
        const nextLevel = levels[current] + 1;
        (graph[current] || []).forEach(child => {
            if (!(child in levels) || nextLevel > levels[child]) {
                levels[child] = nextLevel;
                queue.push(child);
            }
        });
    }

    // Group nodes by level and errorType
    const groupedByLevel = {};
    for (const [id, item] of idMap.entries()) {
        const level = levels[id] ?? 0;
        const errorType = item.errorType;
        if (!groupedByLevel[level]) groupedByLevel[level] = {};
        if (!groupedByLevel[level][errorType]) groupedByLevel[level][errorType] = [];
        groupedByLevel[level][errorType].push(item);
    }

    // Layout settings
    const groupHeight = 160;
    const groupSpacingY = 250;
    const nodeSpacingX = 380;
    let currentY = 0;

    // Build node and group layout
    for (const level of Object.keys(groupedByLevel).map(Number).sort((a, b) => a - b)) {
        const typeBlock = groupedByLevel[level];

        const groupWidths = Object.entries(typeBlock).map(([, items]) => items.length * nodeSpacingX + 150);
        const totalLevelWidth = groupWidths.reduce((sum, w) => sum + w, 0) + (groupWidths.length - 1) * 40;

        let currentX = -totalLevelWidth / 2;
        let groupIndex = 0;

        for (const [errorType, items] of Object.entries(typeBlock)) {
            const groupId = `group-${errorType}-${level}`;
            const groupWidth = groupWidths[groupIndex];

            const color = highlighted.colorTheme === 'plain'
                ? '#e0e0e0' // neutral group color
                : errorType.includes('Constraint') ? '#fdd' :
                    errorType.includes('Required') ? '#ddf' :
                        errorType.includes('Alternative') ? '#ffd' :
                            errorType.includes('Incomplete') ? '#dfd' :
                                errorType.includes('ForbiddenMethodError') ? '#fcb' :
                                    errorType.includes('ImpreciseValueExtractionError') ? '#cfc' :
                                        errorType.includes('InstanceOfError') ? '#cff' :
                                            errorType.includes('TypestateError') ? '#ffc' :
                                                '#eee';


            // Group node
            nodes.push({
                id: groupId,
                type: 'group',
                position: { x: currentX, y: currentY },
                data: { label: errorType },
                style: {
                    width: groupWidth,
                    height: groupHeight,
                    background: `${color}`,
                    border: `2px solid ${color}`,
                    borderRadius: 8,
                },
            });

            let childX = 30;
            for (const item of items) {
                const nodeId = item.hashcode;
                const isNodeHighlighted = highlighted?.nodes?.includes(nodeId);

                const nodeColor = highlighted.colorTheme === 'plain'
                    ? '#e0e0e0' // neutral group color
                    : item.errorType.includes('Constraint') ? '#fdd' :
                        item.errorType.includes('Required') ? '#ddf' :
                            item.errorType.includes('Alternative') ? '#ffd' :
                                item.errorType.includes('Incomplete') ? '#dfd' :
                                    item.errorType.includes('ForbiddenMethodError') ? '#fcb' :
                                        item.errorType.includes('ImpreciseValueExtractionError') ? '#cfc' :
                                            item.errorType.includes('InstanceOfError') ? '#cff' :
                                                item.errorType.includes('TypestateError') ? '#ffc' :
                                                    '#eee';


                nodes.push({
                    id: nodeId,
                    type: 'tooltipNode',
                    position: { x: childX, y: 60 },
                    parentNode: groupId,
                    extent: 'parent',
                    draggable: false,
                    data: { label: `L:${item.line} => ${item.rule}`, full: item, onJumpToIssue },
                    style: {
                        padding: 10,
                        borderRadius: 5,
                        fontSize: '12px',
                        background: nodeColor, // 🔥 always original color
                        border: isNodeHighlighted ? '2px solid #00f' : '1px solid #bbb',
                        opacity: isNodeHighlighted || highlighted?.nodes?.length === 0 ? 1 : 0.3, // 🔥 only opacity changes
                        boxShadow: isNodeHighlighted ? '0 0 10px #00f4' : 'none',
                        width: 'fit-content',
                    },
                });


                childX += nodeSpacingX;
            }

            currentX += groupWidth + 40;
            groupIndex++;
        }

        currentY += groupHeight + groupSpacingY;
    }

    const validNodeIds = new Set(flatList.map(n => n.hashcode));

    // Edges
    flatList.forEach(item => {
        const from = item.hashcode;

        (item.subsequentErrors || []).forEach(to => {
            if (!validNodeIds.has(to)) return;

            const edgeId = `${from}->${to}`;
            const isEdgeHighlighted = highlighted?.edges?.includes(edgeId);

            edges.push({
                id: `${edgeId}-${isEdgeHighlighted ? 'active' : 'inactive'}`,
                source: from,
                target: to,
                type: 'customAnimated',
                animated: isEdgeHighlighted,
                data: { isHighlighted: isEdgeHighlighted },
                style: {
                    stroke: isEdgeHighlighted ? '#00f' : '#bbb',
                    strokeWidth: isEdgeHighlighted ? 3 : 1.5,
                    opacity: isEdgeHighlighted || highlighted?.edges?.length === 0 ? 1 : 0.3, // 🔥 only opacity reduces
                }
            });

        });
    });

    return { nodes, edges };
};


// Highlighting the subtree from a given node
// This function collects all nodes and edges in the subtree starting from a given node ID.
// It returns an object containing the node IDs and edge IDs that form the subtree.
// This is useful for visualizing the structure of a tree or graph from a specific point.
export function collectSubtree(startId, graphMap) {
    const visited = new Set();
    const resultNodes = new Set();
    const resultEdges = new Set();

    function dfs(nodeId) {
        if (visited.has(nodeId)) return;
        visited.add(nodeId);
        resultNodes.add(nodeId);
        const children = graphMap[nodeId] || [];
        for (const child of children) {
            resultEdges.add(`${nodeId}->${child}`);
            dfs(child);
        }
    }

    dfs(startId);
    return { nodeIds: Array.from(resultNodes), edgeIds: Array.from(resultEdges) };
}


// Highlighting the path to the root node
// This function collects the path from a given node to the root node in a directed graph
// It returns an object containing the node IDs and edge IDs that form the path.
// This is useful for visualizing the ancestry of a node in a tree structure.
export function collectPathToRoot(startId, graphMap) {
    const visited = new Set();
    const resultNodes = new Set();
    const resultEdges = new Set();

    const reverseGraph = {};
    for (const [parent, children] of Object.entries(graphMap)) {
        for (const child of children) {
            if (!reverseGraph[child]) reverseGraph[child] = [];
            reverseGraph[child].push(parent);
        }
    }

    function dfs(nodeId) {
        if (visited.has(nodeId)) return;
        visited.add(nodeId);
        resultNodes.add(nodeId);
        const parents = reverseGraph[nodeId] || [];
        for (const parent of parents) {
            resultEdges.add(`${parent}->${nodeId}`);
            dfs(parent);
        }
    }

    dfs(startId);
    return { nodeIds: resultNodes, edgeIds: resultEdges };
}

// Extract source code for all nodes in the path
export async function extractSourceCodeFromPath(fullPathNodes, projectKey) {
    const sourceCodeResults = [];
    
    for (const node of fullPathNodes) {
        try {
            // Get component path - handle both formats
            let componentPath = node.reportLocation?.filePath || '';
            if (componentPath && !componentPath.includes(':')) {
                componentPath = `${projectKey}:${componentPath}`;
            }
            
            if (!componentPath) {
                console.warn('No component path found for node:', node);
                continue;
            }
            // Fetch raw source code
            const response = await fetch(`/api/sources/raw?key=${encodeURIComponent(componentPath)}`);
            if (!response.ok) {
                console.warn(`Failed to fetch source for ${componentPath}:`, response.status);
                continue;
            }
            
            const sourceCode = await response.text();
            const lines = sourceCode.split('\n');
            
            // Get the specific line and context
            const lineNumber = node.line || node.reportLocation?.start?.[0] || 1;
            const targetLine = lines[lineNumber - 1] || '';
            
            sourceCodeResults.push({
                nodeId: node.hashcode,
                errorType: node.errorType,
                rule: node.rule,
                line: lineNumber,
                filePath: componentPath,
                targetLine: targetLine.trim(),
                fullSourceCode: sourceCode,
                contextLines: {
                    before: lines.slice(Math.max(0, lineNumber - 3), lineNumber - 1),
                    current: targetLine,
                    after: lines.slice(lineNumber, lineNumber + 3)
                }
            });
            
        } catch (error) {
            console.error(`Error fetching source code for node ${node.hashcode}:`, error);
        }
    }
    
    return sourceCodeResults;
}

