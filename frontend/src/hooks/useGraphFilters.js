import { useState, useCallback, useMemo, useRef, useEffect } from 'react';

/**
 * Hook for filtering graph nodes by type, search query, etc.
 * Search is debounced to avoid Sigma refresh on every keystroke.
 */
export function useGraphFilters(graph) {
  const [activeTypes, setActiveTypes] = useState(new Set());
  const [searchQuery, setSearchQuery] = useState('');
  const [debouncedSearch, setDebouncedSearch] = useState('');
  const debounceTimer = useRef(null);

  // Debounce search query (150ms)
  useEffect(() => {
    if (debounceTimer.current) clearTimeout(debounceTimer.current);
    debounceTimer.current = setTimeout(() => {
      setDebouncedSearch(searchQuery);
    }, 150);
    return () => {
      if (debounceTimer.current) clearTimeout(debounceTimer.current);
    };
  }, [searchQuery]);

  const nodeTypes = useMemo(() => {
    if (!graph) return [];
    const types = new Set();
    graph.forEachNode((_, attrs) => {
      if (attrs.type) types.add(attrs.type);
    });
    return Array.from(types).sort();
  }, [graph]);

  const toggleType = useCallback((type) => {
    setActiveTypes((prev) => {
      const next = new Set(prev);
      if (next.has(type)) {
        next.delete(type);
      } else {
        next.add(type);
      }
      return next;
    });
  }, []);

  const isNodeVisible = useCallback(
    (nodeId, attrs) => {
      if (activeTypes.size > 0 && !activeTypes.has(attrs.type)) {
        return false;
      }
      if (debouncedSearch) {
        const label = (attrs.label || '').toLowerCase();
        const qname = (attrs.qualifiedName || '').toLowerCase();
        const query = debouncedSearch.toLowerCase();
        return label.includes(query) || qname.includes(query);
      }
      return true;
    },
    [activeTypes, debouncedSearch]
  );

  return {
    activeTypes,
    searchQuery,
    nodeTypes,
    toggleType,
    setSearchQuery,
    isNodeVisible,
  };
}

export default useGraphFilters;
