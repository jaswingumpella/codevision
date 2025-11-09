const ExportPanel = ({ projectId, onDownloadHtml, onDownloadSnapshot, htmlPreview, loading, onRefreshPreview }) => (
  <div className="overview-content">
    <h2>Export &amp; Sharing</h2>
    <p className="overview-hint">
      Download the Confluence-ready HTML or raw JSON snapshot for AI copilots, compliance reviews, or to archive the analysis.
    </p>

    <div className="export-actions">
      <button type="button" onClick={onDownloadHtml} disabled={!projectId || loading}>
        Download HTML
      </button>
      <button type="button" onClick={onDownloadSnapshot} disabled={!projectId || loading}>
        Download Snapshot JSON
      </button>
      <button type="button" className="ghost-button" onClick={onRefreshPreview} disabled={!projectId || loading}>
        Refresh preview
      </button>
    </div>

    <div className="export-preview">
      {loading ? (
        <p className="overview-hint">Loading export preview…</p>
      ) : htmlPreview ? (
        <iframe title="Confluence Export Preview" srcDoc={htmlPreview} />
      ) : (
        <p className="overview-hint">Run an analysis and refresh to view the Confluence-ready HTML.</p>
      )}
    </div>
  </div>
);

export default ExportPanel;
