import './usecases.css';
import { useState, useCallback } from 'react';
import apiClient from '../../lib/apiClient';

export default function ApiSurfaceMap({ graphData, onNavigateToNode }) {
  const [result, setResult] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [expandedIndex, setExpandedIndex] = useState(null);

  const analyze = useCallback(async () => {
    if (!graphData) return;
    setLoading(true);
    setError(null);
    try {
      const response = await apiClient.post('/api/v1/usecases/api-surface', graphData);
      setResult(response.data);
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  }, [graphData]);

  const toggleExpanded = (index) => {
    setExpandedIndex(expandedIndex === index ? null : index);
  };

  const chains = result?.chains ?? [];

  return (
    <div className="usecase-panel">
      <h3>API Surface Map</h3>
      <button onClick={analyze} disabled={loading || !graphData}>
        {loading ? 'Analyzing...' : 'Run Analysis'}
      </button>

      {error && <p className="usecase-error">Error: {error}</p>}

      {result && (
        <div className="usecase-results">
          <div className="usecase-summary">
            <span className="usecase-stat">{chains.length}</span> endpoint chains
          </div>

          <ul className="usecase-list">
            {chains.map((chain, index) => (
              <li key={index} className="usecase-list-item">
                <button
                  className="usecase-link"
                  onClick={() => toggleExpanded(index)}
                >
                  {expandedIndex === index ? '[-]' : '[+]'}{' '}
                  {chain.endpoint ?? `Chain ${index + 1}`}
                </button>

                {expandedIndex === index && (
                  <div className="usecase-chain">
                    {['endpoint', 'service', 'repository', 'table'].map((layer) => {
                      const node = chain[layer];
                      if (!node) return null;
                      return (
                        <div key={layer} className="usecase-chain-step">
                          <span className="usecase-chain-label">{layer}:</span>{' '}
                          <button
                            className="usecase-link"
                            onClick={() => onNavigateToNode?.(node.id ?? node.name ?? node)}
                          >
                            {node.name ?? node}
                          </button>
                        </div>
                      );
                    })}
                  </div>
                )}
              </li>
            ))}
          </ul>
        </div>
      )}
    </div>
  );
}
