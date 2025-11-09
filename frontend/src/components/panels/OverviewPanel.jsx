import ClassDirectory from '../search/ClassDirectory';
import { formatDate } from '../../utils/formatters';

const OverviewPanel = ({ overview, loading, searchQuery }) => {
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
  const metadataDump = overview.metadataDump ?? {};
  const openApiSpecs = metadataDump.openApiSpecs ?? [];
  const wsdlDocs = metadataDump.wsdlDocuments ?? [];
  const xsdDocs = metadataDump.xsdDocuments ?? [];
  const soapServices = metadataDump.soapServices ?? [];
  const metadataHighlights = [
    {
      label: 'Artifact',
      value: overview.buildInfo?.artifactId || overview.projectName || '—',
      hint: overview.buildInfo?.groupId ? `${overview.buildInfo.groupId}:${overview.buildInfo.artifactId}` : '—'
    },
    {
      label: 'Build Version',
      value: overview.buildInfo?.version || '—',
      hint: overview.buildInfo?.javaVersion ? `Java ${overview.buildInfo.javaVersion}` : 'Java version not detected'
    },
    {
      label: 'OpenAPI Specs',
      value: openApiSpecs.length,
      hint: openApiSpecs.length > 0 ? 'Docs captured during scan' : 'No OpenAPI files detected'
    },
    {
      label: 'SOAP Services',
      value: soapServices.length,
      hint: wsdlDocs.length || xsdDocs.length ? `${wsdlDocs.length} WSDL • ${xsdDocs.length} XSD` : 'No SOAP metadata detected'
    }
  ];

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
        <h3>Metadata Highlights</h3>
        <div className="stat-grid metadata-highlight-grid">
          {metadataHighlights.map((item) => (
            <div key={item.label} className="stat metadata-highlight">
              <span className="stat-label">{item.label}</span>
              <span className="stat-value">{item.value}</span>
              <span className="stat-hint">{item.hint}</span>
            </div>
          ))}
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
          <div className="empty-state">
            <p className="overview-hint">No OpenAPI definitions detected.</p>
            <p className="overview-hint">
              Add Swagger annotations or commit an <code>openapi.yaml</code>/<code>swagger.json</code> so we can render specs.
            </p>
          </div>
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

      <section className="overview-section">
        <header className="section-heading">
          <h3>Class Directory</h3>
          <p className="overview-hint">
            {searchQuery
              ? `Filtering results for "${searchQuery}".`
              : 'Browse the captured classes or start a global search to filter this list.'}
          </p>
        </header>
        <ClassDirectory classes={classList} searchQuery={searchQuery} />
      </section>
    </div>
  );
};

export default OverviewPanel;
