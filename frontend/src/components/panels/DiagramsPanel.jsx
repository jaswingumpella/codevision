import { useEffect, useState } from 'react';

const DIAGRAM_TYPE_LABELS = {
  CLASS: 'Class diagrams',
  COMPONENT: 'Component',
  USE_CASE: 'Use case',
  ERD: 'Entity relationship',
  DB_SCHEMA: 'DB schema',
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
  onOpenSvg,
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
                    <span className="badge badge-info">external deps</span>
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
            <button
              type="button"
              className="ghost-button"
              onClick={() =>
                setSourceVisibility((prev) => ({
                  ...prev,
                  plantuml: !prev.plantuml
                }))
              }
            >
              {sourceVisibility.plantuml ? 'Hide PlantUML' : 'View PlantUML'}
            </button>
            <button
              type="button"
              className="ghost-button"
              onClick={() =>
                setSourceVisibility((prev) => ({
                  ...prev,
                  mermaid: !prev.mermaid
                }))
              }
            >
              {sourceVisibility.mermaid ? 'Hide Mermaid' : 'View Mermaid'}
            </button>
            <button type="button" onClick={() => onDownloadSvg(activeDiagram)} disabled={!activeDiagram.svgAvailable}>
              Download SVG
            </button>
            <button
              type="button"
              className="ghost-button"
              onClick={() => onOpenSvg(activeDiagram)}
              disabled={!activeDiagram.svgAvailable || !activeDiagram.svgDownloadUrl}
            >
              Open in new tab
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
            Show external dependencies
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

export default DiagramsPanel;
