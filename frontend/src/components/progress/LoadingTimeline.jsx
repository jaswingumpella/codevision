const STEP_STATUS_COPY = {
  pending: 'Queued',
  active: 'In progress…',
  complete: 'Done',
  error: 'Skipped'
};

const LoadingTimeline = ({ steps }) => {
  if (!steps || steps.length === 0) {
    return null;
  }

  const completedSteps = steps.filter((step) => step.status === 'complete' || step.status === 'error').length;
  const progressValue = Math.round((completedSteps / steps.length) * 100);

  return (
    <div className="loading-timeline" aria-live="polite">
      <p className="timeline-title">Analysis progress</p>
      <div
        className="progress-track"
        role="progressbar"
        aria-valuenow={progressValue}
        aria-valuemin="0"
        aria-valuemax="100"
        aria-label="Analysis completion percentage"
      >
        <span className="progress-fill" style={{ width: `${progressValue}%` }} />
      </div>
      <p className="progress-caption">{progressValue}% complete</p>
      <ul className="loading-steps">
        {steps.map((step) => (
          <li key={step.id} className="loading-step">
            <span className={`step-indicator status-${step.status}`} aria-hidden="true">
              {step.status === 'complete' ? '✓' : step.status === 'error' ? '!' : ''}
            </span>
            <div>
              <p className="step-label">{step.label}</p>
              <p className="step-status">{STEP_STATUS_COPY[step.status] || '—'}</p>
            </div>
          </li>
        ))}
      </ul>
    </div>
  );
};

export default LoadingTimeline;
