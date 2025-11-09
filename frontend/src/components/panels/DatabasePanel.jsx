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

export default DatabasePanel;
