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

const LoggerInsightsPanel = ({ insights, loading, onDownloadCsv, onDownloadPdf }) => {
  const [classFilter, setClassFilter] = useState('');
  const [levelFilter, setLevelFilter] = useState('ALL');
  const [piiOnly, setPiiOnly] = useState(false);
  const [pciOnly, setPciOnly] = useState(false);
  const [expanded, setExpanded] = useState(false);

  if (loading) {
    return (
      <div className="overview-content">
        <h2>Logger Insights</h2>
        <p className="overview-hint">Scanning log statements…</p>
      </div>
    );
  }

  const normalizedFilter = classFilter.trim().toLowerCase();
  const filtered = (insights || []).filter((entry) => {
    const levelMatches = levelFilter === 'ALL' || (entry.logLevel || '').toUpperCase() === levelFilter;
    const classMatches = !normalizedFilter || (entry.className || '').toLowerCase().includes(normalizedFilter);
    const piiMatches = !piiOnly || entry.piiRisk;
    const pciMatches = !pciOnly || entry.pciRisk;
    return levelMatches && classMatches && piiMatches && pciMatches;
  });

  const renderMessage = (message) => {
    if (!message) {
      return '—';
    }
    if (expanded || message.length <= 140) {
      return message;
    }
    return `${message.slice(0, 140)}…`;
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
            placeholder="com.example.OrderService"
            value={classFilter}
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
      </div>

      <div className="export-actions">
        <button type="button" onClick={() => setExpanded(true)} className="ghost-button">
          Expand All
        </button>
        <button type="button" onClick={() => setExpanded(false)} className="ghost-button">
          Collapse All
        </button>
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

const PiiPciPanel = ({ findings, loading, onDownloadCsv, onDownloadPdf }) => {
  const [typeFilter, setTypeFilter] = useState('ALL');
  const [severityFilter, setSeverityFilter] = useState('ALL');
  const [hideIgnored, setHideIgnored] = useState(false);

  if (loading) {
    return (
      <div className="overview-content">
        <h2>PCI / PII Scan</h2>
        <p className="overview-hint">Collecting sensitive data findings…</p>
      </div>
    );
  }

  const filtered = (findings || []).filter((entry) => {
    const typeMatches = typeFilter === 'ALL' || (entry.matchType || '').toUpperCase() === typeFilter;
    const severityMatches = severityFilter === 'ALL' || (entry.severity || '').toUpperCase() === severityFilter;
    const ignoreMatches = !hideIgnored || !entry.ignored;
    return typeMatches && severityMatches && ignoreMatches;
  });

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
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </div>
  );
};

const DIAGRAM_TYPE_LABELS = {
  CLASS: 'Class',
  COMPONENT: 'Component',
  USE_CASE: 'Use Case',
  ERD: 'ERD',
  DB_SCHEMA: 'DB Schema',
  SEQUENCE: 'Sequence'
};

const DIAGRAM_TYPE_ORDER = ['CLASS', 'COMPONENT', 'USE_CASE', 'ERD', 'DB_SCHEMA', 'SEQUENCE'];

const DiagramsPanel = ({
  diagramsByType,
  loading,
  activeType,
  onTypeChange,
  activeDiagram,
  onSelectDiagram,
  svgContent,
  onDownloadSvg,
  sequenceIncludeExternal,
  onSequenceToggle
}) => {
  const [sourceVisibility, setSourceVisibility] = useState({ plantuml: false, mermaid: false });

  useEffect(() => {
    setSourceVisibility({ plantuml: false, mermaid: false });
  }, [activeDiagram]);

  const rawDiagrams = diagramsByType[activeType] || [];
  const sequencePool = diagramsByType.SEQUENCE || [];
  const hasSequenceInternal = sequencePool.some((diagram) => !Boolean(diagram.metadata?.includeExternal));
  const hasSequenceExternal = sequencePool.some((diagram) => Boolean(diagram.metadata?.includeExternal));
  const showSequenceToggle = activeType === 'SEQUENCE' && hasSequenceInternal && hasSequenceExternal;
  const currentDiagrams =
    activeType === 'SEQUENCE'
      ? rawDiagrams.filter((diagram) => Boolean(diagram.metadata?.includeExternal) === sequenceIncludeExternal)
      : rawDiagrams;

  const renderDiagramList = () => {
    if (loading && currentDiagrams.length === 0) {
      return <p className="overview-hint">Loading diagrams…</p>;
    }
    if (!loading && currentDiagrams.length === 0) {
      return <p className="overview-hint">No diagrams available for this category.</p>;
    }
    return (
      <ul className="diagram-list">
        {currentDiagrams.map((diagram) => {
          const isSelected = activeDiagram && diagram.diagramId === activeDiagram.diagramId;
          return (
            <li key={diagram.diagramId}>
              <button
                type="button"
                className={`diagram-card ${isSelected ? 'selected' : ''}`}
                onClick={() => onSelectDiagram(diagram.diagramId)}
              >
                <div className="diagram-card-title">
                  <strong>{diagram.title || 'Diagram'}</strong>
                  {diagram.metadata?.includeExternal ? (
                    <span className="badge badge-info">codeviz2</span>
                  ) : null}
                </div>
                <p className="diagram-card-hint">
                  {diagram.metadata?.pathOrOperation
                    ? `${diagram.metadata?.httpMethod ? `${diagram.metadata.httpMethod} ` : ''}${
                        diagram.metadata.pathOrOperation
                      } · ${diagram.svgAvailable ? 'SVG available' : 'SVG not stored'}`
                    : diagram.svgAvailable
                      ? 'SVG available'
                      : 'SVG not stored'}
                </p>
              </button>
            </li>
          );
        })}
      </ul>
    );
  };

  const renderDiagramViewer = () => {
    if (!activeDiagram) {
      return (
        <div className="diagram-viewer">
          <p className="overview-hint">
            {loading ? 'Pick a diagram once loading completes.' : 'Select a diagram to view its details.'}
          </p>
        </div>
      );
    }

    const activeSvg = svgContent[activeDiagram.diagramId];

    return (
      <div className="diagram-viewer">
        <div className="diagram-viewer-header">
          <div>
            <h3>{activeDiagram.title || DIAGRAM_TYPE_LABELS[activeType] || 'Diagram'}</h3>
            {activeDiagram.metadata?.pathOrOperation ? (
              <span className="overview-hint">
                {`${activeDiagram.metadata?.httpMethod ? `${activeDiagram.metadata.httpMethod} ` : ''}${
                  activeDiagram.metadata.pathOrOperation
                }`}
              </span>
            ) : null}
            {activeDiagram.svgAvailable ? (
              <span className="overview-hint">Rendered SVG available</span>
            ) : (
              <span className="overview-hint">SVG was not stored for this diagram</span>
            )}
          </div>
          <div className="diagram-actions">
            <button type="button" className="ghost-button" onClick={() => setSourceVisibility((prev) => ({
                  ...prev,
                  plantuml: !prev.plantuml
                }))}>
              {sourceVisibility.plantuml ? 'Hide PlantUML' : 'View PlantUML'}
            </button>
            <button type="button" className="ghost-button" onClick={() => setSourceVisibility((prev) => ({
                  ...prev,
                  mermaid: !prev.mermaid
                }))}>
              {sourceVisibility.mermaid ? 'Hide Mermaid' : 'View Mermaid'}
            </button>
            <button type="button" onClick={() => onDownloadSvg(activeDiagram)} disabled={!activeDiagram.svgAvailable}>
              Download SVG
            </button>
          </div>
        </div>
        <div className="diagram-svg">
          {activeDiagram.svgAvailable ? (
            activeSvg ? (
              <div dangerouslySetInnerHTML={{ __html: activeSvg }} />
            ) : (
              <p className="overview-hint">Rendering SVG…</p>
            )
          ) : (
            <p className="overview-hint">SVG rendering is unavailable for this diagram.</p>
          )}
        </div>
        {sourceVisibility.plantuml && (
          <details open className="diagram-source-block">
            <summary>PlantUML Source</summary>
            <pre className="diagram-source">{activeDiagram.plantumlSource || 'Not available'}</pre>
          </details>
        )}
        {sourceVisibility.mermaid && (
          <details open className="diagram-source-block">
            <summary>Mermaid Source</summary>
            <pre className="diagram-source">{activeDiagram.mermaidSource || 'Not available'}</pre>
          </details>
        )}
      </div>
    );
  };

  return (
    <div className="overview-content diagram-panel">
      <div className="diagram-type-tabs">
        {DIAGRAM_TYPE_ORDER.map((type) => {
          const hasDiagrams = (diagramsByType[type] || []).length > 0;
          return (
            <button
              key={type}
              type="button"
              className={`tab-button ${activeType === type ? 'active' : ''}`}
              onClick={() => hasDiagrams && onTypeChange(type)}
              disabled={!hasDiagrams && !loading}
            >
              {DIAGRAM_TYPE_LABELS[type] || type}
            </button>
          );
        })}
      </div>
      {showSequenceToggle ? (
        <div className="diagram-controls">
          <label className="toggle-group">
            <input
              type="checkbox"
              checked={sequenceIncludeExternal}
              onChange={(event) => onSequenceToggle(event.target.checked)}
            />
            Show codeviz2 externals
          </label>
        </div>
      ) : null}
      <div className="diagram-layout">
        <aside className="diagram-sidebar">{renderDiagramList()}</aside>
        {renderDiagramViewer()}
      </div>
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
  const [loggerInsights, setLoggerInsights] = useState([]);
  const [loggerLoading, setLoggerLoading] = useState(false);
  const [piiFindings, setPiiFindings] = useState([]);
  const [piiLoading, setPiiLoading] = useState(false);
  const [projectId, setProjectId] = useState(null);
  const [diagrams, setDiagrams] = useState([]);
  const [diagramLoading, setDiagramLoading] = useState(false);
  const [diagramSvgContent, setDiagramSvgContent] = useState({});
  const [activeDiagramType, setActiveDiagramType] = useState('CLASS');
  const [activeDiagramId, setActiveDiagramId] = useState(null);
  const [sequenceIncludeExternal, setSequenceIncludeExternal] = useState(false);

  const projectName = useMemo(() => deriveProjectName(repoUrl), [repoUrl]);
  const diagramsByType = useMemo(() => {
    const groups = {};
    diagrams.forEach((diagram) => {
      const normalizedType = (diagram.diagramType || 'CLASS').toUpperCase();
      if (!groups[normalizedType]) {
        groups[normalizedType] = [];
      }
      groups[normalizedType].push(diagram);
    });
    return groups;
  }, [diagrams]);

  const activeDiagram = useMemo(() => {
    if (!activeDiagramId) {
      return null;
    }
    return diagrams.find((diagram) => diagram.diagramId === activeDiagramId) || null;
  }, [diagrams, activeDiagramId]);

  const authHeaders = () => (apiKey ? { 'X-API-KEY': apiKey } : {});

  useEffect(() => {
    if (diagrams.length === 0) {
      setActiveDiagramId(null);
      return;
    }
    const availableTypes = Object.keys(diagramsByType);
    if (availableTypes.length === 0) {
      setActiveDiagramId(null);
      return;
    }
    if (!diagramsByType[activeDiagramType] || diagramsByType[activeDiagramType].length === 0) {
      const nextType = availableTypes[0];
      setActiveDiagramType(nextType);
      setActiveDiagramId(diagramsByType[nextType][0]?.diagramId ?? null);
      return;
    }
    if (
      diagramsByType[activeDiagramType] &&
      !diagramsByType[activeDiagramType].some((diagram) => diagram.diagramId === activeDiagramId)
    ) {
      setActiveDiagramId(diagramsByType[activeDiagramType][0]?.diagramId ?? null);
    }
  }, [diagrams, diagramsByType, activeDiagramType, activeDiagramId]);

  useEffect(() => {
    if (activeDiagramType !== 'SEQUENCE') {
      return;
    }
    const sequences = diagramsByType.SEQUENCE || [];
    if (sequences.length === 0) {
      return;
    }
    const matching = sequences.filter(
      (diagram) => Boolean(diagram.metadata?.includeExternal) === sequenceIncludeExternal
    );
    if (matching.length === 0) {
      const fallback = sequences[0];
      setSequenceIncludeExternal(Boolean(fallback.metadata?.includeExternal));
      setActiveDiagramId(fallback.diagramId ?? null);
      return;
    }
    if (!matching.some((diagram) => diagram.diagramId === activeDiagramId)) {
      setActiveDiagramId(matching[0]?.diagramId ?? null);
    }
  }, [activeDiagramType, diagramsByType, sequenceIncludeExternal, activeDiagramId]);

  useEffect(() => {
    if (!activeDiagram || !activeDiagram.svgAvailable || !activeDiagram.svgDownloadUrl) {
      return;
    }
    if (diagramSvgContent[activeDiagram.diagramId]) {
      return;
    }
    let cancelled = false;
    axios
      .get(activeDiagram.svgDownloadUrl, {
        headers: {
          ...authHeaders()
        },
        responseType: 'text'
      })
      .then((response) => {
        if (cancelled) {
          return;
        }
        const payload = typeof response.data === 'string' ? response.data : new TextDecoder().decode(response.data);
        setDiagramSvgContent((prev) => ({
          ...prev,
          [activeDiagram.diagramId]: payload
        }));
      })
      .catch((error) => {
        if (!cancelled) {
          console.warn('Failed to load diagram SVG', error);
        }
      });
    return () => {
      cancelled = true;
    };
  }, [activeDiagram, diagramSvgContent, apiKey]);

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
    setLoggerInsights([]);
    setLoggerLoading(false);
    setPiiFindings([]);
    setPiiLoading(false);
    setProjectId(null);
    setDiagrams([]);
    setDiagramSvgContent({});
    setDiagramLoading(false);
    setActiveDiagramId(null);
    setActiveDiagramType('CLASS');
    setSequenceIncludeExternal(false);

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
      setProjectId(payload?.projectId || null);

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

          try {
            setLoggerLoading(true);
            const loggerResponse = await axios.get(`/project/${payload.projectId}/logger-insights`, {
              headers: {
                ...authHeaders()
              }
            });
            console.info('Loaded logger insights', {
              projectId: payload.projectId,
              count: loggerResponse.data?.loggerInsights?.length ?? 0
            });
            setLoggerInsights(loggerResponse.data?.loggerInsights ?? []);
          } catch (loggerError) {
            console.warn('Failed to load logger insights', loggerError);
            setLoggerInsights([]);
          } finally {
            setLoggerLoading(false);
          }

          try {
            setPiiLoading(true);
            const piiResponse = await axios.get(`/project/${payload.projectId}/pii-pci`, {
              headers: {
                ...authHeaders()
              }
            });
            console.info('Loaded PCI / PII findings', {
              projectId: payload.projectId,
              count: piiResponse.data?.findings?.length ?? 0
            });
            setPiiFindings(piiResponse.data?.findings ?? []);
          } catch (piiError) {
            console.warn('Failed to load PCI / PII findings', piiError);
            setPiiFindings([]);
          } finally {
            setPiiLoading(false);
          }

          try {
            setDiagramLoading(true);
            const diagramsResponse = await axios.get(`/project/${payload.projectId}/diagrams`, {
              headers: {
                ...authHeaders()
              }
            });
            const diagramList = Array.isArray(diagramsResponse.data?.diagrams)
              ? diagramsResponse.data.diagrams
              : [];
            setDiagrams(diagramList);
            if (diagramList.length > 0) {
              const firstDiagram = diagramList[0];
              setActiveDiagramType((firstDiagram.diagramType || 'CLASS').toUpperCase());
              setActiveDiagramId(firstDiagram.diagramId ?? null);
              setSequenceIncludeExternal(Boolean(firstDiagram.metadata?.includeExternal));
            } else {
              setActiveDiagramId(null);
            }
          } catch (diagramError) {
            console.warn('Failed to load diagrams', diagramError);
            setDiagrams([]);
            setActiveDiagramId(null);
          } finally {
            setDiagramLoading(false);
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

  const handleExport = async (path, filename) => {
    if (!projectId) {
      return;
    }
    try {
      const response = await axios.get(path, {
        responseType: 'blob',
        headers: {
          ...authHeaders()
        }
      });
      const blob = new Blob([response.data]);
      const url = window.URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.setAttribute('download', filename);
      document.body.appendChild(link);
      link.click();
      link.remove();
      window.URL.revokeObjectURL(url);
    } catch (exportError) {
      console.warn(`Failed to download ${filename}`, exportError);
    }
  };

  const downloadLogsCsv = () =>
    projectId && handleExport(`/project/${projectId}/export/logs.csv`, `logger-insights-${projectId}.csv`);
  const downloadLogsPdf = () =>
    projectId && handleExport(`/project/${projectId}/export/logs.pdf`, `logger-insights-${projectId}.pdf`);
  const downloadPiiCsv = () =>
    projectId && handleExport(`/project/${projectId}/export/pii.csv`, `pii-findings-${projectId}.csv`);
  const downloadPiiPdf = () =>
    projectId && handleExport(`/project/${projectId}/export/pii.pdf`, `pii-findings-${projectId}.pdf`);

  const downloadDiagramSvg = (diagram) => {
    if (!diagram || !diagram.svgAvailable || !diagram.svgDownloadUrl) {
      return;
    }
    const safeType = (diagram.diagramType || 'diagram').toLowerCase();
    const fileName = `${safeType}-${diagram.diagramId || 'diagram'}.svg`;
    handleExport(diagram.svgDownloadUrl, fileName);
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
            <button
              type="button"
              className={`tab-button ${activeTab === 'logger' ? 'active' : ''}`}
              onClick={() => setActiveTab('logger')}
              disabled={!projectId && loggerInsights.length === 0 && !loggerLoading}
            >
              Logger Insights
            </button>
            <button
              type="button"
              className={`tab-button ${activeTab === 'pii' ? 'active' : ''}`}
              onClick={() => setActiveTab('pii')}
              disabled={!projectId && piiFindings.length === 0 && !piiLoading}
            >
              PCI / PII Scan
            </button>
            <button
              type="button"
              className={`tab-button ${activeTab === 'diagrams' ? 'active' : ''}`}
              onClick={() => setActiveTab('diagrams')}
              disabled={diagrams.length === 0 && !diagramLoading}
            >
              Diagrams
            </button>
          </div>
          {activeTab === 'overview' ? (
            <OverviewPanel overview={overview} loading={loading && !overview} />
          ) : activeTab === 'api' ? (
            <ApiSpecsPanel overview={overview} apiCatalog={apiCatalog} loading={apiLoading && !apiCatalog} />
          ) : activeTab === 'db' ? (
            <DatabasePanel analysis={dbAnalysis} loading={dbLoading && !dbAnalysis} />
          ) : activeTab === 'logger' ? (
            <LoggerInsightsPanel
              insights={loggerInsights}
              loading={loggerLoading && loggerInsights.length === 0}
              onDownloadCsv={downloadLogsCsv}
              onDownloadPdf={downloadLogsPdf}
            />
          ) : activeTab === 'pii' ? (
            <PiiPciPanel
              findings={piiFindings}
              loading={piiLoading && piiFindings.length === 0}
              onDownloadCsv={downloadPiiCsv}
              onDownloadPdf={downloadPiiPdf}
            />
          ) : (
            <DiagramsPanel
              diagramsByType={diagramsByType}
              loading={diagramLoading}
              activeType={activeDiagramType}
              onTypeChange={setActiveDiagramType}
              activeDiagram={activeDiagram}
              onSelectDiagram={setActiveDiagramId}
              svgContent={diagramSvgContent}
              onDownloadSvg={downloadDiagramSvg}
              sequenceIncludeExternal={sequenceIncludeExternal}
              onSequenceToggle={setSequenceIncludeExternal}
            />
          )}
        </section>
      </div>
    </div>
  );
}

export default App;
