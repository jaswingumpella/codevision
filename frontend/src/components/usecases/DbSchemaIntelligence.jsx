import { useState, useCallback } from 'react';
import apiClient from '../../lib/apiClient';

const st = {
  ctr: { padding: '1rem' }, hdr: { display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1rem' },
  btn: { padding: '0.5rem 1rem', borderRadius: '4px', border: 'none', cursor: 'pointer', background: '#4f46e5', color: '#fff', fontWeight: 600 },
  off: { opacity: 0.5, cursor: 'not-allowed' }, err: { color: '#ef4444', margin: '0.5rem 0' }, spin: { color: '#6b7280', fontStyle: 'italic' },
  sec: { marginBottom: '1.5rem' }, title: { fontWeight: 600, fontSize: '1rem', marginBottom: '0.5rem', color: '#374151' },
  tbl: { width: '100%', borderCollapse: 'collapse', fontSize: '0.9rem' },
  th: { textAlign: 'left', padding: '0.5rem', borderBottom: '2px solid #e5e7eb', color: '#374151' },
  td: { padding: '0.5rem', borderBottom: '1px solid #f3f4f6' },
  link: { cursor: 'pointer', color: '#4f46e5', textDecoration: 'underline' },
};

export default function DbSchemaIntelligence({ graphData, onNavigateToNode }) {
  const [result, setResult] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  const analyze = useCallback(async () => {
    if (!graphData) return;
    setLoading(true); setError(null);
    try { setResult((await apiClient.post('/api/v1/usecases/db-schema', graphData)).data); }
    catch (err) { setError(err.message); }
    finally { setLoading(false); }
  }, [graphData]);

  const entities = result?.entities || [];
  const relationships = result?.relationships || [];

  return (
    <div style={st.ctr}>
      <div style={st.hdr}>
        <h2 style={{ margin: 0 }}>DB Schema Intelligence</h2>
        <button type="button" style={{ ...st.btn, ...(loading || !graphData ? st.off : {}) }} disabled={loading || !graphData} onClick={analyze}>
          {loading ? 'Analyzing...' : 'Run Analysis'}
        </button>
      </div>
      {error && <p style={st.err}>{error}</p>}
      {loading && <p style={st.spin}>Scanning database schema...</p>}
      {!loading && result && entities.length === 0 && <p style={{ color: '#6b7280' }}>No database entities found.</p>}

      {entities.length > 0 && (
        <div style={st.sec}>
          <div style={st.title}>Entities ({entities.length})</div>
          <table style={st.tbl}>
            <thead><tr><th style={st.th}>Entity</th><th style={st.th}>Table</th><th style={st.th}>Columns</th></tr></thead>
            <tbody>
              {entities.map((e, i) => (
                <tr key={e.id || i}>
                  <td style={st.td}>
                    <span style={e.id ? st.link : {}} onClick={() => e.id && onNavigateToNode?.(e.id)}
                      role={e.id ? 'button' : undefined} tabIndex={e.id ? 0 : undefined}
                      onKeyDown={(ev) => ev.key === 'Enter' && e.id && onNavigateToNode?.(e.id)}>
                      {e.name || e.label || '\u2014'}
                    </span>
                  </td>
                  <td style={st.td}>{e.tableName || '\u2014'}</td>
                  <td style={st.td}>{e.columnCount ?? e.columns?.length ?? '\u2014'}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {relationships.length > 0 && (
        <div style={st.sec}>
          <div style={st.title}>Relationships ({relationships.length})</div>
          <table style={st.tbl}>
            <thead><tr><th style={st.th}>From</th><th style={st.th}>To</th><th style={st.th}>Type</th></tr></thead>
            <tbody>
              {relationships.map((r, i) => (
                <tr key={i}>
                  <td style={st.td}>{r.from || r.source || '\u2014'}</td>
                  <td style={st.td}>{r.to || r.target || '\u2014'}</td>
                  <td style={st.td}>{r.type || r.relationType || '\u2014'}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}
