/**
 * Toolbar for graph interactions: search, zoom, layout, export.
 */
export default function GraphToolbar({ searchQuery, onSearchChange, stats }) {
  return (
    <div style={{
      display: 'flex',
      alignItems: 'center',
      gap: '12px',
      padding: '8px 16px',
      background: '#fff',
      borderBottom: '1px solid #e0e0e0',
    }}>
      <input
        type="text"
        placeholder="Search nodes..."
        value={searchQuery}
        onChange={(e) => onSearchChange(e.target.value)}
        style={{
          padding: '6px 12px',
          border: '1px solid #ddd',
          borderRadius: '4px',
          fontSize: '14px',
          width: '250px',
        }}
      />
      {stats && (
        <span style={{ fontSize: '13px', color: '#666' }}>
          {stats.nodes} nodes, {stats.edges} edges
        </span>
      )}
    </div>
  );
}
