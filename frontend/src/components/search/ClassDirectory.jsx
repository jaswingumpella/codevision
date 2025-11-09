import { List } from 'react-window';
import { CLASS_ROW_HEIGHT, MAX_VIRTUALIZED_HEIGHT } from '../../utils/constants';
import { textMatches } from '../../utils/formatters';

const ClassDirectory = ({ classes, searchQuery }) => {
  const normalized = (searchQuery || '').trim().toLowerCase();
  const filtered = Array.isArray(classes)
    ? normalized
      ? classes.filter((cls) =>
          textMatches(normalized, cls.fullyQualifiedName, cls.packageName, cls.stereotype, cls.relativePath)
        )
      : classes
    : [];

  if (!classes || classes.length === 0) {
    return <p className="overview-hint">No classes were captured during the last scan.</p>;
  }

  if (filtered.length === 0) {
    return <p className="overview-hint">No classes match "{searchQuery}".</p>;
  }

  const listHeight = Math.min(MAX_VIRTUALIZED_HEIGHT, filtered.length * CLASS_ROW_HEIGHT);

  return (
    <div className="class-directory" role="table" aria-label="Class directory">
      <div className="class-directory-header" role="row">
        <span role="columnheader">Fully Qualified Name</span>
        <span role="columnheader">Stereotype</span>
        <span role="columnheader">Source Set</span>
      </div>
      <div className="class-directory-rows" role="rowgroup">
        <List
          height={Math.max(CLASS_ROW_HEIGHT, listHeight)}
          itemCount={filtered.length}
          itemSize={CLASS_ROW_HEIGHT}
          width="100%"
          itemData={filtered}
        >
          {({ index, style, data }) => {
            const cls = data[index];
            return (
              <div className="class-directory-row" key={cls.fullyQualifiedName} style={style} role="row">
                <span role="cell">{cls.fullyQualifiedName}</span>
                <span role="cell">{cls.stereotype || '—'}</span>
                <span role="cell">{cls.sourceSet || '—'}</span>
              </div>
            );
          }}
        </List>
      </div>
    </div>
  );
};

export default ClassDirectory;
