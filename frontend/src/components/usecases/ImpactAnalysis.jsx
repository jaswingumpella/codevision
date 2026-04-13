import './usecases.css';
import { useState, useCallback } from 'react';
import apiClient from '../../lib/apiClient';

export default function ImpactAnalysis({ graphData, selectedNodeId, onNavigateToNode }) {
  const [result, setResult] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  const analyze = useCallback(async () => {
    if (!graphData || !selectedNodeId) return;
    setLoading(true);
    setError(null);
    try {
      const response = await apiClient.post(
        `/api/v1/usecases/impact/${encodeURIComponent(selectedNodeId)}`,
        graphData
      );
      setResult(response.data);
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  }, [graphData, selectedNodeId]);

  const impactedNodes = result?.impactedNodes ?? [];

  return (
    <div className="usecase-panel">
      <h3>Impact Analysis</h3>

      {!selectedNodeId ? (
        <p className="usecase-hint">Select a node in the graph to analyze its impact.</p>
      ) : (
        <>
          <p className="usecase-context">
            Analyzing impact of: <strong>{selectedNodeId}</strong>
          </p>
          <button onClick={analyze} disabled={loading || !graphData}>
            {loading ? 'Analyzing...' : 'Run Analysis'}
          </button>
        </>
      )}

      {error && <p className="usecase-error">Error: {error}</p>}

      {result && (
        <div className="usecase-results">
          <div className="usecase-summary">
            <span className="usecase-stat">{impactedNodes.length}</span> impacted nodes
          </div>

          <ul className="usecase-list">
            {impactedNodes.map((node) => (
              <li key={node.id ?? node.name} className="usecase-list-item">
                <button
                  className="usecase-link"
                  onClick={() => onNavigateToNode?.(node.id ?? node.name)}
                >
                  {node.name}
                </button>
                {node.type && <span className="usecase-badge">{node.type}</span>}
              </li>
            ))}
          </ul>
        </div>
      )}
    </div>
  );
}
