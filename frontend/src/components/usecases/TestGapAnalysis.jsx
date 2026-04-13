import './usecases.css';
import { useState, useCallback } from 'react';
import apiClient from '../../lib/apiClient';

function riskColor(risk) {
  if (risk === 'HIGH') return '#e74c3c';
  if (risk === 'MEDIUM') return '#f39c12';
  return '#27ae60';
}

export default function TestGapAnalysis({ graphData, onNavigateToNode }) {
  const [result, setResult] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  const analyze = useCallback(async () => {
    if (!graphData) return;
    setLoading(true);
    setError(null);
    try {
      const response = await apiClient.post('/api/v1/usecases/test-gaps', graphData);
      setResult(response.data);
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  }, [graphData]);

  const gaps = result?.gaps ?? [];

  return (
    <div className="usecase-panel">
      <h3>Test Gap Analysis</h3>
      <button onClick={analyze} disabled={loading || !graphData}>
        {loading ? 'Analyzing...' : 'Run Analysis'}
      </button>

      {error && <p className="usecase-error">Error: {error}</p>}

      {result && (
        <div className="usecase-results">
          <div className="usecase-summary">
            <span className="usecase-stat">{gaps.length}</span> untested classes found
          </div>

          <ul className="usecase-list">
            {gaps.map((gap) => (
              <li key={gap.id ?? gap.className} className="usecase-list-item">
                <button
                  className="usecase-link"
                  onClick={() => onNavigateToNode?.(gap.id ?? gap.className)}
                >
                  {gap.className}
                </button>
                <span
                  className="usecase-risk-badge"
                  style={{ color: riskColor(gap.risk) }}
                >
                  {gap.risk ?? 'UNKNOWN'}
                </span>
                {gap.reason && (
                  <span className="usecase-reason">{gap.reason}</span>
                )}
              </li>
            ))}
          </ul>
        </div>
      )}
    </div>
  );
}
