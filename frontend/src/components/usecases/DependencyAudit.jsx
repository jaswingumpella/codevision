import { useState, useCallback, useMemo } from 'react';
import apiClient from '../../lib/apiClient';

const styles = {
  container: { padding: '1rem' },
  header: { display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1rem' },
  btn: { padding: '0.5rem 1rem', borderRadius: '4px', border: 'none', cursor: 'pointer', background: '#4f46e5', color: '#fff', fontWeight: 600 },
  btnDisabled: { opacity: 0.5, cursor: 'not-allowed' },
  error: { color: '#ef4444', margin: '0.5rem 0' },
  spinner: { color: '#6b7280', fontStyle: 'italic' },
  riskGroup: { marginBottom: '1.25rem' },
  riskTitle: { fontWeight: 600, fontSize: '1rem', marginBottom: '0.4rem' },
  table: { width: '100%', borderCollapse: 'collapse', fontSize: '0.9rem' },
  th: { textAlign: 'left', padding: '0.5rem', borderBottom: '2px solid #e5e7eb', color: '#374151' },
  td: { padding: '0.5rem', borderBottom: '1px solid #f3f4f6' },
  badgeHigh: { display: 'inline-block', padding: '0.15rem 0.5rem', borderRadius: '4px', background: '#fee2e2', color: '#b91c1c', fontWeight: 600, fontSize: '0.8rem' },
  badgeMedium: { display: 'inline-block', padding: '0.15rem 0.5rem', borderRadius: '4px', background: '#fef3c7', color: '#92400e', fontWeight: 600, fontSize: '0.8rem' },
  badgeLow: { display: 'inline-block', padding: '0.15rem 0.5rem', borderRadius: '4px', background: '#d1fae5', color: '#065f46', fontWeight: 600, fontSize: '0.8rem' },
};

const RISK_ORDER = ['HIGH', 'MEDIUM', 'LOW'];
const badgeStyle = (level) => ({ HIGH: styles.badgeHigh, MEDIUM: styles.badgeMedium, LOW: styles.badgeLow }[level] || styles.badgeLow);

export default function DependencyAudit({ graphData, onNavigateToNode }) {
  const [result, setResult] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  const analyze = useCallback(async () => {
    if (!graphData) return;
    setLoading(true);
    setError(null);
    try {
      const response = await apiClient.post('/api/v1/usecases/dependency-audit', graphData);
      setResult(response.data);
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  }, [graphData]);

  const deps = result?.dependencies || result?.items || [];
  const grouped = useMemo(() => {
    const map = {};
    deps.forEach((d) => {
      const level = (d.riskLevel || d.risk || 'LOW').toUpperCase();
      (map[level] = map[level] || []).push(d);
    });
    return RISK_ORDER.map((r) => ({ level: r, items: map[r] || [] })).filter((g) => g.items.length > 0);
  }, [deps]);

  return (
    <div style={styles.container}>
      <div style={styles.header}>
        <h2 style={{ margin: 0 }}>Dependency Audit</h2>
        <button
          type="button"
          style={{ ...styles.btn, ...(loading || !graphData ? styles.btnDisabled : {}) }}
          disabled={loading || !graphData}
          onClick={analyze}
        >
          {loading ? 'Analyzing...' : 'Run Audit'}
        </button>
      </div>

      {error && <p style={styles.error}>{error}</p>}
      {loading && <p style={styles.spinner}>Auditing dependencies...</p>}

      {!loading && result && grouped.length === 0 && (
        <p style={{ color: '#6b7280' }}>No dependency risks detected.</p>
      )}

      {grouped.map(({ level, items }) => (
        <div key={level} style={styles.riskGroup}>
          <div style={styles.riskTitle}>
            <span style={badgeStyle(level)}>{level}</span> ({items.length})
          </div>
          <table style={styles.table}>
            <thead>
              <tr>
                <th style={styles.th}>Artifact</th>
                <th style={styles.th}>Version</th>
                <th style={styles.th}>Dependents</th>
              </tr>
            </thead>
            <tbody>
              {items.map((dep, i) => (
                <tr
                  key={dep.id || dep.artifactId || i}
                  style={{ cursor: dep.id ? 'pointer' : 'default' }}
                  onClick={() => dep.id && onNavigateToNode?.(dep.id)}
                >
                  <td style={styles.td}>{dep.artifactId || dep.name || dep.label || '—'}</td>
                  <td style={styles.td}>{dep.version || '—'}</td>
                  <td style={styles.td}>{dep.dependentCount ?? dep.dependents ?? '—'}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      ))}
    </div>
  );
}
