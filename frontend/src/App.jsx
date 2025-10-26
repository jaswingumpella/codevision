import { useMemo, useState } from 'react';
import axios from 'axios';
import './App.css';

const STATUS_ANALYZED = 'ANALYZED_BASE';

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

function App() {
  const [repoUrl, setRepoUrl] = useState('');
  const [apiKey, setApiKey] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [result, setResult] = useState(null);

  const projectName = useMemo(() => deriveProjectName(repoUrl), [repoUrl]);

  const handleSubmit = async (event) => {
    event.preventDefault();
    setLoading(true);
    setError('');
    setResult(null);

    try {
      const response = await axios.post(
        '/analyze',
        { repoUrl },
        {
          headers: {
            'Content-Type': 'application/json',
            ...(apiKey ? { 'X-API-KEY': apiKey } : {})
          }
        }
      );
      setResult({ ...response.data, projectName });
    } catch (err) {
      setError(err.response?.data || 'Failed to analyze repository');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="app">
      <form className="card" onSubmit={handleSubmit}>
        <h1>CodeVision Analyzer</h1>
        <p className="hint">Enter a Git repository URL and click Analyze to ingest it.</p>

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
    </div>
  );
}

export default App;
