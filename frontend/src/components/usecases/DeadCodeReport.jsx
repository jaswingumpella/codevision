import './usecases.css';
import { useState, useCallback } from 'react';
import apiClient from '../../lib/apiClient';

export default function DeadCodeReport({ graphData, onNavigateToNode }) {
  const [result, setResult] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  const analyze = useCallback(async () => {
    if (!graphData) return;
    setLoading(true);
    setError(null);
    try {
      const response = await apiClient.post('/api/v1/usecases/dead-code', graphData);
      setResult(response.data);
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  }, [graphData]);

  const deadNodes = result?.deadNodes ?? [];
  const totalNodes = result?.totalNodes ?? 0;
  const percentage = totalNodes > 0
    ? ((deadNodes.length / totalNodes) * 100).toFixed(1)
    : 0;

  return (
    <div className="usecase-panel">
      <h3>Dead Code Report</h3>
      <button onClick={analyze} disabled={loading || !graphData}>
        {loading ? 'Analyzing...' : 'Run Analysis'}
      </button>

      {error && <p className="usecase-error">Error: {error}</p>}

      {result && (
        <div className="usecase-results">
          <div className="usecase-summary">
            <span className="usecase-stat">{deadNodes.length}</span> dead nodes
            out of <span className="usecase-stat">{totalNodes}</span> total
            ({percentage}%)
          </div>

          <ul className="usecase-list">
            {deadNodes.map((node) => (
              <li key={node.id ?? node.name} className="usecase-list-item">
                <button
                  className="usecase-link"
                  onClick={() => onNavigateToNode?.(node.id ?? node.name)}
                >
                  {node.name}
                </button>
                <span className="usecase-badge">{node.type}</span>
              </li>
            ))}
          </ul>
        </div>
      )}
    </div>
  );
}
