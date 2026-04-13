import './usecases.css';
import { useState, useCallback } from 'react';
import apiClient from '../../lib/apiClient';

export default function CycleBreaker({ graphData, onNavigateToNode }) {
  const [result, setResult] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  const analyze = useCallback(async () => {
    if (!graphData) return;
    setLoading(true);
    setError(null);
    try {
      const response = await apiClient.post('/api/v1/usecases/cycles', graphData);
      setResult(response.data);
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  }, [graphData]);

  const cycles = result?.cycles ?? [];

  return (
    <div className="usecase-panel">
      <h3>
        Cycle Breaker{' '}
        {result && <span className="usecase-count-badge">{cycles.length}</span>}
      </h3>
      <button onClick={analyze} disabled={loading || !graphData}>
        {loading ? 'Analyzing...' : 'Run Analysis'}
      </button>

      {error && <p className="usecase-error">Error: {error}</p>}

      {result && cycles.length === 0 && (
        <p className="usecase-hint">No dependency cycles detected.</p>
      )}

      {result && cycles.length > 0 && (
        <div className="usecase-results">
          <ul className="usecase-list">
            {cycles.map((cycle, index) => (
              <li key={index} className="usecase-list-item">
                <div className="usecase-cycle-header">
                  Cycle {index + 1} ({(cycle.nodes ?? []).length} nodes)
                </div>

                <div className="usecase-cycle-nodes">
                  {(cycle.nodes ?? []).map((node, nIdx) => (
                    <span key={nIdx}>
                      <button
                        className="usecase-link"
                        onClick={() => onNavigateToNode?.(node.id ?? node.name ?? node)}
                      >
                        {node.name ?? node}
                      </button>
                      {nIdx < (cycle.nodes ?? []).length - 1 && ' -> '}
                    </span>
                  ))}
                </div>

                {cycle.suggestion && (
                  <p className="usecase-suggestion">{cycle.suggestion}</p>
                )}
              </li>
            ))}
          </ul>
        </div>
      )}
    </div>
  );
}
