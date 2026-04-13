import { useState, useCallback } from 'react';

/**
 * Timeline slider showing analysis snapshots over time.
 * Allows selecting a point in time to view the graph state.
 */
export default function GraphTimeline({ snapshots = [], currentIndex = 0, onSelect, visible = true }) {
  const [hoveredIndex, setHoveredIndex] = useState(null);

  const handleSelect = useCallback((index) => {
    if (onSelect) onSelect(index);
  }, [onSelect]);

  if (!visible || snapshots.length === 0) return null;

  return (
    <div style={{
      position: 'absolute',
      bottom: 0,
      left: 0,
      right: 0,
      height: 48,
      background: 'linear-gradient(to top, rgba(255,255,255,0.95), rgba(255,255,255,0.8))',
      borderTop: '1px solid #e0e0e0',
      display: 'flex',
      alignItems: 'center',
      padding: '0 24px',
      zIndex: 10,
    }}>
      <span style={{ fontSize: 12, color: '#666', marginRight: 12, whiteSpace: 'nowrap' }}>
        Timeline
      </span>
      <div style={{ flex: 1, position: 'relative', height: 24, display: 'flex', alignItems: 'center' }}>
        <div style={{ position: 'absolute', top: '50%', left: 0, right: 0, height: 2, background: '#ddd', transform: 'translateY(-50%)' }} />
        {snapshots.map((snapshot, i) => (
          <div
            key={i}
            style={{
              position: 'absolute',
              left: `${(i / Math.max(snapshots.length - 1, 1)) * 100}%`,
              transform: 'translateX(-50%)',
              cursor: 'pointer',
            }}
            onClick={() => handleSelect(i)}
            onMouseEnter={() => setHoveredIndex(i)}
            onMouseLeave={() => setHoveredIndex(null)}
            title={snapshot.label || `Snapshot ${i + 1}`}
          >
            <div style={{
              width: i === currentIndex ? 14 : 10,
              height: i === currentIndex ? 14 : 10,
              borderRadius: '50%',
              background: i === currentIndex ? '#3498db' : '#bbb',
              border: i === currentIndex ? '2px solid #2980b9' : '2px solid #999',
              transition: 'all 0.2s',
              transform: hoveredIndex === i ? 'scale(1.3)' : 'scale(1)',
            }} />
            {hoveredIndex === i && (
              <div style={{
                position: 'absolute',
                bottom: 20,
                left: '50%',
                transform: 'translateX(-50%)',
                background: '#333',
                color: '#fff',
                padding: '2px 8px',
                borderRadius: 4,
                fontSize: 11,
                whiteSpace: 'nowrap',
              }}>
                {snapshot.label || `Snapshot ${i + 1}`}
              </div>
            )}
          </div>
        ))}
      </div>
      <span style={{ fontSize: 11, color: '#888', marginLeft: 12, whiteSpace: 'nowrap' }}>
        {currentIndex + 1} / {snapshots.length}
      </span>
    </div>
  );
}
