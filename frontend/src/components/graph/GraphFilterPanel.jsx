/**
 * Panel for filtering graph nodes by type.
 */
export default function GraphFilterPanel({ nodeTypes, activeTypes, onToggleType }) {
  if (!nodeTypes || nodeTypes.length === 0) return null;

  return (
    <div style={{
      position: 'absolute',
      left: '16px',
      top: '60px',
      width: '200px',
      background: '#fff',
      border: '1px solid #e0e0e0',
      borderRadius: '8px',
      padding: '12px',
      boxShadow: '0 2px 8px rgba(0,0,0,0.1)',
      zIndex: 10,
      maxHeight: '400px',
      overflowY: 'auto',
    }}>
      <strong style={{ fontSize: '13px', marginBottom: '8px', display: 'block' }}>Node Types</strong>
      {nodeTypes.map((type) => (
        <label key={type} style={{ display: 'flex', alignItems: 'center', gap: '6px', fontSize: '12px', padding: '2px 0' }}>
          <input
            type="checkbox"
            checked={activeTypes.size === 0 || activeTypes.has(type)}
            onChange={() => onToggleType(type)}
          />
          {type}
        </label>
      ))}
    </div>
  );
}
