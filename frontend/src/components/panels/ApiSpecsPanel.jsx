import { useEffect, useMemo, useState } from 'react';
import { PAGE_SIZE } from '../../utils/constants';
import { textMatches } from '../../utils/formatters';

const ApiSpecsPanel = ({ overview, apiCatalog, loading, searchQuery }) => {
  const [pages, setPages] = useState({ REST: 0, SOAP: 0, LEGACY: 0 });

  useEffect(() => {
    setPages({ REST: 0, SOAP: 0, LEGACY: 0 });
  }, [apiCatalog, overview, searchQuery]);

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
  const normalizedSearch = (searchQuery || '').trim().toLowerCase();
  const filteredEndpoints = useMemo(() => {
    if (!normalizedSearch) {
      return endpoints;
    }
    return endpoints.filter((endpoint) => {
      const specs = (endpoint.specArtifacts || []).map((artifact) => `${artifact.type} ${artifact.name}`);
      return textMatches(
        normalizedSearch,
        endpoint.httpMethod,
        endpoint.pathOrOperation,
        endpoint.controllerClass,
        endpoint.controllerMethod,
        ...specs
      );
    });
  }, [endpoints, normalizedSearch]);

  const grouped = useMemo(() => {
    const buckets = {
      REST: [],
      SOAP: [],
      LEGACY: []
    };
    filteredEndpoints.forEach((endpoint) => {
      if ((endpoint.protocol || '').toUpperCase() === 'REST') {
        buckets.REST.push(endpoint);
      } else if ((endpoint.protocol || '').toUpperCase() === 'SOAP') {
        buckets.SOAP.push(endpoint);
      } else {
        buckets.LEGACY.push(endpoint);
      }
    });
    return buckets;
  }, [filteredEndpoints]);

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
      {renderEndpoints(
        searchQuery ? `REST Endpoints matching "${searchQuery}"` : 'REST Endpoints',
        'REST',
        grouped.REST
      )}
      {renderEndpoints(
        searchQuery ? `SOAP Endpoints matching "${searchQuery}"` : 'SOAP Endpoints',
        'SOAP',
        grouped.SOAP
      )}
      {renderEndpoints(
        searchQuery ? `Legacy Endpoints matching "${searchQuery}"` : 'Legacy Endpoints',
        'LEGACY',
        grouped.LEGACY
      )}

      <section className="api-section">
        <h3>OpenAPI Specifications</h3>
        {openApiSpecs.length === 0 ? (
          <div className="empty-state">
            <p className="overview-hint">No OpenAPI specs found for this project.</p>
            <p className="overview-hint">
              Push an <code>openapi</code> file or annotate controllers with Swagger so downstream docs can be generated.
            </p>
          </div>
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

export default ApiSpecsPanel;
