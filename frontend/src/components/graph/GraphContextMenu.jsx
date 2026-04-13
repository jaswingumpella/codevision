import { useEffect, useRef } from 'react';

const MENU_ACTIONS = [
  { type: 'focus-subgraph', label: 'Focus Subgraph' },
  { type: 'show-impact', label: 'Show Impact' },
  { type: 'copy-name', label: 'Copy Qualified Name' },
  { type: 'show-detail', label: 'Show in Detail Panel' },
  { type: 'highlight-neighbors', label: 'Highlight Neighbors' },
];

/**
 * Context menu displayed on right-click of a graph node.
 * Positioned at the click coordinates (x, y).
 * Closes when clicking outside.
 */
export default function GraphContextMenu({ x, y, nodeId, nodeAttrs, onClose, onAction }) {
  const menuRef = useRef(null);

  useEffect(() => {
    function handleClickOutside(e) {
      if (menuRef.current && !menuRef.current.contains(e.target)) {
        onClose();
      }
    }
    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, [onClose]);

  if (!nodeId) return null;

  return (
    <div
      ref={menuRef}
      style={{
        position: 'absolute',
        left: `${x}px`,
        top: `${y}px`,
        zIndex: 25,
        background: '#fff',
        border: '1px solid #e0e0e0',
        borderRadius: '6px',
        boxShadow: '0 4px 12px rgba(0,0,0,0.15)',
        minWidth: '200px',
        padding: '4px 0',
      }}
    >
      <div style={{ padding: '6px 12px', fontSize: '12px', color: '#888', borderBottom: '1px solid #eee' }}>
        {nodeAttrs?.label || nodeId}
      </div>
      {MENU_ACTIONS.map(({ type, label }) => (
        <button
          key={type}
          onClick={() => {
            onAction(type, nodeId, nodeAttrs);
            onClose();
          }}
          style={{
            display: 'block',
            width: '100%',
            padding: '8px 12px',
            fontSize: '13px',
            border: 'none',
            background: 'none',
            textAlign: 'left',
            cursor: 'pointer',
            color: '#333',
          }}
          onMouseEnter={(e) => { e.currentTarget.style.background = '#f0f4ff'; }}
          onMouseLeave={(e) => { e.currentTarget.style.background = 'none'; }}
        >
          {label}
        </button>
      ))}
    </div>
  );
}
