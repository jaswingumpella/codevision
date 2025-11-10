import { useMemo, useState } from 'react';
import { textMatches } from '../../utils/formatters';

const PiiPciPanel = ({ findings, loading, onDownloadCsv, onDownloadPdf, onToggleIgnored, searchQuery }) => {
  const [typeFilter, setTypeFilter] = useState('ALL');
  const [severityFilter, setSeverityFilter] = useState('ALL');
  const [hideIgnored, setHideIgnored] = useState(true);

  if (loading) {
    return (
      <div className="overview-content">
        <h2>PCI / PII Scan</h2>
        <p className="overview-hint">Collecting sensitive data findings…</p>
      </div>
    );
  }

  const normalizedSearch = (searchQuery || '').trim().toLowerCase();
  const filtered = useMemo(
    () =>
      (findings || []).filter((entry) => {
        const typeMatches = typeFilter === 'ALL' || (entry.matchType || '').toUpperCase() === typeFilter;
        const severityMatches = severityFilter === 'ALL' || (entry.severity || '').toUpperCase() === severityFilter;
        const ignoreMatches = !hideIgnored || !entry.ignored;
        const searchMatches =
          !normalizedSearch || textMatches(normalizedSearch, entry.filePath, entry.snippet, entry.matchType, entry.severity);
        return typeMatches && severityMatches && ignoreMatches && searchMatches;
      }),
    [findings, typeFilter, severityFilter, hideIgnored, normalizedSearch]
  );

  const exportDisabled = typeof onDownloadCsv !== 'function' || typeof onDownloadPdf !== 'function';

  return (
    <div className="overview-content">
      <h2>PCI / PII Scan</h2>

      <div className="filter-controls">
        <label htmlFor="typeFilter">
          Match Type
          <select id="typeFilter" value={typeFilter} onChange={(event) => setTypeFilter(event.target.value)}>
            {['ALL', 'PII', 'PCI'].map((type) => (
              <option key={type} value={type}>{type}</option>
            ))}
          </select>
        </label>

        <label htmlFor="severityFilter">
          Severity
          <select id="severityFilter" value={severityFilter} onChange={(event) => setSeverityFilter(event.target.value)}>
            {['ALL', 'LOW', 'MEDIUM', 'HIGH'].map((level) => (
              <option key={level} value={level}>{level}</option>
            ))}
          </select>
        </label>

        <div className="toggle-group">
          <label>
            <input type="checkbox" checked={hideIgnored} onChange={(event) => setHideIgnored(event.target.checked)} />
            Hide ignored matches
          </label>
        </div>
      </div>

      <div className="export-actions">
        <button type="button" onClick={onDownloadCsv} disabled={exportDisabled}>
          Download CSV
        </button>
        <button type="button" onClick={onDownloadPdf} disabled={exportDisabled}>
          Download PDF
        </button>
      </div>

      {filtered.length === 0 ? (
        <p className="overview-hint">No findings match the selected filters.</p>
      ) : (
        <table className="api-table">
          <thead>
            <tr>
              <th>File</th>
              <th>Line</th>
              <th>Snippet</th>
              <th>Type</th>
              <th>Severity</th>
              <th>Ignored?</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>
            {filtered.map((entry, index) => (
              <tr key={`${entry.filePath}-${entry.lineNumber}-${index}`}>
                <td>{entry.filePath || '—'}</td>
                <td>{entry.lineNumber > 0 ? entry.lineNumber : '—'}</td>
                <td>
                  {entry.snippet ? (
                    <code className="query-snippet">{entry.snippet}</code>
                  ) : (
                    <span className="overview-hint">—</span>
                  )}
                </td>
                <td>
                  <span className="badge badge-info">{entry.matchType || '—'}</span>
                </td>
                <td>
                  <span className={`badge ${entry.severity === 'HIGH' ? 'badge-alert' : 'badge-info'}`}>
                    {entry.severity || '—'}
                  </span>
                </td>
                <td>{entry.ignored ? 'Yes' : 'No'}</td>
                <td>
                  {typeof onToggleIgnored === 'function' ? (
                    <button
                      type="button"
                      className="ghost-button"
                      disabled={!entry.findingId}
                      onClick={async () => entry.findingId && (await onToggleIgnored(entry.findingId, !entry.ignored))}
                    >
                      {entry.ignored ? 'Restore' : 'Ignore'}
                    </button>
                  ) : (
                    <span className="overview-hint">—</span>
                  )}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </div>
  );
};

export default PiiPciPanel;
