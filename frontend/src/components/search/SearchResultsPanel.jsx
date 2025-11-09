import { List } from 'react-window';
import { MAX_VIRTUALIZED_HEIGHT, RESULT_TYPE_COPY, SEARCH_RESULT_ROW_HEIGHT } from '../../utils/constants';

const SearchResultRow = ({ index, style, data }) => {
  const match = data.matches[index];
  if (!match) {
    return null;
  }
  const copy = RESULT_TYPE_COPY[match.type] || RESULT_TYPE_COPY.class;
  const handleNavigate = () => data.onNavigate(match);

  return (
    <div
      className="search-result-row"
      style={style}
      role="listitem"
      aria-roledescription="Search result"
      tabIndex={0}
      onClick={handleNavigate}
      onKeyDown={(event) => {
        if (event.key === 'Enter' || event.key === ' ') {
          event.preventDefault();
          handleNavigate();
        }
      }}
      aria-label={`${copy.label} result: ${match.title}`}
    >
      <div className="search-result-copy">
        <p className="search-result-title">
          <span className="badge badge-info">{copy.label}</span>
          {match.title}
        </p>
        <p className="search-result-meta">{match.subtitle || copy.tabLabel}</p>
        {match.description ? <p className="search-result-description">{match.description}</p> : null}
      </div>
      <button
        type="button"
        className="ghost-button"
        onClick={(event) => {
          event.stopPropagation();
          handleNavigate();
        }}
      >
        View {copy.tabLabel}
      </button>
    </div>
  );
};

const SearchResultsPanel = ({ query, matches, onNavigate }) => {
  if (!query) {
    return null;
  }
  return (
    <section className="search-results" aria-live="polite" aria-label="Global search results">
      <div className="search-results-header">
        <h2>Search results</h2>
        <span>{matches.length} match{matches.length === 1 ? '' : 'es'}</span>
      </div>
      {matches.length === 0 ? (
        <p className="overview-hint">No classes, endpoints, logs, or PII findings match "{query}".</p>
      ) : (
        <div className="search-results-list" role="list">
          <List
            height={Math.min(MAX_VIRTUALIZED_HEIGHT, SEARCH_RESULT_ROW_HEIGHT * matches.length)}
            itemCount={matches.length}
            itemSize={SEARCH_RESULT_ROW_HEIGHT}
            width="100%"
            itemData={{ matches, onNavigate }}
          >
            {SearchResultRow}
          </List>
        </div>
      )}
    </section>
  );
};

export default SearchResultsPanel;
