import React, { useEffect, useRef, useState } from 'react';
import ReactFlow, {
    Controls,
    Background,
    useNodesState,
    useEdgesState,
    ReactFlowProvider
} from 'reactflow';
import 'reactflow/dist/style.css';

import { useDispatch, useSelector } from 'react-redux';
import {
    selectViewMode,
    selectColorTheme,
    selectHighlightMode,
} from '../../store/errorTreeReducer';

import { findProjects, getActiveproject, fetchIssues } from '../APIs/api'; // Import fetchIssues
import {
    buildReactFlowElements,
    collectPathToRoot,
    collectSubtree,
    extractSourceCodeFromPath
} from './helperCalls';

import TooltipNode from './TooltipNode';
import ResizableGroupNode from './ResizableGroupNode';
import CustomAnimatedEdge from './CustomAnimatedEdge';
import HorizontalControls from './HorizontalControls';
import ErrorLegend from './ErrorLegend';

const nodeTypes = {
    tooltipNode: TooltipNode,
    group: ResizableGroupNode,
};

const edgeTypes = {
    customAnimated: CustomAnimatedEdge
};

function ErrorVis({ onJumpToIssue }) {
    const [nodes, setNodes, onNodesChange] = useNodesState([]);
    const [edges, setEdges, onEdgesChange] = useEdgesState([]);
    const graphMapRef = useRef({});
    const flatListRef = useRef([]);
    const allIssuesRef = useRef([]); // Ref to store all fetched issues
    const [fileList, setFileList] = useState([]);
    const [activeNodeId, setActiveNodeId] = useState(null);

    const [projectKey, setProjectKey] = useState(null);


    const viewMode = useSelector(selectViewMode);
    const colorTheme = useSelector(selectColorTheme);
    const highlightMode = useSelector(selectHighlightMode);

    // Load initial data (both error tree and all detailed issues)
    useEffect(() => {
        async function loadData() {
            try {
                const _projects = await findProjects();
                const _id = new URLSearchParams(window.location.search).get('id');
                const _activeProject = getActiveproject(_id, _projects);
                if (!_activeProject?.key) return;
                const _projectKey = _activeProject.key;
                setProjectKey(_projectKey);

                // Fetch both datasets in parallel
                const [treeRes, issuesRes] = await Promise.all([
                    fetch(`/api/measures/component?component=${_projectKey}&metricKeys=secai.cognicrypt.error.tree`),
                    fetchIssues(_projectKey)
                ]);

                const treeJson = await treeRes.json();
                const rawValue = treeJson?.component?.measures?.[0]?.value ?? null;
                const parsed = JSON.parse(rawValue || '{ "issues": []}');
                const flatList = parsed.issues;
                allIssuesRef.current = issuesRes || [];
                flatListRef.current = flatList;

                const graphMap = {};
                const fileSet = new Set();
                flatList.forEach(n => {
                    graphMap[n.hashcode] = n.subsequentErrors || [];
                    if (n.class) fileSet.add(n.class);
                });
                graphMapRef.current = graphMap;
                setFileList(Array.from(fileSet));

                // Pass all issues to the builder function
                const { nodes, edges } = buildReactFlowElements(flatList, { nodes: [], edges: [], colorTheme, allIssues: allIssuesRef.current }, onJumpToIssue);

                // console.log("Loaded error tree with nodes:", nodes, "and edges:", edges);
                setNodes(nodes);
                setEdges(edges);
            } catch (e) {
                console.error("Failed to parse error tree JSON:", e);
            }
        }
        loadData();
    }, []); // eslint-disable-line react-hooks/exhaustive-deps


    // Re-render graph based on view mode and filters
    useEffect(() => {
        if (!flatListRef.current.length) return;
        const allItems = flatListRef.current;
        const isAllSelected = !viewMode || viewMode.length === 0 || viewMode.includes('all');

        const itemsToRender = isAllSelected ? allItems : allItems.filter(item => {
            return item.class && viewMode.includes(item.class);
        });

        const { nodes, edges } = buildReactFlowElements(itemsToRender, { nodes: [], edges: [], viewMode, colorTheme, allIssues: allIssuesRef.current }, onJumpToIssue);
        setNodes(nodes);
        setEdges(edges);
    }, [viewMode, colorTheme]); // eslint-disable-line react-hooks/exhaustive-deps

    // Update nodes with the `_oldRule` when a node is clicked
    useEffect(() => {
        setNodes((nds) =>
            nds.map((n) => {
                if (n.id === activeNodeId) {
                    let errorType = n.data.full.errorType;
                     if (errorType === 'AlternativeReqPredicateError') {
                        errorType = 'RequiredPredicateError';
                    } else if (errorType === 'IncompleteOperationError' || errorType === 'TypestateError') {
                        errorType = 'OrderError';
                    }

                    const rule = n.data.full.rule;
                    const ruleSuffix = rule.substring(rule.lastIndexOf('.') + 1);
                    const sonarRuleKey = `cognicrypt:${errorType}_${ruleSuffix}`;
                    
                    return { ...n, data: { ...n.data, _oldRule: sonarRuleKey } };
                }
                return n;
            })
        );
    }, [activeNodeId, setNodes]);

    const getFilteredItems = () => {
        const allItems = flatListRef.current;
        const isAllSelected = !viewMode || viewMode.length === 0 || viewMode.includes('all');
        return isAllSelected ? allItems : allItems.filter(item => item.class && viewMode.includes(item.class));
    };

    const handleHighlight = (nodeId) => {
        const visibleItems = getFilteredItems();
        const ancestor = collectPathToRoot(nodeId, graphMapRef.current);
        const children = collectSubtree(nodeId, graphMapRef.current);
        const merged = { nodeIds: new Set(), edgeIds: new Set() };

        if (highlightMode === 'ancestor' || highlightMode === 'chain') {
            ancestor.nodeIds.forEach(id => merged.nodeIds.add(id));
            ancestor.edgeIds.forEach(id => merged.edgeIds.add(id));
        }
        if (highlightMode === 'children' || highlightMode === 'chain') {
            children.nodeIds.forEach(id => merged.nodeIds.add(id));
            children.edgeIds.forEach(id => merged.edgeIds.add(id));
        }

        const { nodes: newNodes, edges: newEdges } = buildReactFlowElements(visibleItems, {
            nodes: Array.from(merged.nodeIds),
            edges: Array.from(merged.edgeIds),
            viewMode,
            colorTheme,
            allIssues: allIssuesRef.current
        }, onJumpToIssue);

        // console.log("merged highlight nodes:", merged, "edges:", merged.edgeIds);
        setNodes(newNodes);
        setEdges(newEdges);
    };

    const clearHighlight = () => {
        const visibleItems = getFilteredItems(); 
        const { nodes, edges } = buildReactFlowElements(visibleItems, { nodes: [], edges: [], viewMode, colorTheme, allIssues: allIssuesRef.current }, onJumpToIssue);
        setNodes(nodes);
        setEdges(edges);
        setActiveNodeId(null);
    };
    
    const handleNodeClick = (_, node) => {
        setActiveNodeId(node.id);
        
        // Calculate subsequent and preceding errors
        const nodeData = node.data.full;
        const subsequentErrors = graphMapRef.current[node.id] || [];
        const precedingErrors = [];
        
        // Find preceding errors by checking which nodes have this node as subsequent
        Object.keys(graphMapRef.current).forEach(nodeId => {
            if (graphMapRef.current[nodeId].includes(node.id)) {
                precedingErrors.push(nodeId);
            }
        });
        
        // Get full error node info from root to bottom
        const pathToRoot = collectPathToRoot(node.id, graphMapRef.current);
        const subtree = collectSubtree(node.id, graphMapRef.current);
        
        // Collect full node information for the path
        const rootToBottomPath = [];
        const allNodes = flatListRef.current;
        
        // Add path from root to current node
        pathToRoot.nodeIds.forEach(nodeId => {
            const fullNode = allNodes.find(n => n.hashcode === nodeId);
            if (fullNode) rootToBottomPath.push(fullNode);
        });
        
        // Add subtree nodes (from current to bottom)
        subtree.nodeIds.forEach(nodeId => {
            if (!Array.from(pathToRoot.nodeIds).includes(nodeId)) {
                const fullNode = allNodes.find(n => n.hashcode === nodeId);
                if (fullNode) rootToBottomPath.push(fullNode);
            }
        });
        
        // Extract source code for all nodes in the path
        // const projectKey = new URLSearchParams(window.location.search).get('id');
        extractSourceCodeFromPath(rootToBottomPath, projectKey).then(sourceCodeResults => {
            // console.log('ErrorTree Vis - Selected Node:', {
            //     selectedNode: nodeData,
            //     // subsequentCount: subsequentErrors.length,
            //     // precedingCount: precedingErrors.length,
            //     // subsequentErrors: subsequentErrors.map(id => allNodes.find(n => n.hashcode === id)).filter(Boolean),
            //     // precedingErrors: precedingErrors.map(id => allNodes.find(n => n.hashcode === id)).filter(Boolean),
            //     fullPathFromRootToBottom: rootToBottomPath,
            //     sourceCodeAnalysis: sourceCodeResults
            // });
        }).catch(error => {
            console.error('ErrorTree Vis - Failed to extract source code:', error);
        });
        
        setTimeout(() => handleHighlight(node.id), 0);
    }

    return (
        <ReactFlowProvider>
            <div style={{ height: '100%', width: '100%' }}>
                <div style={{ height: '100%', width: '100%', position: 'relative' }}>
                    <ReactFlow
                        nodes={nodes}
                        edges={edges}
                        nodeTypes={nodeTypes}
                        edgeTypes={edgeTypes}
                        onNodesChange={onNodesChange}
                        onEdgesChange={onEdgesChange}
                        fitView
                        nodesDraggable={false}
                        nodesConnectable={false}
                        zoomOnScroll
                        panOnDrag
                        snapToGrid
                        snapGrid={[15, 15]}
                        onPaneClick={clearHighlight}
                        onNodeClick={handleNodeClick}
                    >
                        <Background />
                        <HorizontalControls fileList={fileList} />
                        <ErrorLegend />
                    </ReactFlow>
                </div>
            </div>
        </ReactFlowProvider>
    );
}

export default ErrorVis;