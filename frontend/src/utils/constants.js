const STATUS_ANALYZED = 'ANALYZED_METADATA';
const PAGE_SIZE = 10;

const ANALYSIS_STEPS = [
  { id: 'analyze', label: 'Cloning repository and running analysis' },
  { id: 'overview', label: 'Loading project overview' },
  { id: 'api', label: 'Mapping APIs and OpenAPI specs' },
  { id: 'db', label: 'Inspecting database entities' },
  { id: 'logger', label: 'Gathering logger insights' },
  { id: 'pii', label: 'Scanning for PCI / PII risks' },
  { id: 'diagrams', label: 'Generating diagrams' }
];

const SEARCH_RESULT_ROW_HEIGHT = 72;
const CLASS_ROW_HEIGHT = 56;
const MAX_VIRTUALIZED_HEIGHT = 360;

const RESULT_TYPE_COPY = {
  class: { label: 'Class', tabLabel: 'Overview' },
  endpoint: { label: 'Endpoint', tabLabel: 'API Specs' },
  log: { label: 'Log', tabLabel: 'Logger Insights' },
  pii: { label: 'PII Finding', tabLabel: 'PCI / PII Scan' }
};

export {
  STATUS_ANALYZED,
  PAGE_SIZE,
  ANALYSIS_STEPS,
  SEARCH_RESULT_ROW_HEIGHT,
  CLASS_ROW_HEIGHT,
  MAX_VIRTUALIZED_HEIGHT,
  RESULT_TYPE_COPY
};
