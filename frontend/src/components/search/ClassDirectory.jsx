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

  return (
    <div className="class-directory" role="table" aria-label="Class directory">
      <div className="class-directory-header" role="row">
        <span role="columnheader">Fully Qualified Name</span>
        <span role="columnheader">Stereotype</span>
        <span role="columnheader">Source Set</span>
      </div>
      <div className="class-directory-rows class-directory-rows--scrollable" role="rowgroup">
        {filtered.map((cls, index) => (
          <div
            className="class-directory-row"
            key={`${cls.fullyQualifiedName || 'class'}-${index}`}
            role="row"
          >
            <span role="cell">{cls.fullyQualifiedName}</span>
            <span role="cell">{cls.stereotype || '—'}</span>
            <span role="cell">{cls.sourceSet || '—'}</span>
          </div>
        ))}
      </div>
    </div>
  );
};

export default ClassDirectory;
