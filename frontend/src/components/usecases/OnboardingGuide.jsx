import { useState, useCallback } from 'react';
import apiClient from '../../lib/apiClient';

const styles = {
  container: { padding: '1rem' },
  header: { display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1rem' },
  btn: { padding: '0.5rem 1rem', borderRadius: '4px', border: 'none', cursor: 'pointer', background: '#4f46e5', color: '#fff', fontWeight: 600 },
  btnDisabled: { opacity: 0.5, cursor: 'not-allowed' },
  error: { color: '#ef4444', margin: '0.5rem 0' },
  spinner: { color: '#6b7280', fontStyle: 'italic' },
  stepList: { listStyle: 'none', padding: 0, margin: 0 },
  step: { display: 'flex', alignItems: 'flex-start', gap: '0.75rem', padding: '0.75rem', borderBottom: '1px solid #e5e7eb', cursor: 'pointer' },
  stepHover: { background: '#f9fafb' },
  badge: { display: 'inline-flex', alignItems: 'center', justifyContent: 'center', width: '1.75rem', height: '1.75rem', borderRadius: '50%', background: '#4f46e5', color: '#fff', fontWeight: 700, fontSize: '0.85rem', flexShrink: 0 },
  stepBody: { display: 'flex', flexDirection: 'column' },
  stepName: { fontWeight: 600 },
  stepType: { fontSize: '0.8rem', color: '#6b7280' },
  section: { marginTop: '1.25rem' },
  sectionTitle: { fontWeight: 600, fontSize: '1rem', marginBottom: '0.5rem', color: '#374151' },
};

const TYPE_ORDER = ['ENDPOINT', 'CLASS', 'DB_ENTITY'];
const typeLabel = (t) => ({ ENDPOINT: 'Endpoints', CLASS: 'Classes', DB_ENTITY: 'DB Entities' }[t] || t);

export default function OnboardingGuide({ graphData, onNavigateToNode }) {
  const [result, setResult] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  const analyze = useCallback(async () => {
    if (!graphData) return;
    setLoading(true);
    setError(null);
    try {
      const response = await apiClient.post('/api/v1/usecases/onboarding', graphData);
      setResult(response.data);
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  }, [graphData]);

  const nodes = result?.tourSteps || result?.nodes || [];
  const grouped = TYPE_ORDER.map((type) => ({
    type,
    items: nodes.filter((n) => (n.type || '').toUpperCase() === type),
  })).filter((g) => g.items.length > 0);

  let stepNum = 0;

  return (
    <div style={styles.container}>
      <div style={styles.header}>
        <h2 style={{ margin: 0 }}>Onboarding Guide</h2>
        <button
          type="button"
          style={{ ...styles.btn, ...(loading || !graphData ? styles.btnDisabled : {}) }}
          disabled={loading || !graphData}
          onClick={analyze}
        >
          {loading ? 'Analyzing...' : 'Start Tour'}
        </button>
      </div>

      {error && <p style={styles.error}>{error}</p>}
      {loading && <p style={styles.spinner}>Building onboarding tour...</p>}

      {!loading && result && grouped.length === 0 && (
        <p style={{ color: '#6b7280' }}>No tour steps found in this codebase.</p>
      )}

      {grouped.map((group) => (
        <div key={group.type} style={styles.section}>
          <div style={styles.sectionTitle}>{typeLabel(group.type)}</div>
          <ul style={styles.stepList}>
            {group.items.map((node) => {
              stepNum += 1;
              return (
                <li
                  key={node.id || stepNum}
                  style={styles.step}
                  onClick={() => onNavigateToNode?.(node.id)}
                  role="button"
                  tabIndex={0}
                  onKeyDown={(e) => e.key === 'Enter' && onNavigateToNode?.(node.id)}
                >
                  <span style={styles.badge}>{stepNum}</span>
                  <div style={styles.stepBody}>
                    <span style={styles.stepName}>{node.label || node.name || node.id}</span>
                    <span style={styles.stepType}>{node.type}</span>
                  </div>
                </li>
              );
            })}
          </ul>
        </div>
      ))}
    </div>
  );
}
