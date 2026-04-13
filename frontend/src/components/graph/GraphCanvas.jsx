import { useRef, useEffect } from 'react';
import Sigma from 'sigma';
import forceAtlas2 from 'graphology-layout-forceatlas2';

/**
 * WebGL graph canvas powered by Sigma.js v3.
 * Renders a Graphology graph with ForceAtlas2 layout.
 *
 * Sigma instance lifecycle is separated from filter/event updates:
 * - Sigma is created/destroyed only when the graph changes
 * - Reducers and events use refs to avoid re-instantiation on filter changes
 */
export default function GraphCanvas({ graph, onNodeClick, onNodeHover, onNodeRightClick, isNodeVisible, focusNodeId, highlightedNodes }) {
  const containerRef = useRef(null);
  const sigmaRef = useRef(null);
  const isNodeVisibleRef = useRef(isNodeVisible);
  const onNodeClickRef = useRef(onNodeClick);
  const onNodeHoverRef = useRef(onNodeHover);
  const onNodeRightClickRef = useRef(onNodeRightClick);
  const highlightedNodesRef = useRef(highlightedNodes);

  // Keep refs up to date without triggering effects
  useEffect(() => { isNodeVisibleRef.current = isNodeVisible; }, [isNodeVisible]);
  useEffect(() => { onNodeClickRef.current = onNodeClick; }, [onNodeClick]);
  useEffect(() => { onNodeHoverRef.current = onNodeHover; }, [onNodeHover]);
  useEffect(() => { onNodeRightClickRef.current = onNodeRightClick; }, [onNodeRightClick]);
  useEffect(() => { highlightedNodesRef.current = highlightedNodes; }, [highlightedNodes]);

  // Create/destroy Sigma only when graph changes
  useEffect(() => {
    if (!graph || !containerRef.current) return;

    // Apply ForceAtlas2 layout
    if (graph.order > 0) {
      forceAtlas2.assign(graph, {
        iterations: 50,
        settings: {
          gravity: 1,
          scalingRatio: 2,
          barnesHutOptimize: graph.order > 500,
        },
      });
    }

    // Create Sigma instance with ref-based reducers
    const sigma = new Sigma(graph, containerRef.current, {
      renderEdgeLabels: false,
      defaultEdgeColor: '#e0e0e0',
      defaultEdgeType: 'arrow',
      labelSize: 12,
      labelRenderedSizeThreshold: 6,
      nodeReducer: (node, data) => {
        const res = { ...data };
        const fn = isNodeVisibleRef.current;
        if (fn && !fn(node, data)) {
          res.hidden = true;
        }
        const hl = highlightedNodesRef.current;
        if (hl && hl.size > 0 && !hl.has(node)) {
          res.color = '#e0e0e0';
          res.zIndex = 0;
        } else if (hl && hl.size > 0 && hl.has(node)) {
          res.zIndex = 1;
        }
        return res;
      },
      edgeReducer: (edge, data) => {
        const res = { ...data };
        const fn = isNodeVisibleRef.current;
        if (fn) {
          const src = graph.source(edge);
          const tgt = graph.target(edge);
          const srcAttrs = graph.getNodeAttributes(src);
          const tgtAttrs = graph.getNodeAttributes(tgt);
          if (!fn(src, srcAttrs) || !fn(tgt, tgtAttrs)) {
            res.hidden = true;
          }
        }
        return res;
      },
    });

    // Event handlers use refs for stable identity
    sigma.on('clickNode', ({ node }) => {
      const fn = onNodeClickRef.current;
      if (fn) fn(node, graph.getNodeAttributes(node));
    });

    sigma.on('rightClickNode', ({ node, event }) => {
      const fn = onNodeRightClickRef.current;
      if (fn) {
        event.original.preventDefault();
        fn(node, graph.getNodeAttributes(node), event.original);
      }
    });

    sigma.on('enterNode', ({ node }) => {
      const fn = onNodeHoverRef.current;
      if (fn) fn(node, graph.getNodeAttributes(node));
    });

    sigma.on('leaveNode', () => {
      const fn = onNodeHoverRef.current;
      if (fn) fn(null, null);
    });

    sigmaRef.current = sigma;

    return () => {
      sigma.kill();
      sigmaRef.current = null;
    };
  }, [graph]);

  // Refresh renderer when filter/highlight changes (no Sigma rebuild)
  useEffect(() => {
    if (sigmaRef.current) {
      sigmaRef.current.refresh();
    }
  }, [isNodeVisible, highlightedNodes]);

  // Focus on a specific node when focusNodeId changes
  useEffect(() => {
    if (sigmaRef.current && focusNodeId && graph && graph.hasNode(focusNodeId)) {
      const nodeAttrs = graph.getNodeAttributes(focusNodeId);
      const camera = sigmaRef.current.getCamera();
      camera.animate(
        { x: nodeAttrs.x, y: nodeAttrs.y, ratio: 0.3 },
        { duration: 500 }
      );
    }
  }, [focusNodeId, graph]);

  return (
    <div
      ref={containerRef}
      style={{
        width: '100%',
        height: '100%',
        minHeight: '500px',
        background: '#fafafa',
        borderRadius: '8px',
      }}
    />
  );
}
