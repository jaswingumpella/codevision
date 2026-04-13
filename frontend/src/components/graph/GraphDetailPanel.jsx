/**
 * Panel showing details of the selected node.
 */
export default function GraphDetailPanel({ nodeId, nodeAttrs, onClose }) {
  if (!nodeId) return null;

  return (
    <div style={{
      position: 'absolute',
      right: '16px',
      top: '60px',
      width: '300px',
      background: '#fff',
      border: '1px solid #e0e0e0',
      borderRadius: '8px',
      padding: '16px',
      boxShadow: '0 2px 8px rgba(0,0,0,0.1)',
      zIndex: 10,
    }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '12px' }}>
        <strong>{nodeAttrs?.label || nodeId}</strong>
        <button onClick={onClose} style={{ border: 'none', background: 'none', cursor: 'pointer', fontSize: '16px' }}>
          x
        </button>
      </div>
      <div style={{ fontSize: '13px', color: '#666' }}>
        <p><strong>Type:</strong> {nodeAttrs?.type || 'Unknown'}</p>
        <p><strong>ID:</strong> {nodeId}</p>
        {nodeAttrs?.qualifiedName && (
          <p><strong>Qualified Name:</strong> {nodeAttrs.qualifiedName}</p>
        )}
      </div>
    </div>
  );
}
