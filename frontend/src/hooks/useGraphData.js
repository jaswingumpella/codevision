import { useState, useCallback } from 'react';
import Graph from 'graphology';
import apiClient from '../lib/apiClient';
import { getNodeColor, getNodeSize } from '../constants/nodeTypes';

/**
 * Simple deterministic hash from a string to a number.
 * Used for reproducible initial node positions.
 */
function hashCode(str) {
  let hash = 0;
  for (let i = 0; i < str.length; i++) {
    hash = ((hash << 5) - hash + str.charCodeAt(i)) | 0;
  }
  return Math.abs(hash);
}

/**
 * Hook for loading and transforming KnowledgeGraph data into a Graphology graph.
 */
export function useGraphData() {
  const [graph, setGraph] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [stats, setStats] = useState({ nodes: 0, edges: 0 });

  const loadGraph = useCallback(async (projectId) => {
    setLoading(true);
    setError(null);
    try {
      const response = await apiClient.get(`/api/v1/graph/${projectId}/full`);
      const data = response.data;
      const g = buildGraphologyGraph(data);
      setGraph(g);
      setStats({ nodes: g.order, edges: g.size });
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  }, []);

  const loadFromData = useCallback((data) => {
    const g = buildGraphologyGraph(data);
    setGraph(g);
    setStats({ nodes: g.order, edges: g.size });
  }, []);

  return { graph, loading, error, stats, loadGraph, loadFromData };
}

function buildGraphologyGraph(data) {
  const g = new Graph();

  if (data.nodes) {
    const nodes = typeof data.nodes === 'object' && !Array.isArray(data.nodes)
      ? Object.values(data.nodes)
      : data.nodes;

    for (const node of nodes) {
      const id = node.id || node.key;
      if (!id || g.hasNode(id)) continue;
      const type = node.type || 'DEFAULT';
      const h = hashCode(id);
      g.addNode(id, {
        label: node.name || node.label || id,
        color: getNodeColor(type),
        size: getNodeSize(type),
        type: type,
        qualifiedName: node.qualifiedName || '',
        x: (h % 1000),
        y: ((h * 31) % 1000),
      });
    }
  }

  if (data.edges) {
    for (const edge of data.edges) {
      const src = edge.sourceNodeId || edge.source;
      const tgt = edge.targetNodeId || edge.target;
      if (src && tgt && g.hasNode(src) && g.hasNode(tgt) && !g.hasEdge(src, tgt)) {
        g.addEdge(src, tgt, {
          label: edge.type || edge.label || '',
          color: '#ccc',
          size: 1,
        });
      }
    }
  }

  return g;
}

export default useGraphData;
