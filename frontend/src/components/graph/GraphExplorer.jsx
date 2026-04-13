import { useState, useCallback, useEffect, useMemo } from 'react';
import GraphCanvas from './GraphCanvas';
import GraphToolbar from './GraphToolbar';
import GraphDetailPanel from './GraphDetailPanel';
import GraphFilterPanel from './GraphFilterPanel';
import GraphLegend from './GraphLegend';
import GraphContextMenu from './GraphContextMenu';
import GuidedTourOverlay from './GuidedTourOverlay';
import GraphMinimap from './GraphMinimap';
import GraphTimeline from './GraphTimeline';
import GraphDiffOverlay from './GraphDiffOverlay';
import { useGraphData } from '../../hooks/useGraphData';
import { useGraphFilters } from '../../hooks/useGraphFilters';

/**
 * Main graph explorer component that combines canvas, toolbar, filters, and detail panel.
 * Includes context menu on right-click and guided tour overlay support.
 */
export default function GraphExplorer({ projectId, graphData }) {
  const { graph, loading, error, stats, loadGraph, loadFromData } = useGraphData();
  const { activeTypes, searchQuery, nodeTypes, toggleType, setSearchQuery, isNodeVisible } = useGraphFilters(graph);
  const [selectedNode, setSelectedNode] = useState(null);
  const [selectedAttrs, setSelectedAttrs] = useState(null);
  const [contextMenu, setContextMenu] = useState(null);
  const [tourSteps, setTourSteps] = useState([]);
  const [tourStep, setTourStep] = useState(0);
  const [tourActive, setTourActive] = useState(false);
  const [highlightedNodes, setHighlightedNodes] = useState(new Set());
  const [focusNodeId, setFocusNodeId] = useState(null);
  const [minimapVisible, setMinimapVisible] = useState(true);
  const [timelineSnapshots, setTimelineSnapshots] = useState([]);
  const [timelineIndex, setTimelineIndex] = useState(0);
  const [diffData, setDiffData] = useState(null);
  const [diffVisible, setDiffVisible] = useState(false);

  // Load graph data on mount or when graphData prop changes
  useEffect(() => {
    if (graphData) {
      loadFromData(graphData);
    } else if (projectId) {
      loadGraph(projectId);
    }
  }, [projectId, graphData, loadGraph, loadFromData]);

  const handleNodeClick = useCallback((nodeId, attrs) => {
    setSelectedNode(nodeId);
    setSelectedAttrs(attrs);
    setContextMenu(null);
  }, []);

  const handleCloseDetail = useCallback(() => {
    setSelectedNode(null);
    setSelectedAttrs(null);
  }, []);

  // Context menu handlers
  const handleContextMenu = useCallback((e) => {
    e.preventDefault();
    setContextMenu(null);
  }, []);

  const handleNodeRightClick = useCallback((nodeId, attrs, event) => {
    setContextMenu({ x: event.clientX || event.x, y: event.clientY || event.y, nodeId, nodeAttrs: attrs });
  }, []);

  const handleCloseContextMenu = useCallback(() => {
    setContextMenu(null);
  }, []);

  const handleContextAction = useCallback((actionType, nodeId, nodeAttrs) => {
    switch (actionType) {
      case 'show-detail':
        setSelectedNode(nodeId);
        setSelectedAttrs(nodeAttrs);
        break;
      case 'copy-name':
        if (nodeAttrs?.qualifiedName) {
          navigator.clipboard.writeText(nodeAttrs.qualifiedName);
        }
        break;
      case 'highlight-neighbors':
        if (graph) {
          const neighbors = new Set();
          neighbors.add(nodeId);
          graph.forEachNeighbor(nodeId, (neighbor) => neighbors.add(neighbor));
          setHighlightedNodes(neighbors);
        }
        break;
      case 'focus-subgraph':
        setFocusNodeId(nodeId);
        setSelectedNode(nodeId);
        setSelectedAttrs(nodeAttrs);
        break;
      case 'show-impact':
        setSelectedNode(nodeId);
        setSelectedAttrs(nodeAttrs);
        break;
      default:
        break;
    }
    setContextMenu(null);
  }, [graph]);

  // Tour handlers
  const handleStartTour = useCallback((steps) => {
    setTourSteps(steps);
    setTourStep(0);
    setTourActive(true);
    if (steps.length > 0 && steps[0].nodeId) {
      setFocusNodeId(steps[0].nodeId);
    }
  }, []);

  const handleTourNext = useCallback(() => {
    setTourStep((s) => {
      const next = Math.min(s + 1, tourSteps.length - 1);
      if (tourSteps[next]?.nodeId) {
        setFocusNodeId(tourSteps[next].nodeId);
      }
      return next;
    });
  }, [tourSteps]);

  const handleTourPrev = useCallback(() => {
    setTourStep((s) => {
      const prev = Math.max(s - 1, 0);
      if (tourSteps[prev]?.nodeId) {
        setFocusNodeId(tourSteps[prev].nodeId);
      }
      return prev;
    });
  }, [tourSteps]);

  const handleTourClose = useCallback(() => {
    setTourActive(false);
    setTourSteps([]);
    setTourStep(0);
    setFocusNodeId(null);
  }, []);

  // Count visible nodes for empty-filter-result detection
  const visibleNodeCount = useMemo(() => {
    if (!graph) return 0;
    let count = 0;
    graph.forEachNode((node, attrs) => {
      if (!isNodeVisible || isNodeVisible(node, attrs)) {
        count++;
      }
    });
    return count;
  }, [graph, isNodeVisible]);

  if (loading) {
    return <div style={{ padding: '24px', textAlign: 'center' }}>Loading graph...</div>;
  }

  if (error) {
    return <div style={{ padding: '24px', color: '#e74c3c' }}>Error: {error}</div>;
  }

  // Empty state: no graph loaded
  if (!graph) {
    return (
      <div style={{ padding: '48px 24px', textAlign: 'center', color: '#888' }}>
        <p style={{ fontSize: '16px' }}>Select or analyze a project to view its graph</p>
      </div>
    );
  }

  return (
    <div
      style={{ position: 'relative', width: '100%', height: '100%' }}
      onContextMenu={handleContextMenu}
    >
      <GraphToolbar
        searchQuery={searchQuery}
        onSearchChange={setSearchQuery}
        stats={stats}
      />
      <div style={{ position: 'relative', width: '100%', height: 'calc(100% - 48px)' }}>
        {visibleNodeCount === 0 ? (
          <div style={{
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            width: '100%',
            height: '100%',
            minHeight: '500px',
            color: '#888',
            fontSize: '15px',
            background: '#fafafa',
            borderRadius: '8px',
          }}>
            No nodes match current filters
          </div>
        ) : (
          <GraphCanvas
            graph={graph}
            onNodeClick={handleNodeClick}
            onNodeRightClick={handleNodeRightClick}
            isNodeVisible={isNodeVisible}
            focusNodeId={focusNodeId}
            highlightedNodes={highlightedNodes}
          />
        )}
        <GraphFilterPanel
          nodeTypes={nodeTypes}
          activeTypes={activeTypes}
          onToggleType={toggleType}
        />
        <GraphDetailPanel
          nodeId={selectedNode}
          nodeAttrs={selectedAttrs}
          onClose={handleCloseDetail}
        />
        <GraphLegend />
        <GraphMinimap graph={graph} sigma={null} visible={minimapVisible} />
        <GraphTimeline
          snapshots={timelineSnapshots}
          currentIndex={timelineIndex}
          onSelect={setTimelineIndex}
          visible={timelineSnapshots.length > 0}
        />
        <GraphDiffOverlay diffData={diffData} visible={diffVisible} />

        {contextMenu && (
          <GraphContextMenu
            x={contextMenu.x}
            y={contextMenu.y}
            nodeId={contextMenu.nodeId}
            nodeAttrs={contextMenu.nodeAttrs}
            onClose={handleCloseContextMenu}
            onAction={handleContextAction}
          />
        )}

        {tourActive && tourSteps.length > 0 && (
          <GuidedTourOverlay
            steps={tourSteps}
            currentStep={tourStep}
            onNext={handleTourNext}
            onPrev={handleTourPrev}
            onClose={handleTourClose}
          />
        )}
      </div>
    </div>
  );
}
