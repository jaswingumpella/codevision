import { useState, useCallback } from 'react';
import apiClient from '../../lib/apiClient';

const styles = {
  container: { padding: '1rem' },
  header: { display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1rem' },
  btn: { padding: '0.5rem 1rem', borderRadius: '4px', border: 'none', cursor: 'pointer', background: '#4f46e5', color: '#fff', fontWeight: 600 },
  btnDisabled: { opacity: 0.5, cursor: 'not-allowed' },
  error: { color: '#ef4444', margin: '0.5rem 0' },
  spinner: { color: '#6b7280', fontStyle: 'italic' },
  group: { marginBottom: '1.25rem', border: '1px solid #e5e7eb', borderRadius: '6px', overflow: 'hidden' },
  groupHeader: { padding: '0.6rem 0.75rem', background: '#f9fafb', fontWeight: 600, fontSize: '0.9rem', borderBottom: '1px solid #e5e7eb' },
  item: { padding: '0.5rem 0.75rem', borderBottom: '1px solid #f3f4f6', cursor: 'pointer', fontSize: '0.9rem' },
  itemLast: { padding: '0.5rem 0.75rem', cursor: 'pointer', fontSize: '0.9rem' },
  similarity: { marginLeft: '0.5rem', fontSize: '0.8rem', color: '#6b7280' },
  summary: { color: '#6b7280', marginBottom: '1rem', fontSize: '0.9rem' },
};

export default function DuplicationFinder({ graphData, onNavigateToNode }) {
  const [result, setResult] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  const analyze = useCallback(async () => {
    if (!graphData) return;
    setLoading(true);
    setError(null);
    try {
      const response = await apiClient.post('/api/v1/usecases/duplications', graphData);
      setResult(response.data);
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  }, [graphData]);

  const groups = result?.groups || result?.duplicateGroups || [];

  return (
    <div style={styles.container}>
      <div style={styles.header}>
        <h2 style={{ margin: 0 }}>Duplication Finder</h2>
        <button
          type="button"
          style={{ ...styles.btn, ...(loading || !graphData ? styles.btnDisabled : {}) }}
          disabled={loading || !graphData}
          onClick={analyze}
        >
          {loading ? 'Analyzing...' : 'Find Duplicates'}
        </button>
      </div>

      {error && <p style={styles.error}>{error}</p>}
      {loading && <p style={styles.spinner}>Scanning for structural duplicates...</p>}

      {!loading && result && groups.length === 0 && (
        <p style={{ color: '#6b7280' }}>No structural duplications detected.</p>
      )}

      {groups.length > 0 && (
        <p style={styles.summary}>{groups.length} duplicate group{groups.length !== 1 ? 's' : ''} found</p>
      )}

      {groups.map((group, gi) => {
        const members = group.members || group.classes || group.items || [];
        return (
          <div key={gi} style={styles.group}>
            <div style={styles.groupHeader}>
              Group {gi + 1}
              {group.similarity != null && (
                <span style={styles.similarity}>{Math.round(group.similarity * 100)}% similar</span>
              )}
            </div>
            {members.map((cls, ci) => {
              const name = typeof cls === 'string' ? cls : cls.name || cls.label || cls.id;
              const id = typeof cls === 'string' ? cls : cls.id;
              const isLast = ci === members.length - 1;
              return (
                <div
                  key={id || ci}
                  style={isLast ? styles.itemLast : styles.item}
                  onClick={() => id && onNavigateToNode?.(id)}
                  role="button"
                  tabIndex={0}
                  onKeyDown={(e) => e.key === 'Enter' && id && onNavigateToNode?.(id)}
                >
                  {name}
                </div>
              );
            })}
          </div>
        );
      })}
    </div>
  );
}
