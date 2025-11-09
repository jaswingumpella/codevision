import React from 'react';
import { formatDate } from '../../utils/formatters';

const DiffSection = ({ title, additions = [], removals = [], renderItem }) => (
  <div className="snapshot-diff-section">
    <h4>{title}</h4>
    <div className="snapshot-diff-grid">
      <div>
        <strong>Added</strong>
        {additions.length === 0 ? (
          <p className="overview-hint">No additions</p>
        ) : (
          <ul>
            {additions.map((item, index) => (
              <li key={`${title}-add-${index}`}>{renderItem(item)}</li>
            ))}
          </ul>
        )}
      </div>
      <div>
        <strong>Removed</strong>
        {removals.length === 0 ? (
          <p className="overview-hint">No removals</p>
        ) : (
          <ul>
            {removals.map((item, index) => (
              <li key={`${title}-remove-${index}`}>{renderItem(item)}</li>
            ))}
          </ul>
        )}
      </div>
    </div>
  </div>
);

const SnapshotsPanel = ({
  snapshots,
  loading,
  error,
  onRefresh,
  selectedBase,
  selectedCompare,
  onSelectBase,
  onSelectCompare,
  onDiff,
  diff
}) => {
  const hasSnapshots = Array.isArray(snapshots) && snapshots.length > 0;
  const compareDisabled = !selectedBase || snapshots.length < 2;

  return (
    <div className="overview-content">
      <div className="panel-header">
        <div>
          <h2>Snapshot History</h2>
          <p className="overview-hint">Track how the project evolves between analyses.</p>
        </div>
        <button type="button" className="ghost-button" onClick={onRefresh} disabled={loading}>
          Refresh
        </button>
      </div>

      {error && <p className="error-text">{error}</p>}

      {loading ? (
        <p className="overview-hint">Loading snapshots…</p>
      ) : !hasSnapshots ? (
        <p className="overview-hint">Run at least one analysis to capture the first snapshot.</p>
      ) : (
        <div className="snapshot-controls">
          <label>
            Baseline
            <select
              value={selectedBase || ''}
              onChange={(event) => onSelectBase && onSelectBase(Number(event.target.value) || null)}
            >
              {snapshots.map((snapshot) => (
                <option key={snapshot.snapshotId} value={snapshot.snapshotId}>
                  {formatDate(snapshot.createdAt)} · {snapshot.commitHash || 'unspecified'}
                </option>
              ))}
            </select>
          </label>

          <label>
            Compare to
            <select
              value={selectedCompare || ''}
              onChange={(event) => onSelectCompare && onSelectCompare(Number(event.target.value) || null)}
              disabled={compareDisabled}
            >
              <option value="">Select snapshot</option>
              {snapshots
                .filter((snapshot) => snapshot.snapshotId !== selectedBase)
                .map((snapshot) => (
                  <option key={snapshot.snapshotId} value={snapshot.snapshotId}>
                    {formatDate(snapshot.createdAt)} · {snapshot.commitHash || 'unspecified'}
                  </option>
                ))}
            </select>
          </label>

          <button
            type="button"
            onClick={() => onDiff && selectedBase && selectedCompare && onDiff(selectedBase, selectedCompare)}
            disabled={!selectedBase || !selectedCompare || selectedBase === selectedCompare}
          >
            Show Diff
          </button>
        </div>
      )}

      {diff && (
        <div className="snapshot-diff-results">
          <p className="overview-hint">
            Comparing snapshot {diff.baseSnapshotId} (commit {diff.baseCommitHash || 'n/a'}) to snapshot
            {' '} {diff.compareSnapshotId} (commit {diff.compareCommitHash || 'n/a'}).
          </p>
          <DiffSection
            title="Classes"
            additions={diff.addedClasses || []}
            removals={diff.removedClasses || []}
            renderItem={(item) => `${item.fullyQualifiedName || 'Unknown'}${item.stereotype ? ` · ${item.stereotype}` : ''}`}
          />
          <DiffSection
            title="Endpoints"
            additions={diff.addedEndpoints || []}
            removals={diff.removedEndpoints || []}
            renderItem={(item) => `${item.protocol || ''} ${item.httpMethod || ''} ${item.pathOrOperation || ''}`}
          />
          <DiffSection
            title="Database Entities"
            additions={diff.addedEntities || []}
            removals={diff.removedEntities || []}
            renderItem={(item) => `${item.entityName || 'Entity'}${item.tableName ? ` → ${item.tableName}` : ''}`}
          />
        </div>
      )}

      {hasSnapshots && (
        <table className="api-table snapshot-table">
          <thead>
            <tr>
              <th>Snapshot</th>
              <th>Branch</th>
              <th>Commit</th>
              <th>Captured</th>
            </tr>
          </thead>
          <tbody>
            {snapshots.map((snapshot) => (
              <tr key={snapshot.snapshotId}>
                <td>{snapshot.snapshotId}</td>
                <td>{snapshot.branchName || 'main'}</td>
                <td>{snapshot.commitHash || 'n/a'}</td>
                <td>{formatDate(snapshot.createdAt)}</td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </div>
  );
};

export default SnapshotsPanel;
