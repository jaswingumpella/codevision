import { useMemo, useState } from 'react';
import axios from 'axios';
import './App.css';

const STATUS_ANALYZED = 'ANALYZED_METADATA';

const deriveProjectName = (repoUrl) => {
  if (!repoUrl) {
    return '';
  }
  let sanitized = repoUrl.trim();
  if (sanitized.endsWith('/')) {
    sanitized = sanitized.slice(0, -1);
  }
  const lastSlash = sanitized.lastIndexOf('/') + 1;
  let name = sanitized.slice(lastSlash);
  if (name.endsWith('.git')) {
    name = name.slice(0, -4);
  }
  return name;
};

const formatDate = (value) => {
  if (!value) {
    return '—';
  }
  try {
    return new Date(value).toLocaleString();
  } catch {
    return value;
  }
};

const OverviewPanel = ({ overview, loading }) => {
  if (loading) {
    return (
      <div className="overview-content">
        <h2>Project Overview</h2>
        <p className="overview-hint">Analyzing repository… this can take a moment.</p>
      </div>
    );
  }

  if (!overview) {
    return (
      <div className="overview-content">
        <h2>Project Overview</h2>
        <p className="overview-hint">Run an analysis to populate project metadata.</p>
      </div>
    );
  }

  const classList = overview.classes ?? [];
  const totalClasses = classList.length;
  const mainClasses = classList.filter((cls) => cls.sourceSet === 'MAIN').length;
  const testClasses = totalClasses - mainClasses;
  const openApiSpecs = overview.metadataDump?.openApiSpecs ?? [];

  return (
    <div className="overview-content">
      <header className="overview-header">
        <div>
          <h2>{overview.projectName}</h2>
          <p className="overview-hint">{overview.repoUrl}</p>
        </div>
        <span className="pill">Last analyzed: {formatDate(overview.analyzedAt)}</span>
      </header>

      <section className="overview-section">
        <h3>Build</h3>
        <div className="stat-grid">
          <div className="stat">
            <span className="stat-label">Group</span>
            <span className="stat-value">{overview.buildInfo?.groupId || '—'}</span>
          </div>
          <div className="stat">
            <span className="stat-label">Artifact</span>
            <span className="stat-value">{overview.buildInfo?.artifactId || '—'}</span>
          </div>
          <div className="stat">
            <span className="stat-label">Version</span>
            <span className="stat-value">{overview.buildInfo?.version || '—'}</span>
          </div>
          <div className="stat">
            <span className="stat-label">Java</span>
            <span className="stat-value">{overview.buildInfo?.javaVersion || '—'}</span>
          </div>
        </div>
      </section>

      <section className="overview-section">
        <h3>Class Coverage</h3>
        <div className="stat-grid">
          <div className="stat">
            <span className="stat-label">Total Classes</span>
            <span className="stat-value">{totalClasses}</span>
          </div>
          <div className="stat">
            <span className="stat-label">Main Source</span>
            <span className="stat-value">{mainClasses}</span>
          </div>
          <div className="stat">
            <span className="stat-label">Tests</span>
            <span className="stat-value">{testClasses}</span>
          </div>
        </div>
      </section>

      <section className="overview-section">
        <h3>OpenAPI Specs</h3>
        {openApiSpecs.length === 0 ? (
          <p className="overview-hint">No OpenAPI definitions detected.</p>
        ) : (
          <ul className="openapi-list">
            {openApiSpecs.map((spec) => (
              <li key={spec.fileName}>
                <span className="openapi-name">{spec.fileName}</span>
              </li>
            ))}
          </ul>
        )}
      </section>
    </div>
  );
};

function App() {
  const [repoUrl, setRepoUrl] = useState('');
  const [apiKey, setApiKey] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [result, setResult] = useState(null);
  const [overview, setOverview] = useState(null);

  const projectName = useMemo(() => deriveProjectName(repoUrl), [repoUrl]);

  const authHeaders = () => (apiKey ? { 'X-API-KEY': apiKey } : {});

  const handleSubmit = async (event) => {
    event.preventDefault();
    setLoading(true);
    setError('');
    setResult(null);
    setOverview(null);

    try {
      const response = await axios.post(
        '/analyze',
        { repoUrl },
        {
          headers: {
            'Content-Type': 'application/json',
            ...authHeaders()
          }
        }
      );

      const payload = response.data;
      setResult({ ...payload, projectName });

      if (payload?.projectId) {
        try {
          const overviewResponse = await axios.get(`/project/${payload.projectId}/overview`, {
            headers: {
              ...authHeaders()
            }
          });
          setOverview(overviewResponse.data);
        } catch (fetchError) {
          setError(fetchError.response?.data || 'Analysis completed, but the overview failed to load.');
        }
      }
    } catch (err) {
      setError(err.response?.data || 'Failed to analyze repository');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="app">
      <div className="grid">
        <form className="card analyze-card" onSubmit={handleSubmit}>
          <h1>CodeVision Analyzer</h1>
          <p className="hint">Launch a repository analysis to capture structural metadata.</p>

          <label htmlFor="repoUrl">Repository URL</label>
          <input
            id="repoUrl"
            type="url"
            placeholder="https://github.com/org/repo.git"
            value={repoUrl}
            onChange={(event) => setRepoUrl(event.target.value)}
            required
          />

          <label htmlFor="apiKey">API Key</label>
          <input
            id="apiKey"
            type="text"
            placeholder="Optional if disabled"
            value={apiKey}
            onChange={(event) => setApiKey(event.target.value)}
          />

          <button type="submit" disabled={loading}>
            {loading ? 'Analyzing…' : 'Analyze'}
          </button>

          {error && <p className="error">{error}</p>}
          {result && result.status === STATUS_ANALYZED && (
            <div className="success">
              <h2>Analysis complete!</h2>
              <p>
                <strong>Project:</strong> {result.projectName || projectName}
              </p>
              <p>
                <strong>ID:</strong> {result.projectId}
              </p>
            </div>
          )}
        </form>

        <section className="card overview-card">
          <OverviewPanel overview={overview} loading={loading && !overview} />
        </section>
      </div>
    </div>
  );
}

export default App;
