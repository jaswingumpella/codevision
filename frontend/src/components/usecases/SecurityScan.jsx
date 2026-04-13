import { useState, useCallback, useMemo } from 'react';
import apiClient from '../../lib/apiClient';

const base = { display: 'inline-block', padding: '0.15rem 0.5rem', borderRadius: '4px', fontWeight: 600, fontSize: '0.8rem' };
const sev = { HIGH: { ...base, background: '#fee2e2', color: '#b91c1c' }, MEDIUM: { ...base, background: '#fef3c7', color: '#92400e' }, LOW: { ...base, background: '#d1fae5', color: '#065f46' } };
const st = {
  ctr: { padding: '1rem' }, hdr: { display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1rem' },
  btn: { padding: '0.5rem 1rem', borderRadius: '4px', border: 'none', cursor: 'pointer', background: '#4f46e5', color: '#fff', fontWeight: 600 },
  off: { opacity: 0.5, cursor: 'not-allowed' }, err: { color: '#ef4444', margin: '0.5rem 0' }, spin: { color: '#6b7280', fontStyle: 'italic' },
  sec: { marginBottom: '1.25rem' }, title: { fontWeight: 600, fontSize: '1rem', marginBottom: '0.5rem', color: '#374151' },
  tbl: { width: '100%', borderCollapse: 'collapse', fontSize: '0.9rem' },
  th: { textAlign: 'left', padding: '0.5rem', borderBottom: '2px solid #e5e7eb', color: '#374151' },
  td: { padding: '0.5rem', borderBottom: '1px solid #f3f4f6' },
  sum: { display: 'flex', gap: '1.5rem', marginBottom: '1rem' },
  sv: { fontSize: '1.25rem', fontWeight: 700 }, sl: { fontSize: '0.8rem', color: '#6b7280' },
};
const sevOf = (s) => sev[s?.toUpperCase()] || sev.LOW;
const link = (id) => ({ cursor: id ? 'pointer' : 'default', color: id ? '#4f46e5' : 'inherit' });

export default function SecurityScan({ graphData, onNavigateToNode }) {
  const [result, setResult] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  const analyze = useCallback(async () => {
    if (!graphData) return;
    setLoading(true); setError(null);
    try { setResult((await apiClient.post('/api/v1/usecases/security', graphData)).data); }
    catch (err) { setError(err.message); }
    finally { setLoading(false); }
  }, [graphData]);

  const findings = result?.findings || result?.issues || [];
  const sensitiveFields = result?.sensitiveFields || [];
  const directDbAccess = result?.directDbAccess || [];
  const counts = useMemo(() => {
    const c = { HIGH: 0, MEDIUM: 0, LOW: 0 };
    findings.forEach((f) => { c[(f.severity || 'LOW').toUpperCase()] += 1; });
    return c;
  }, [findings]);

  const Table = ({ heads, rows }) => (
    <table style={st.tbl}>
      <thead><tr>{heads.map((h) => <th key={h} style={st.th}>{h}</th>)}</tr></thead>
      <tbody>{rows}</tbody>
    </table>
  );

  return (
    <div style={st.ctr}>
      <div style={st.hdr}>
        <h2 style={{ margin: 0 }}>Security Scan</h2>
        <button type="button" style={{ ...st.btn, ...(loading || !graphData ? st.off : {}) }} disabled={loading || !graphData} onClick={analyze}>
          {loading ? 'Scanning...' : 'Run Scan'}
        </button>
      </div>
      {error && <p style={st.err}>{error}</p>}
      {loading && <p style={st.spin}>Running security analysis...</p>}
      {!loading && result && !findings.length && !sensitiveFields.length && !directDbAccess.length && (
        <p style={{ color: '#6b7280' }}>No security findings detected.</p>
      )}
      {findings.length > 0 && (<>
        <div style={st.sum}>
          {['HIGH', 'MEDIUM', 'LOW'].map((l) => (
            <div key={l} style={{ textAlign: 'center' }}><div style={st.sv}>{counts[l]}</div><div style={{ ...st.sl, color: sev[l].color }}>{l}</div></div>
          ))}
        </div>
        <div style={st.sec}>
          <div style={st.title}>Findings ({findings.length})</div>
          <Table heads={['Severity', 'Description', 'Location']} rows={findings.map((f, i) => (
            <tr key={f.id || i}>
              <td style={st.td}><span style={sevOf(f.severity)}>{(f.severity || 'LOW').toUpperCase()}</span></td>
              <td style={st.td}>{f.description || f.message || '\u2014'}</td>
              <td style={st.td}><span style={link(f.nodeId)} onClick={() => f.nodeId && onNavigateToNode?.(f.nodeId)}>{f.location || f.className || '\u2014'}</span></td>
            </tr>
          ))} />
        </div>
      </>)}
      {sensitiveFields.length > 0 && (
        <div style={st.sec}>
          <div style={st.title}>Sensitive Fields ({sensitiveFields.length})</div>
          <Table heads={['Field', 'Class', 'Type']} rows={sensitiveFields.map((sf, i) => (
            <tr key={i}><td style={st.td}>{sf.fieldName || sf.name || '\u2014'}</td><td style={st.td}>{sf.className || sf.owner || '\u2014'}</td>
              <td style={st.td}><span style={sevOf('HIGH')}>{sf.sensitivityType || sf.type || '\u2014'}</span></td></tr>
          ))} />
        </div>
      )}
      {directDbAccess.length > 0 && (
        <div style={st.sec}>
          <div style={st.title}>Direct DB Access from Endpoints ({directDbAccess.length})</div>
          <Table heads={['Endpoint', 'Entity']} rows={directDbAccess.map((d, i) => (
            <tr key={i}><td style={st.td}>{d.endpoint || d.source || '\u2014'}</td><td style={st.td}>{d.entity || d.target || '\u2014'}</td></tr>
          ))} />
        </div>
      )}
    </div>
  );
}
