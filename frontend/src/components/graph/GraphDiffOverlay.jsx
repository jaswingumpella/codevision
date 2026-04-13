import { useMemo } from 'react';

/**
 * Diff overlay that highlights changes between two graph states.
 * Added nodes are shown in green, removed in red, modified in yellow.
 */
export default function GraphDiffOverlay({ diffData, visible = true }) {
  const stats = useMemo(() => {
    if (!diffData) return { added: 0, removed: 0, modified: 0 };
    return {
      added: diffData.added?.length || 0,
      removed: diffData.removed?.length || 0,
      modified: diffData.modified?.length || 0,
    };
  }, [diffData]);

  if (!visible || !diffData) return null;

  return (
    <div style={{
      position: 'absolute',
      top: 8,
      right: 8,
      background: 'rgba(255,255,255,0.95)',
      border: '1px solid #ddd',
      borderRadius: 8,
      padding: '8px 12px',
      fontSize: 12,
      zIndex: 10,
      boxShadow: '0 2px 8px rgba(0,0,0,0.1)',
    }}>
      <div style={{ fontWeight: 600, marginBottom: 4 }}>Changes</div>
      {stats.added > 0 && (
        <div style={{ display: 'flex', alignItems: 'center', gap: 6, marginBottom: 2 }}>
          <span style={{ width: 8, height: 8, borderRadius: '50%', background: '#27ae60', display: 'inline-block' }} />
          <span>{stats.added} added</span>
        </div>
      )}
      {stats.removed > 0 && (
        <div style={{ display: 'flex', alignItems: 'center', gap: 6, marginBottom: 2 }}>
          <span style={{ width: 8, height: 8, borderRadius: '50%', background: '#e74c3c', display: 'inline-block' }} />
          <span>{stats.removed} removed</span>
        </div>
      )}
      {stats.modified > 0 && (
        <div style={{ display: 'flex', alignItems: 'center', gap: 6, marginBottom: 2 }}>
          <span style={{ width: 8, height: 8, borderRadius: '50%', background: '#f39c12', display: 'inline-block' }} />
          <span>{stats.modified} modified</span>
        </div>
      )}
      {stats.added === 0 && stats.removed === 0 && stats.modified === 0 && (
        <div style={{ color: '#888' }}>No changes</div>
      )}
    </div>
  );
}
