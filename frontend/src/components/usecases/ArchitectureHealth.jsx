import './usecases.css';
import { useState, useCallback } from 'react';
import apiClient from '../../lib/apiClient';

function scoreColor(score) {
  if (score < 40) return '#e74c3c';
  if (score < 70) return '#f39c12';
  return '#27ae60';
}

export default function ArchitectureHealth({ graphData, onNavigateToNode }) {
  const [result, setResult] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  const analyze = useCallback(async () => {
    if (!graphData) return;
    setLoading(true);
    setError(null);
    try {
      const response = await apiClient.post('/api/v1/usecases/health', graphData);
      setResult(response.data);
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  }, [graphData]);

  const overallScore = result?.overallScore ?? 0;
  const components = result?.componentScores ?? [];

  return (
    <div className="usecase-panel">
      <h3>Architecture Health</h3>
      <button onClick={analyze} disabled={loading || !graphData}>
        {loading ? 'Analyzing...' : 'Run Analysis'}
      </button>

      {error && <p className="usecase-error">Error: {error}</p>}

      {result && (
        <div className="usecase-results">
          <div className="usecase-summary">
            <span
              className="usecase-score"
              style={{
                color: scoreColor(overallScore),
                fontSize: '2rem',
                fontWeight: 'bold',
              }}
            >
              {overallScore}
            </span>
            <span> / 100</span>
          </div>

          {components.length > 0 && (
            <table className="usecase-table">
              <thead>
                <tr>
                  <th>Component</th>
                  <th>Score</th>
                </tr>
              </thead>
              <tbody>
                {components.map((comp) => (
                  <tr key={comp.name}>
                    <td>{comp.name}</td>
                    <td style={{ color: scoreColor(comp.score) }}>{comp.score}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </div>
      )}
    </div>
  );
}
