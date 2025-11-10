import React from 'react';

const Section = ({ title, children }) => (
  <section className="compiled-section">
    <header>
      <h3>{title}</h3>
    </header>
    {children}
  </section>
);

const Table = ({ columns, rows, emptyLabel }) => {
  if (!rows || rows.length === 0) {
    return <p className="overview-hint">{emptyLabel}</p>;
  }
  return (
    <div className="compiled-table-wrapper">
      <table>
        <thead>
          <tr>
            {columns.map((col) => (
              <th key={col.key}>{col.label}</th>
            ))}
          </tr>
        </thead>
        <tbody>
          {rows.map((row, idx) => (
            <tr key={`${row.key || row.id || idx}`}>
              {columns.map((col) => (
                <td key={col.key}>{row[col.key] ?? '—'}</td>
              ))}
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
};

const CompiledAnalysisPanel = ({
  repoPath,
  onRepoPathChange,
  acceptPackages,
  onAcceptPackagesChange,
  onRunAnalysis,
  analysis,
  exports,
  entities,
  sequences,
  endpoints,
  mermaidSource,
  loading,
  error,
  onDownloadExport,
  onRefreshExports
}) => {
  const renderStatus = () => {
    if (!analysis) {
      return <p className="overview-hint">Run the compiled analysis to populate artifacts.</p>;
    }
    return (
      <ul className="compiled-status-list">
        <li>
          <strong>Status:</strong> {analysis.status}
        </li>
        <li>
          <strong>Entities:</strong> {analysis.entityCount ?? 0}
        </li>
        <li>
          <strong>Endpoints:</strong> {analysis.endpointCount ?? 0}
        </li>
        <li>
          <strong>Dependencies:</strong> {analysis.dependencyCount ?? 0}
        </li>
        <li>
          <strong>Sequence generators:</strong> {analysis.sequenceCount ?? 0}
        </li>
        <li>
          <strong>Output dir:</strong> {analysis.outputDirectory || 'n/a'}
        </li>
      </ul>
    );
  };

  return (
    <div className="overview-content compiled-panel">
      <Section title="Run Compiled Analysis">
        <div className="compiled-form-grid">
          <label>
            <span>Repository path</span>
            <input
              type="text"
              value={repoPath}
              onChange={(event) => onRepoPathChange(event.target.value)}
              placeholder="/Users/me/work/backend"
            />
          </label>
          <label>
            <span>Accept packages (comma separated)</span>
            <input
              type="text"
              value={acceptPackages}
              onChange={(event) => onAcceptPackagesChange(event.target.value)}
              placeholder="com.barclays,com.codeviz2"
            />
          </label>
        </div>
        <div className="compiled-actions">
          <button type="button" onClick={onRunAnalysis} disabled={loading}>
            {loading ? 'Analyzing…' : 'Run Analysis'}
          </button>
        </div>
        {error ? <p className="error-text">{error}</p> : null}
        {renderStatus()}
      </Section>

      <Section title="Exports">
        <div className="compiled-actions">
          <button type="button" onClick={onRefreshExports} disabled={!analysis || loading}>
            Refresh exports
          </button>
        </div>
        {exports && exports.length > 0 ? (
          <ul className="compiled-export-list">
            {exports.map((file) => (
              <li key={file.name}>
                <div>
                  <strong>{file.name}</strong>
                  <span className="overview-hint">{file.size ? `${file.size} bytes` : ''}</span>
                </div>
                <button type="button" onClick={() => onDownloadExport(file)}>
                  Download
                </button>
              </li>
            ))}
          </ul>
        ) : (
          <p className="overview-hint">No exports yet.</p>
        )}
      </Section>

      <Section title="Entities (sample)">
        <Table
          columns={[
            { key: 'className', label: 'Class' },
            { key: 'tableName', label: 'Table' },
            { key: 'origin', label: 'Origin' },
            { key: 'inCycle', label: 'Cycle?' }
          ]}
          rows={(entities?.items || []).slice(0, 10).map((item, index) => ({
            key: `${item.className}-${index}`,
            ...item,
            inCycle: item.inCycle ? 'Yes' : 'No'
          }))}
          emptyLabel="Entities will appear after the analysis completes."
        />
      </Section>

      <Section title="Sequences (sample)">
        <Table
          columns={[
            { key: 'generatorName', label: 'Generator' },
            { key: 'sequenceName', label: 'Sequence' },
            { key: 'allocationSize', label: 'Alloc' },
            { key: 'initialValue', label: 'Initial' }
          ]}
          rows={(sequences?.items || []).slice(0, 10).map((item, index) => ({
            key: `${item.generatorName}-${index}`,
            ...item
          }))}
          emptyLabel="Sequence generators will appear here when detected."
        />
      </Section>

      <Section title="Endpoints (sample)">
        <Table
          columns={[
            { key: 'httpMethod', label: 'Method' },
            { key: 'path', label: 'Path' },
            { key: 'controllerClass', label: 'Controller' },
            { key: 'framework', label: 'Framework' }
          ]}
          rows={(endpoints?.items || []).slice(0, 10).map((item, index) => ({
            key: `${item.controllerClass}-${item.path}-${index}`,
            ...item
          }))}
          emptyLabel="Endpoints will appear here when detected."
        />
      </Section>

      <Section title="Mermaid ERD">
        <details open>
          <summary>Mermaid Source</summary>
          <pre className="diagram-source">{mermaidSource || 'Run analysis to populate the ERD.'}</pre>
        </details>
      </Section>
    </div>
  );
};

export default CompiledAnalysisPanel;
