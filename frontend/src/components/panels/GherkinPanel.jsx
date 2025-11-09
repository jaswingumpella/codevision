const GherkinPanel = ({ features, loading }) => {
  if (loading) {
    return (
      <div className="overview-content">
        <h2>Gherkin Features</h2>
        <p className="overview-hint">Loading scenarios…</p>
      </div>
    );
  }

  if (!features || features.length === 0) {
    return (
      <div className="overview-content">
        <h2>Gherkin Features</h2>
        <p className="overview-hint">No .feature files were detected during the last analysis.</p>
      </div>
    );
  }

  return (
    <div className="overview-content">
      <h2>Gherkin Features</h2>
      <p className="overview-hint">
        These scenarios come straight from your repository&apos;s <code>.feature</code> files. Expand a feature to review the captured
        steps and share them with QA or AI copilots.
      </p>
      <div className="gherkin-grid">
        {features.map((feature) => (
          <details key={`${feature.featureTitle}-${feature.featureFile}`} className="gherkin-card" open>
            <summary>
              <div>
                <strong>{feature.featureTitle || 'Untitled feature'}</strong>
                <p className="overview-hint">{feature.featureFile || 'Unknown path'}</p>
              </div>
            </summary>
            {Array.isArray(feature.scenarios) && feature.scenarios.length > 0 ? (
              feature.scenarios.map((scenario) => (
                <div key={`${feature.featureTitle}-${scenario.name}`} className="gherkin-scenario">
                  <h4>
                    {scenario.name || 'Scenario'}
                    {scenario.scenarioType ? <span className="pill pill--muted">{scenario.scenarioType}</span> : null}
                  </h4>
                  {Array.isArray(scenario.steps) && scenario.steps.length > 0 ? (
                    <ul>
                      {scenario.steps.map((step, index) => (
                        <li key={`${scenario.name}-${index}`}>{step}</li>
                      ))}
                    </ul>
                  ) : (
                    <p className="overview-hint">No steps recorded.</p>
                  )}
                </div>
              ))
            ) : (
              <p className="overview-hint">No scenarios found for this feature.</p>
            )}
          </details>
        ))}
      </div>
    </div>
  );
};

export default GherkinPanel;
