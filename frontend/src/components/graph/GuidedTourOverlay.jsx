/**
 * Guided tour overlay that walks users through notable graph nodes step-by-step.
 * Positioned at top-center of the graph area as a floating card.
 */
export default function GuidedTourOverlay({ steps, currentStep, onNext, onPrev, onClose }) {
  if (!steps || steps.length === 0) return null;
  if (currentStep < 0 || currentStep >= steps.length) return null;

  const step = steps[currentStep];
  const isFirst = currentStep === 0;
  const isLast = currentStep === steps.length - 1;

  return (
    <div style={{
      position: 'absolute',
      top: '16px',
      left: '50%',
      transform: 'translateX(-50%)',
      zIndex: 20,
      background: '#fff',
      border: '1px solid #e0e0e0',
      borderRadius: '8px',
      padding: '16px 20px',
      boxShadow: '0 4px 12px rgba(0,0,0,0.15)',
      minWidth: '320px',
      maxWidth: '440px',
    }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '8px' }}>
        <span style={{ fontSize: '12px', color: '#888', fontWeight: 500 }}>
          Step {currentStep + 1} of {steps.length}
        </span>
        <button
          onClick={onClose}
          style={{
            border: 'none',
            background: 'none',
            cursor: 'pointer',
            fontSize: '16px',
            color: '#888',
            padding: '0 4px',
          }}
          aria-label="Close tour"
        >
          &times;
        </button>
      </div>

      <div style={{ marginBottom: '12px' }}>
        <strong style={{ fontSize: '15px', display: 'block', marginBottom: '4px' }}>
          {step.label}
        </strong>
        <p style={{ fontSize: '13px', color: '#555', margin: 0, lineHeight: 1.5 }}>
          {step.description}
        </p>
      </div>

      <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '8px' }}>
        <button
          onClick={onPrev}
          disabled={isFirst}
          style={{
            padding: '6px 14px',
            fontSize: '13px',
            border: '1px solid #ccc',
            borderRadius: '4px',
            background: isFirst ? '#f5f5f5' : '#fff',
            color: isFirst ? '#bbb' : '#333',
            cursor: isFirst ? 'default' : 'pointer',
          }}
        >
          Prev
        </button>
        <button
          onClick={isLast ? onClose : onNext}
          style={{
            padding: '6px 14px',
            fontSize: '13px',
            border: '1px solid #3b82f6',
            borderRadius: '4px',
            background: '#3b82f6',
            color: '#fff',
            cursor: 'pointer',
          }}
        >
          {isLast ? 'Finish' : 'Next'}
        </button>
      </div>
    </div>
  );
}
