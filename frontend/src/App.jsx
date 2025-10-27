import { useEffect, useMemo, useState } from 'react';
import axios from 'axios';
import './App.css';

const STATUS_ANALYZED = 'ANALYZED_METADATA';
const PAGE_SIZE = 10;

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

const ApiSpecsPanel = ({ overview, apiCatalog, loading }) => {
  const [pages, setPages] = useState({ REST: 0, SOAP: 0, LEGACY: 0 });

  useEffect(() => {
    setPages({ REST: 0, SOAP: 0, LEGACY: 0 });
  }, [apiCatalog, overview]);

  if (loading) {
    return (
      <div className="overview-content">
        <h2>API Catalog</h2>
        <p className="overview-hint">Loading endpoints…</p>
      </div>
    );
  }

  if (!overview) {
    return (
      <div className="overview-content">
        <h2>API Catalog</h2>
        <p className="overview-hint">Run an analysis to view endpoints and specifications.</p>
      </div>
    );
  }

  const endpoints = apiCatalog?.endpoints ?? [];
  const grouped = useMemo(() => {
    const buckets = {
      REST: [],
      SOAP: [],
      LEGACY: []
    };
    endpoints.forEach((endpoint) => {
      if ((endpoint.protocol || '').toUpperCase() === 'REST') {
        buckets.REST.push(endpoint);
      } else if ((endpoint.protocol || '').toUpperCase() === 'SOAP') {
        buckets.SOAP.push(endpoint);
      } else {
        buckets.LEGACY.push(endpoint);
      }
    });
    return buckets;
  }, [endpoints]);

  const metadata = overview.metadataDump ?? {};
  const openApiSpecs = metadata.openApiSpecs ?? [];
  const wsdlDocs = metadata.wsdlDocuments ?? [];
  const xsdDocs = metadata.xsdDocuments ?? [];
  const soapServices = metadata.soapServices ?? [];
  const assets = overview.assets?.images ?? [];

  const renderEndpoints = (title, type, list) => {
    if (list.length === 0) {
      return (
        <section className="api-section" key={title}>
          <h3>{title}</h3>
          <p className="overview-hint">No endpoints found.</p>
        </section>
      );
    }

    const totalPages = Math.ceil(list.length / PAGE_SIZE);
    const currentPage = Math.min(pages[type] ?? 0, Math.max(totalPages - 1, 0));
    const sliceStart = currentPage * PAGE_SIZE;
    const pageItems = list.slice(sliceStart, sliceStart + PAGE_SIZE);

    const goToPage = (nextPage) => {
      setPages((prev) => ({ ...prev, [type]: nextPage }));
    };

    return (
      <section className="api-section" key={title}>
        <h3>{title}</h3>
        <table className="api-table">
          <thead>
            <tr>
              <th>Method</th>
              <th>Path / Operation</th>
              <th>Class</th>
              <th>Handler</th>
              <th>Specs</th>
            </tr>
          </thead>
          <tbody>
            {pageItems.map((endpoint, index) => (
              <tr key={`${endpoint.controllerClass}-${endpoint.controllerMethod}-${index}`}>
                <td>{endpoint.httpMethod || '—'}</td>
                <td>{endpoint.pathOrOperation}</td>
                <td>{endpoint.controllerClass}</td>
                <td>{endpoint.controllerMethod || '—'}</td>
                <td>
                  {endpoint.specArtifacts && endpoint.specArtifacts.length > 0 ? (
                    endpoint.specArtifacts.map((artifact) => (
                      <span key={`${artifact.type}-${artifact.name}`} className="spec-pill">
                        {artifact.type}: {artifact.name}
                      </span>
                    ))
                  ) : (
                    <span className="overview-hint">None</span>
                  )}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
        {totalPages > 1 && (
          <div className="pagination">
            <button
              type="button"
              className="pagination-button"
              onClick={() => goToPage(currentPage - 1)}
              disabled={currentPage === 0}
            >
              Previous
            </button>
            <span className="pagination-info">Page {currentPage + 1} of {totalPages}</span>
            <button
              type="button"
              className="pagination-button"
              onClick={() => goToPage(currentPage + 1)}
              disabled={currentPage >= totalPages - 1}
            >
              Next
            </button>
          </div>
        )}
      </section>
    );
  };

  return (
    <div className="overview-content">
      <h2>API Catalog</h2>
      {renderEndpoints('REST Endpoints', 'REST', grouped.REST)}
      {renderEndpoints('SOAP Endpoints', 'SOAP', grouped.SOAP)}
      {renderEndpoints('Legacy Endpoints', 'LEGACY', grouped.LEGACY)}

      <section className="api-section">
        <h3>OpenAPI Specifications</h3>
        {openApiSpecs.length === 0 ? (
          <p className="overview-hint">No OpenAPI documents detected.</p>
        ) : (
          openApiSpecs.map((spec) => (
            <details key={spec.fileName} className="spec-doc" open={false}>
              <summary>{spec.fileName}</summary>
              <pre>{spec.content}</pre>
            </details>
          ))
        )}
      </section>

      <section className="api-section">
        <h3>SOAP Specifications</h3>
        {wsdlDocs.length === 0 && xsdDocs.length === 0 ? (
          <p className="overview-hint">No SOAP documents detected.</p>
        ) : (
          <div className="spec-doc-group">
            {wsdlDocs.map((doc) => (
              <details key={`wsdl-${doc.fileName}`} className="spec-doc">
                <summary>{doc.fileName}</summary>
                <pre>{doc.content}</pre>
              </details>
            ))}
            {xsdDocs.map((doc) => (
              <details key={`xsd-${doc.fileName}`} className="spec-doc">
                <summary>{doc.fileName}</summary>
                <pre>{doc.content}</pre>
              </details>
            ))}
            {soapServices.length > 0 && (
              <div className="soap-summary">
                <h4>Service Summary</h4>
                {soapServices.map((service) => (
                  <div key={`${service.fileName}-${service.serviceName}`} className="soap-service">
                    <strong>{service.serviceName}</strong>
                    <span className="overview-hint">Source: {service.fileName}</span>
                    <ul>
                      {(service.ports || []).map((port) => (
                        <li key={`${service.serviceName}-${port.portName}`}>
                          <strong>{port.portName}:</strong> {(port.operations || []).join(', ') || '—'}
                        </li>
                      ))}
                    </ul>
                  </div>
                ))}
              </div>
            )}
          </div>
        )}
      </section>

      <section className="api-section">
        <h3>Media Assets</h3>
        {assets.length === 0 ? (
          <p className="overview-hint">No image assets detected.</p>
        ) : (
          <ul className="asset-list">
            {assets.map((asset) => (
              <li key={asset.relativePath}>
                <span className="asset-name">{asset.fileName}</span>
                <span className="asset-path">{asset.relativePath}</span>
                {asset.sizeBytes ? <span className="asset-size">{asset.sizeBytes.toLocaleString()} bytes</span> : null}
              </li>
            ))}
          </ul>
        )}
      </section>
    </div>
  );
};

const DatabasePanel = ({ analysis, loading }) => {
  if (loading) {
    return (
      <div className="overview-content">
        <h2>Database Analysis</h2>
        <p className="overview-hint">Loading database metadata…</p>
      </div>
    );
  }

  if (!analysis) {
    return (
      <div className="overview-content">
        <h2>Database Analysis</h2>
        <p className="overview-hint">Run an analysis to view entities and repository activity.</p>
      </div>
    );
  }

  const entities = Array.isArray(analysis.entities) ? analysis.entities : [];
  const classesByEntity = analysis.classesByEntity || {};
  const operationsByClass = analysis.operationsByClass || {};

  const entityRows = [...entities.map((entity) => {
    const entityName = entity.entityName || entity.fullyQualifiedName || entity.tableName || 'Unknown entity';
    const classList = classesByEntity[entity.entityName] || classesByEntity[entityName] || [];
    return {
      key: `${entityName}-${entity.tableName || 'nt'}`,
      entityName,
      tableName: entity.tableName || '—',
      primaryKeys: (entity.primaryKeys || []).filter(Boolean).join(', ') || '—',
      classes: Array.isArray(classList) ? classList : []
    };
  }),
  ...Object.entries(classesByEntity)
      .filter(([name]) => !entities.some((entity) => (entity.entityName || entity.fullyQualifiedName) === name))
      .map(([name, classList]) => ({
        key: `extra-${name}`,
        entityName: name,
        tableName: '—',
        primaryKeys: '—',
        classes: Array.isArray(classList) ? classList : []
      }))]
    .filter(Boolean)
    .sort((a, b) => a.entityName.localeCompare(b.entityName));

  const operationRows = Object.entries(operationsByClass)
    .flatMap(([repository, ops]) => {
      const entries = Array.isArray(ops) ? ops : [];
      return entries
        .filter(Boolean)
        .map((op, index) => ({
          key: `${repository}-${op.methodName || index}`,
          repository,
          methodName: op.methodName || '—',
          operationType: op.operationType || '—',
          target: op.target || '—',
          querySnippet: op.querySnippet || ''
        }));
    })
    .sort((a, b) => {
      const repoCompare = a.repository.localeCompare(b.repository);
      if (repoCompare !== 0) {
        return repoCompare;
      }
      return a.methodName.localeCompare(b.methodName);
    });

  return (
    <div className="overview-content">
      <h2>Database Analysis</h2>

      <section className="api-section">
        <h3>Entities and Interacting Classes</h3>
        {entityRows.length === 0 ? (
          <p className="overview-hint">No JPA entities detected.</p>
        ) : (
          <table className="api-table">
            <thead>
              <tr>
                <th>Entity</th>
                <th>Table</th>
                <th>Primary Keys</th>
                <th>Classes Using It</th>
              </tr>
            </thead>
            <tbody>
              {entityRows.map((row) => (
                <tr key={row.key}>
                  <td>{row.entityName}</td>
                  <td>{row.tableName}</td>
                  <td>{row.primaryKeys}</td>
                  <td>
                    {row.classes.length === 0 ? (
                      <span className="overview-hint">—</span>
                    ) : (
                      row.classes.map((cls) => (
                        <div key={`${row.key}-${cls}`}>{cls}</div>
                      ))
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </section>

      <section className="api-section">
        <h3>DAO Operations by Class</h3>
        {operationRows.length === 0 ? (
          <p className="overview-hint">No repository or DAO methods were classified.</p>
        ) : (
          <table className="api-table">
            <thead>
              <tr>
                <th>Repository / DAO</th>
                <th>Method</th>
                <th>Operation</th>
                <th>Target</th>
                <th>Query Snippet</th>
              </tr>
            </thead>
            <tbody>
              {operationRows.map((row) => (
                <tr key={row.key}>
                  <td>{row.repository}</td>
                  <td>{row.methodName}</td>
                  <td>{row.operationType}</td>
                  <td>{row.target}</td>
                  <td>
                    {row.querySnippet ? (
                      <code className="query-snippet">{row.querySnippet}</code>
                    ) : (
                      <span className="overview-hint">—</span>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
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
  const [activeTab, setActiveTab] = useState('overview');
  const [apiCatalog, setApiCatalog] = useState(null);
  const [apiLoading, setApiLoading] = useState(false);
  const [dbAnalysis, setDbAnalysis] = useState(null);
  const [dbLoading, setDbLoading] = useState(false);

  const projectName = useMemo(() => deriveProjectName(repoUrl), [repoUrl]);

  const authHeaders = () => (apiKey ? { 'X-API-KEY': apiKey } : {});

  const handleSubmit = async (event) => {
    event.preventDefault();
    setLoading(true);
    setError('');
    setResult(null);
    setOverview(null);
    setActiveTab('overview');
    setApiCatalog(null);
    setApiLoading(false);
    setDbAnalysis(null);
    setDbLoading(false);

    console.info('Submitting analysis request', {
      repoUrl,
      hasApiKey: Boolean(apiKey)
    });

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
      console.info('Analysis response received', payload);
      setResult({ ...payload, projectName });

      if (payload?.projectId) {
        try {
          const overviewResponse = await axios.get(`/project/${payload.projectId}/overview`, {
            headers: {
              ...authHeaders()
            }
          });
          console.info('Loaded project overview', {
            projectId: payload.projectId,
            classCount: overviewResponse.data?.classes?.length ?? 0
          });
          setOverview(overviewResponse.data);
          try {
            setApiLoading(true);
            const endpointsResponse = await axios.get(`/project/${payload.projectId}/api-endpoints`, {
              headers: {
                ...authHeaders()
              }
            });
            console.info('Loaded API endpoints', {
              projectId: payload.projectId,
              endpointCount: endpointsResponse.data?.endpoints?.length ?? 0
            });
            setApiCatalog(endpointsResponse.data);
          } catch (catalogError) {
            console.warn('Failed to load API endpoint catalog', catalogError);
            setApiCatalog(null);
          } finally {
            setApiLoading(false);
          }

          try {
            setDbLoading(true);
            const dbResponse = await axios.get(`/project/${payload.projectId}/db-analysis`, {
              headers: {
                ...authHeaders()
              }
            });
            console.info('Loaded database analysis', {
              projectId: payload.projectId,
              entityCount: dbResponse.data?.dbAnalysis?.entities?.length ?? 0
            });
            setDbAnalysis(dbResponse.data?.dbAnalysis ?? null);
          } catch (dbError) {
            console.warn('Failed to load database analysis', dbError);
            setDbAnalysis(null);
          } finally {
            setDbLoading(false);
          }
        } catch (fetchError) {
          console.warn('Failed to load project overview', fetchError);
          setError(fetchError.response?.data || 'Analysis completed, but the overview failed to load.');
        }
      }
    } catch (err) {
      console.error('Repository analysis failed', err);
      setError(err.response?.data || 'Failed to analyze repository');
    } finally {
      console.debug('Analysis request finalized');
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
          <div className="tab-bar">
            <button
              type="button"
              className={`tab-button ${activeTab === 'overview' ? 'active' : ''}`}
              onClick={() => setActiveTab('overview')}
            >
              Overview
            </button>
            <button
              type="button"
              className={`tab-button ${activeTab === 'api' ? 'active' : ''}`}
              onClick={() => setActiveTab('api')}
              disabled={!overview && !apiCatalog}
            >
              API Specs
            </button>
            <button
              type="button"
              className={`tab-button ${activeTab === 'db' ? 'active' : ''}`}
              onClick={() => setActiveTab('db')}
              disabled={!overview && !dbAnalysis && !dbLoading}
            >
              Database
            </button>
          </div>
          {activeTab === 'overview' ? (
            <OverviewPanel overview={overview} loading={loading && !overview} />
          ) : activeTab === 'api' ? (
            <ApiSpecsPanel overview={overview} apiCatalog={apiCatalog} loading={apiLoading && !apiCatalog} />
          ) : (
            <DatabasePanel analysis={dbAnalysis} loading={dbLoading && !dbAnalysis} />
          )}
        </section>
      </div>
    </div>
  );
}

export default App;
