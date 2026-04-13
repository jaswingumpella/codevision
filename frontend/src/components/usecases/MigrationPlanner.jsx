import { useState, useCallback } from 'react';
import apiClient from '../../lib/apiClient';

const st = {
  ctr: { padding: '1rem' }, hdr: { display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1rem' },
  btn: { padding: '0.5rem 1rem', borderRadius: '4px', border: 'none', cursor: 'pointer', background: '#4f46e5', color: '#fff', fontWeight: 600 },
  off: { opacity: 0.5, cursor: 'not-allowed' }, err: { color: '#ef4444', margin: '0.5rem 0' }, spin: { color: '#6b7280', fontStyle: 'italic' },
  prompt: { padding: '1.5rem', textAlign: 'center', color: '#6b7280', border: '1px dashed #d1d5db', borderRadius: '6px', marginTop: '1rem' },
  sec: { marginBottom: '1.25rem' }, title: { fontWeight: 600, fontSize: '1rem', marginBottom: '0.5rem', color: '#374151' },
  tbl: { width: '100%', borderCollapse: 'collapse', fontSize: '0.9rem' },
  th: { textAlign: 'left', padding: '0.5rem', borderBottom: '2px solid #e5e7eb', color: '#374151' },
  td: { padding: '0.5rem', borderBottom: '1px solid #f3f4f6' },
  link: { cursor: 'pointer', color: '#4f46e5', textDecoration: 'underline' },
  stat: { display: 'inline-block', marginRight: '1.5rem', marginBottom: '0.5rem' },
  sv: { fontSize: '1.25rem', fontWeight: 700 }, sl: { fontSize: '0.8rem', color: '#6b7280' },
};

export default function MigrationPlanner({ graphData, onNavigateToNode, artifactNodeId }) {
  const [result, setResult] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  const analyze = useCallback(async () => {
    if (!graphData || !artifactNodeId) return;
    setLoading(true); setError(null);
    try { setResult((await apiClient.post(`/api/v1/usecases/migration/${encodeURIComponent(artifactNodeId)}`, graphData)).data); }
    catch (err) { setError(err.message); }
    finally { setLoading(false); }
  }, [graphData, artifactNodeId]);

  if (!artifactNodeId) {
    return (
      <div style={st.ctr}>
        <h2 style={{ marginBottom: '0.5rem' }}>Migration Planner</h2>
        <div style={st.prompt}>Select a dependency node in the graph to plan its migration impact.</div>
      </div>
    );
  }

  const affected = result?.affectedClasses || result?.affected || [];
  const methods = result?.affectedMethods || [];

  return (
    <div style={st.ctr}>
      <div style={st.hdr}>
        <h2 style={{ margin: 0 }}>Migration Planner</h2>
        <button type="button" style={{ ...st.btn, ...(loading || !graphData ? st.off : {}) }} disabled={loading || !graphData} onClick={analyze}>
          {loading ? 'Analyzing...' : 'Plan Migration'}
        </button>
      </div>
      <p style={{ color: '#6b7280', margin: '0 0 1rem' }}>Artifact: <strong>{artifactNodeId}</strong></p>
      {error && <p style={st.err}>{error}</p>}
      {loading && <p style={st.spin}>Calculating migration impact...</p>}
      {!loading && result && (<>
        <div style={{ marginBottom: '1rem' }}>
          <div style={st.stat}><div style={st.sl}>Affected Classes</div><div style={st.sv}>{affected.length}</div></div>
          <div style={st.stat}><div style={st.sl}>Affected Methods</div><div style={st.sv}>{methods.length}</div></div>
        </div>
        {affected.length > 0 && (
          <div style={st.sec}>
            <div style={st.title}>Affected Classes</div>
            <table style={st.tbl}>
              <thead><tr><th style={st.th}>Class</th><th style={st.th}>Impact</th></tr></thead>
              <tbody>
                {affected.map((c, i) => {
                  const name = typeof c === 'string' ? c : c.name || c.label || c.id;
                  const id = typeof c === 'string' ? c : c.id;
                  return (
                    <tr key={id || i}>
                      <td style={st.td}><span style={id ? st.link : {}} onClick={() => id && onNavigateToNode?.(id)}>{name}</span></td>
                      <td style={st.td}>{typeof c === 'object' ? c.impact || '\u2014' : '\u2014'}</td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        )}
        {methods.length > 0 && (
          <div style={st.sec}>
            <div style={st.title}>Affected Methods</div>
            <table style={st.tbl}>
              <thead><tr><th style={st.th}>Method</th><th style={st.th}>Class</th></tr></thead>
              <tbody>
                {methods.map((m, i) => (
                  <tr key={m.id || i}><td style={st.td}>{m.name || m.method || '\u2014'}</td><td style={st.td}>{m.className || m.owner || '\u2014'}</td></tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </>)}
    </div>
  );
}
