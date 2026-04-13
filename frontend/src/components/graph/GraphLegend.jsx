import { NODE_TYPE_CONFIG } from '../../constants/nodeTypes';

const LEGEND_ITEMS = Object.entries(NODE_TYPE_CONFIG).map(([type, config]) => ({
  type,
  color: config.color,
  label: config.label,
}));

/**
 * Color legend for node types, driven from the shared NODE_TYPE_CONFIG.
 */
export default function GraphLegend() {
  return (
    <div style={{
      position: 'absolute',
      left: '16px',
      bottom: '16px',
      background: '#fff',
      border: '1px solid #e0e0e0',
      borderRadius: '8px',
      padding: '8px 12px',
      boxShadow: '0 2px 4px rgba(0,0,0,0.05)',
      zIndex: 10,
      maxHeight: '300px',
      overflowY: 'auto',
    }}>
      {LEGEND_ITEMS.map(({ type, color, label }) => (
        <div key={type} style={{ display: 'flex', alignItems: 'center', gap: '6px', fontSize: '12px', padding: '1px 0' }}>
          <span style={{ width: '10px', height: '10px', borderRadius: '50%', background: color, display: 'inline-block', flexShrink: 0 }} />
          {label}
        </div>
      ))}
    </div>
  );
}
