import { formatDate } from '../../utils/formatters';

const MetadataPanel = ({ metadata, loading }) => {
  if (loading) {
    return (
      <div className="overview-content">
        <h2>Metadata &amp; Specs</h2>
        <p className="overview-hint">Collecting metadata artifacts…</p>
      </div>
    );
  }

  if (!metadata) {
    return (
      <div className="overview-content">
        <h2>Metadata &amp; Specs</h2>
        <p className="overview-hint">Switch to this tab after running an analysis to view OpenAPI/WSDL/XSD captures.</p>
      </div>
    );
  }

  const dump = metadata.metadataDump || {};
  const openApiSpecs = dump.openApiSpecs || [];
  const wsdlDocs = dump.wsdlDocuments || [];
  const xsdDocs = dump.xsdDocuments || [];
  const soapServices = dump.soapServices || [];

  return (
    <div className="overview-content">
      <h2>Metadata &amp; Specs</h2>
      <p className="overview-hint">
        Download the snapshot JSON and feed it to GitLab Duo, ChatGPT, or other copilots for deeper reasoning. Specs listed here
        mirror the raw artifacts captured on disk.
      </p>
      <dl className="metadata-summary">
        <div>
          <dt>Project</dt>
          <dd>{metadata.projectName || '—'}</dd>
        </div>
        <div>
          <dt>Analyzed</dt>
          <dd>{formatDate(metadata.analyzedAt)}</dd>
        </div>
        <div>
          <dt>Snapshot API</dt>
          <dd>
            <code>{metadata.snapshotDownloadUrl || `/project/${metadata.projectId}/export/snapshot`}</code>
          </dd>
        </div>
      </dl>

      <section className="api-section">
        <h3>OpenAPI Specs ({openApiSpecs.length})</h3>
        {openApiSpecs.length === 0 ? (
          <p className="overview-hint">No OpenAPI files were captured.</p>
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
        <h3>WSDL Documents ({wsdlDocs.length})</h3>
        {wsdlDocs.length === 0 ? (
          <p className="overview-hint">No WSDL files detected.</p>
        ) : (
          wsdlDocs.map((doc) => (
            <details key={`wsdl-${doc.fileName}`} className="spec-doc" open={false}>
              <summary>{doc.fileName}</summary>
              <pre>{doc.content}</pre>
            </details>
          ))
        )}
      </section>

      <section className="api-section">
        <h3>XSD Documents ({xsdDocs.length})</h3>
        {xsdDocs.length === 0 ? (
          <p className="overview-hint">No XSD files detected.</p>
        ) : (
          xsdDocs.map((doc) => (
            <details key={`xsd-${doc.fileName}`} className="spec-doc" open={false}>
              <summary>{doc.fileName}</summary>
              <pre>{doc.content}</pre>
            </details>
          ))
        )}
      </section>

      {soapServices.length > 0 && (
        <section className="api-section">
          <h3>SOAP Services</h3>
          <div className="soap-summary">
            {soapServices.map((service) => (
              <div key={`${service.fileName}-${service.serviceName}`} className="soap-service">
                <strong>{service.serviceName}</strong>
                <span className="overview-hint">{service.fileName}</span>
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
        </section>
      )}
    </div>
  );
};

export default MetadataPanel;
