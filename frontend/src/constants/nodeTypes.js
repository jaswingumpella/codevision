/**
 * Single source of truth for node type visual configuration.
 * Used by useGraphData (graph rendering) and GraphLegend (UI legend).
 */
export const NODE_TYPE_CONFIG = {
  ENDPOINT:            { color: '#e74c3c', size: 8,  label: 'Endpoint' },
  CLASS:               { color: '#3498db', size: 6,  label: 'Class' },
  INTERFACE:           { color: '#9b59b6', size: 6,  label: 'Interface' },
  METHOD:              { color: '#2ecc71', size: 4,  label: 'Method' },
  FIELD:               { color: '#f39c12', size: 3,  label: 'Field' },
  DATABASE_ENTITY:     { color: '#1abc9c', size: 7,  label: 'DB Entity' },
  DATABASE_COLUMN:     { color: '#16a085', size: 3,  label: 'DB Column' },
  DEPENDENCY_ARTIFACT: { color: '#e67e22', size: 5,  label: 'Dependency' },
  PACKAGE:             { color: '#8e44ad', size: 5,  label: 'Package' },
  TEST_CASE:           { color: '#27ae60', size: 5,  label: 'Test' },
  TEST_SUITE:          { color: '#2d8659', size: 6,  label: 'Test Suite' },
  ENUM:                { color: '#d35400', size: 5,  label: 'Enum' },
  ANNOTATION:          { color: '#c0392b', size: 4,  label: 'Annotation' },
  CONSTRUCTOR:         { color: '#2980b9', size: 4,  label: 'Constructor' },
  PARAMETER:           { color: '#7f8c8d', size: 2,  label: 'Parameter' },
  GENERIC_TYPE:        { color: '#95a5a6', size: 3,  label: 'Generic Type' },
};

/** Default color for unknown node types. */
export const DEFAULT_NODE_COLOR = '#bdc3c7';

/** Default size for unknown node types. */
export const DEFAULT_NODE_SIZE = 4;

/**
 * Get the color for a node type.
 */
export function getNodeColor(type) {
  return NODE_TYPE_CONFIG[type]?.color || DEFAULT_NODE_COLOR;
}

/**
 * Get the size for a node type.
 */
export function getNodeSize(type) {
  return NODE_TYPE_CONFIG[type]?.size || DEFAULT_NODE_SIZE;
}
