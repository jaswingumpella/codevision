import { useMemo, useState } from 'react';
import { textMatches } from '../../utils/formatters';

const LoggerInsightsPanel = ({ insights, loading, onDownloadCsv, onDownloadPdf, searchQuery }) => {
  const [classFilter, setClassFilter] = useState('');
  const [levelFilter, setLevelFilter] = useState('ALL');
  const [piiOnly, setPiiOnly] = useState(false);
  const [pciOnly, setPciOnly] = useState(false);
  const [expandAll, setExpandAll] = useState(false);
  const [messageFilter, setMessageFilter] = useState('');

  if (loading) {
    return (
      <div className="overview-content">
        <h2>Logger Insights</h2>
        <p className="overview-hint">Scanning log statements…</p>
      </div>
    );
  }

  const normalizedFilter = classFilter.trim().toLowerCase();
  const normalizedSearch = (searchQuery || '').trim().toLowerCase();
  const normalizedMessage = messageFilter.trim().toLowerCase();
  const filtered = useMemo(
    () =>
      (insights || []).filter((entry) => {
        const levelMatches = levelFilter === 'ALL' || (entry.logLevel || '').toUpperCase() === levelFilter;
        const classMatches = !normalizedFilter || (entry.className || '').toLowerCase().includes(normalizedFilter);
        const piiMatches = !piiOnly || entry.piiRisk;
        const pciMatches = !pciOnly || entry.pciRisk;
        const textMatchesFilter =
          !normalizedSearch ||
          textMatches(normalizedSearch, entry.className, entry.filePath, entry.messageTemplate, (entry.variables || []).join(', '));
        const messageMatches =
          !normalizedMessage ||
          textMatches(normalizedMessage, entry.messageTemplate, entry.variables?.join(', '));
        return levelMatches && classMatches && piiMatches && pciMatches && textMatchesFilter && messageMatches;
      }),
    [insights, levelFilter, normalizedFilter, piiOnly, pciOnly, normalizedSearch, normalizedMessage]
  );

  const renderMessage = (message) => {
    if (!message) {
      return <span className="overview-hint">—</span>;
    }
    if (expandAll) {
      return <code className="query-snippet">{message}</code>;
    }
    return <button type="button" className="ghost-button" onClick={() => setExpandAll(true)}>View snippet</button>;
  };

  const exportDisabled = typeof onDownloadCsv !== 'function' || typeof onDownloadPdf !== 'function';

  return (
    <div className="overview-content">
      <h2>Logger Insights</h2>

      <div className="filter-controls">
        <label htmlFor="classFilter">
          Class Filter
          <input
            id="classFilter"
            type="text"
            value={classFilter}
            placeholder="e.g. com.acme.OrderService"
            onChange={(event) => setClassFilter(event.target.value)}
          />
        </label>

        <label htmlFor="levelFilter">
          Level
          <select id="levelFilter" value={levelFilter} onChange={(event) => setLevelFilter(event.target.value)}>
            {['ALL', 'TRACE', 'DEBUG', 'INFO', 'WARN', 'ERROR'].map((level) => (
              <option key={level} value={level}>{level}</option>
            ))}
          </select>
        </label>

        <div className="toggle-group">
          <label>
            <input type="checkbox" checked={piiOnly} onChange={(event) => setPiiOnly(event.target.checked)} />
            Only PII risk
          </label>
          <label>
            <input type="checkbox" checked={pciOnly} onChange={(event) => setPciOnly(event.target.checked)} />
            Only PCI risk
          </label>
        </div>

        <label htmlFor="messageFilter">
          Message Search
          <input
            id="messageFilter"
            type="text"
            value={messageFilter}
            placeholder="Search message text"
            onChange={(event) => setMessageFilter(event.target.value)}
          />
        </label>

        <div className="toggle-group">
          <button
            type="button"
            className="ghost-button"
            onClick={() => setExpandAll((prev) => !prev)}
          >
            {expandAll ? 'Collapse All' : 'Expand All'}
          </button>
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
        <p className="overview-hint">No log statements match the selected filters.</p>
      ) : (
        <table className="api-table">
          <thead>
            <tr>
              <th>Class</th>
              <th>File</th>
              <th>Level</th>
              <th>Line</th>
              <th>Message</th>
              <th>Variables</th>
              <th>PII</th>
              <th>PCI</th>
            </tr>
          </thead>
          <tbody>
            {filtered.map((entry, index) => (
              <tr key={`${entry.className}-${entry.lineNumber}-${index}`}>
                <td>{entry.className || '—'}</td>
                <td>{entry.filePath || '—'}</td>
                <td>{entry.logLevel || '—'}</td>
                <td>{entry.lineNumber >= 0 ? entry.lineNumber : '—'}</td>
                <td>{renderMessage(entry.messageTemplate)}</td>
                <td>{entry.variables && entry.variables.length > 0 ? entry.variables.join(', ') : '—'}</td>
                <td>
                  <span className={`badge ${entry.piiRisk ? 'badge-alert' : 'badge-muted'}`}>
                    {entry.piiRisk ? 'Yes' : 'No'}
                  </span>
                </td>
                <td>
                  <span className={`badge ${entry.pciRisk ? 'badge-alert' : 'badge-muted'}`}>
                    {entry.pciRisk ? 'Yes' : 'No'}
                  </span>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </div>
  );
};

export default LoggerInsightsPanel;
