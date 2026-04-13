import { useRef, useEffect, useState, useCallback } from 'react';

/**
 * Computes padded bounding box of all graph nodes.
 * Returns null if graph has no nodes.
 */
function computeGraphBounds(graph) {
  let minX = Infinity, maxX = -Infinity, minY = Infinity, maxY = -Infinity;
  graph.forEachNode((_, attrs) => {
    if (attrs.x < minX) minX = attrs.x;
    if (attrs.x > maxX) maxX = attrs.x;
    if (attrs.y < minY) minY = attrs.y;
    if (attrs.y > maxY) maxY = attrs.y;
  });

  if (minX === Infinity) return null;

  const padX = (maxX - minX) * 0.1 || 1;
  const padY = (maxY - minY) * 0.1 || 1;
  return { minX: minX - padX, maxX: maxX + padX, minY: minY - padY, maxY: maxY + padY };
}

/**
 * Minimap component showing a bird's eye view of the full graph.
 * Displays a viewport rectangle showing the current visible area.
 * Supports click-to-navigate.
 */
export default function GraphMinimap({ graph, sigma, visible = true, width = 180, height = 120 }) {
  const canvasRef = useRef(null);
  const [hovering, setHovering] = useState(false);

  // Draw minimap whenever graph or camera changes
  useEffect(() => {
    if (!visible || !graph || !sigma || !canvasRef.current) return;

    const canvas = canvasRef.current;
    const ctx = canvas.getContext('2d');
    if (!ctx) return;
    const camera = sigma.getCamera();

    const draw = () => {
      const w = canvas.width;
      const h = canvas.height;
      ctx.clearRect(0, 0, w, h);

      ctx.fillStyle = '#f8f9fa';
      ctx.fillRect(0, 0, w, h);

      const bounds = computeGraphBounds(graph);
      if (!bounds) return;

      const { minX, maxX, minY, maxY } = bounds;
      const scaleX = w / (maxX - minX);
      const scaleY = h / (maxY - minY);
      const scale = Math.min(scaleX, scaleY);

      graph.forEachNode((_, attrs) => {
        const x = (attrs.x - minX) * scale;
        const y = (attrs.y - minY) * scale;
        ctx.fillStyle = attrs.color || '#999';
        ctx.beginPath();
        ctx.arc(x, y, 2, 0, Math.PI * 2);
        ctx.fill();
      });

      // Draw viewport rectangle
      const state = camera.getState();
      const viewportW = w / state.ratio;
      const viewportH = h / state.ratio;
      const vpX = w / 2 - state.x * scale / state.ratio - viewportW / 2;
      const vpY = h / 2 - state.y * scale / state.ratio - viewportH / 2;

      ctx.strokeStyle = '#3498db';
      ctx.lineWidth = 2;
      ctx.strokeRect(vpX, vpY, viewportW, viewportH);
    };

    draw();

    const handler = () => draw();
    camera.on('updated', handler);
    return () => camera.removeListener('updated', handler);
  }, [graph, sigma, visible]);

  const handleClick = useCallback((e) => {
    if (!sigma || !graph) return;
    const canvas = canvasRef.current;
    const rect = canvas.getBoundingClientRect();
    const x = e.clientX - rect.left;
    const y = e.clientY - rect.top;

    const bounds = computeGraphBounds(graph);
    if (!bounds) return;

    const { minX, maxX, minY, maxY } = bounds;
    const graphX = minX + (x / canvas.width) * (maxX - minX);
    const graphY = minY + (y / canvas.height) * (maxY - minY);

    sigma.getCamera().animate({ x: graphX, y: graphY }, { duration: 300 });
  }, [sigma, graph]);

  if (!visible || !graph || !sigma) return null;

  return (
    <div
      style={{
        position: 'absolute',
        bottom: 16,
        right: 16,
        width,
        height,
        border: '1px solid #ddd',
        borderRadius: 8,
        overflow: 'hidden',
        background: '#fff',
        boxShadow: '0 2px 8px rgba(0,0,0,0.15)',
        opacity: hovering ? 1 : 0.8,
        transition: 'opacity 0.2s',
        zIndex: 10,
        cursor: 'crosshair',
      }}
      onMouseEnter={() => setHovering(true)}
      onMouseLeave={() => setHovering(false)}
    >
      <canvas
        ref={canvasRef}
        width={width}
        height={height}
        onClick={handleClick}
        style={{ width: '100%', height: '100%' }}
      />
    </div>
  );
}
