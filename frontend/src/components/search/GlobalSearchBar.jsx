const GlobalSearchBar = ({ value, onChange, onClear, resultCount }) => (
  <div className="global-search" role="search">
    <label className="sr-only" htmlFor="globalSearchInput">
      Search classes, endpoints, logs, and PII findings
    </label>
    <input
      id="globalSearchInput"
      type="search"
      placeholder="Filter classes, endpoints, logs, PII…"
      value={value}
      onChange={(event) => onChange(event.target.value)}
      aria-describedby="globalSearchHint"
    />
    {value ? (
      <button type="button" className="ghost-button" onClick={onClear} aria-label="Clear search input">
        Clear
      </button>
    ) : null}
    <span id="globalSearchHint" className="search-hint">
      {resultCount === null ? 'Type to search across tabs' : `${resultCount} match${resultCount === 1 ? '' : 'es'}`}
    </span>
  </div>
);

export default GlobalSearchBar;
