import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import axios from './lib/apiClient';
import './App.css';
import {
  OverviewPanel,
  ApiSpecsPanel,
  DatabasePanel,
  LoggerInsightsPanel,
  PiiPciPanel,
  DiagramsPanel,
  GherkinPanel,
  MetadataPanel,
  ExportPanel,
  SnapshotsPanel,
  CompiledAnalysisPanel
} from './components/panels';
import GlobalSearchBar from './components/search/GlobalSearchBar';
import SearchResultsPanel from './components/search/SearchResultsPanel';
import LoadingTimeline from './components/progress/LoadingTimeline';
import useMediaQuery from './hooks/useMediaQuery';
import { deriveProjectName, textMatches, formatDate } from './utils/formatters';
import { buildFriendlyError, ERROR_SUGGESTIONS } from './utils/errors';
import { ANALYSIS_STEPS, STATUS_ANALYZED, JOB_STATUS } from './utils/constants';

const sleep = (ms) =>
  new Promise((resolve) => {
    setTimeout(resolve, ms);
  });

const createAbortError = () => {
  if (typeof DOMException === 'function') {
    return new DOMException('Aborted', 'AbortError');
  }
  const error = new Error('Aborted');
  error.name = 'AbortError';
  return error;
};

function App() {
  const [repoUrl, setRepoUrl] = useState('');
  const [branchName, setBranchName] = useState('main');
  const [apiKey, setApiKey] = useState('');
  const [loading, setLoading] = useState(false);
  const [errorState, setErrorState] = useState(null);
  const [result, setResult] = useState(null);
  const [overview, setOverview] = useState(null);
  const [activeTab, setActiveTab] = useState('overview');
  const [apiCatalog, setApiCatalog] = useState(null);
  const [apiLoading, setApiLoading] = useState(false);
  const [dbAnalysis, setDbAnalysis] = useState(null);
  const [dbLoading, setDbLoading] = useState(false);
  const [loggerInsights, setLoggerInsights] = useState([]);
  const [loggerLoading, setLoggerLoading] = useState(false);
  const [piiFindings, setPiiFindings] = useState([]);
  const [piiLoading, setPiiLoading] = useState(false);
  const [projectId, setProjectId] = useState(null);
  const [analysisJob, setAnalysisJob] = useState(null);
  const [diagrams, setDiagrams] = useState([]);
  const [diagramLoading, setDiagramLoading] = useState(false);
  const [diagramSvgContent, setDiagramSvgContent] = useState({});
  const [activeDiagramType, setActiveDiagramType] = useState('CLASS');
  const [activeDiagramId, setActiveDiagramId] = useState(null);
  const [sequenceIncludeExternal, setSequenceIncludeExternal] = useState(false);
  const [sidebarCollapsed, setSidebarCollapsed] = useState(false);
  const [loadingSteps, setLoadingSteps] = useState([]);
  const [metadataPayload, setMetadataPayload] = useState(null);
  const [metadataLoading, setMetadataLoading] = useState(false);
  const [exportPreviewHtml, setExportPreviewHtml] = useState('');
  const [exportPreviewProjectId, setExportPreviewProjectId] = useState(null);
  const [exportPreviewLoading, setExportPreviewLoading] = useState(false);
  const [globalSearchQuery, setGlobalSearchQuery] = useState('');
  const [snapshots, setSnapshots] = useState([]);
  const [snapshotLoading, setSnapshotLoading] = useState(false);
  const [snapshotError, setSnapshotError] = useState(null);
  const [selectedBaseSnapshot, setSelectedBaseSnapshot] = useState(null);
  const [selectedCompareSnapshot, setSelectedCompareSnapshot] = useState(null);
  const [snapshotDiff, setSnapshotDiff] = useState(null);
  const [compiledAnalysis, setCompiledAnalysis] = useState(null);
  const [compiledExports, setCompiledExports] = useState([]);
  const [compiledEntities, setCompiledEntities] = useState({ items: [] });
  const [compiledSequencesTable, setCompiledSequencesTable] = useState({ items: [] });
  const [compiledEndpointsTable, setCompiledEndpointsTable] = useState({ items: [] });
  const [compiledMermaidSource, setCompiledMermaidSource] = useState('');
  const [compiledLoading, setCompiledLoading] = useState(false);
  const [compiledError, setCompiledError] = useState(null);
  const isCompactLayout = useMediaQuery('(max-width: 960px)');
  const tabRefs = useRef({});
  const jobPollRef = useRef({ cancelled: false });

  const projectName = useMemo(() => deriveProjectName(repoUrl), [repoUrl]);
  const diagramsByType = useMemo(() => {
    const groups = {};
    diagrams.forEach((diagram) => {
      const normalizedType = (diagram.diagramType || 'CLASS').toUpperCase();
      if (!groups[normalizedType]) {
        groups[normalizedType] = [];
      }
      groups[normalizedType].push(diagram);
    });
    return groups;
  }, [diagrams]);

  const activeDiagram = useMemo(() => {
    if (!activeDiagramId) {
      return null;
    }
    return diagrams.find((diagram) => diagram.diagramId === activeDiagramId) || null;
  }, [diagrams, activeDiagramId]);
  const normalizedSearch = globalSearchQuery.trim().toLowerCase();
  const globalSearchMatches = useMemo(() => {
    if (!normalizedSearch) {
      return [];
    }
    const matches = [];
    (overview?.classes || []).forEach((cls) => {
      if (
        textMatches(
          normalizedSearch,
          cls.fullyQualifiedName,
          cls.packageName,
          cls.stereotype,
          cls.relativePath
        )
      ) {
        matches.push({
          id: `class-${cls.fullyQualifiedName}`,
          type: 'class',
          title: cls.fullyQualifiedName,
          subtitle: cls.stereotype || cls.packageName || 'Class',
          description: cls.relativePath || '',
          tabValue: 'overview'
        });
      }
    });
    (apiCatalog?.endpoints || []).forEach((endpoint, index) => {
      if (
        textMatches(
          normalizedSearch,
          endpoint.httpMethod,
          endpoint.pathOrOperation,
          endpoint.controllerClass,
          endpoint.controllerMethod
        )
      ) {
        matches.push({
          id: `endpoint-${endpoint.controllerClass}-${endpoint.pathOrOperation}-${index}`,
          type: 'endpoint',
          title: `${endpoint.httpMethod || 'ANY'} ${endpoint.pathOrOperation}`,
          subtitle: endpoint.controllerClass || 'Endpoint',
          description: endpoint.controllerMethod || '',
          tabValue: 'api'
        });
      }
    });
    (loggerInsights || []).forEach((entry, index) => {
      if (textMatches(normalizedSearch, entry.className, entry.filePath, entry.messageTemplate)) {
        matches.push({
          id: `log-${entry.className}-${index}`,
          type: 'log',
          title: entry.className || entry.filePath || 'Logger entry',
          subtitle: `${entry.logLevel || 'LOG'} · line ${entry.lineNumber ?? '—'}`,
          description: entry.messageTemplate || '',
          tabValue: 'logger'
        });
      }
    });
    (piiFindings || []).forEach((entry, index) => {
      if (textMatches(normalizedSearch, entry.filePath, entry.snippet, entry.matchType, entry.severity)) {
        matches.push({
          id: `pii-${entry.filePath}-${index}`,
          type: 'pii',
          title: entry.filePath || entry.matchType || 'Sensitive finding',
          subtitle: `${entry.matchType || 'PII'} · ${entry.severity || 'unknown'} severity`,
          description: entry.snippet || '',
          tabValue: 'pii'
        });
      }
    });
    return matches;
  }, [normalizedSearch, overview, apiCatalog, loggerInsights, piiFindings]);

  const authHeaders = () => (apiKey ? { 'X-API-KEY': apiKey } : {});

  const startProgress = () => {
    setLoadingSteps(
      ANALYSIS_STEPS.map((step, index) => ({
        ...step,
        status: index === 0 ? 'active' : 'pending'
      }))
    );
  };

  const updateStepStatuses = (updates) => {
    if (!updates || updates.length === 0) {
      return;
    }
    setLoadingSteps((prev) => {
      if (prev.length === 0) {
        return prev;
      }
      const lookup = updates.reduce((acc, item) => {
        acc[item.id] = item.status;
        return acc;
      }, {});
      return prev.map((step) => (lookup[step.id] ? { ...step, status: lookup[step.id] } : step));
    });
  };

const handleSearchNavigate = useCallback(
    (match) => {
      if (!match || !match.tabValue) {
        return;
      }
      setActiveTab(match.tabValue);
    },
    [setActiveTab]
  );

  const fetchSnapshotDiff = useCallback(
    async (targetProjectId, baseId, compareId) => {
      if (!targetProjectId || !baseId || !compareId || baseId === compareId) {
        setSnapshotDiff(null);
        return;
      }
      try {
        setSnapshotError(null);
        const response = await axios.get(
          `/project/${targetProjectId}/snapshots/${baseId}/diff/${compareId}`,
          {
            headers: {
              ...authHeaders()
            }
          }
        );
        setSnapshotDiff(response.data || null);
      } catch (error) {
        console.warn('Failed to diff snapshots', error);
        setSnapshotError('Unable to compute snapshot differences.');
      }
    },
    [authHeaders]
  );

  const fetchSnapshots = useCallback(
    async (targetProjectId) => {
      if (!targetProjectId) {
        return;
      }
      try {
        setSnapshotLoading(true);
        setSnapshotError(null);
        const response = await axios.get(`/project/${targetProjectId}/snapshots`, {
          headers: {
            ...authHeaders()
          }
        });
        const snapshotList = response.data?.snapshots || [];
        setSnapshots(snapshotList);
        if (snapshotList.length > 0) {
          const newest = snapshotList[0]?.snapshotId || null;
          const previous = snapshotList[1]?.snapshotId || null;
          setSelectedBaseSnapshot(newest);
          setSelectedCompareSnapshot(previous);
          if (newest && previous) {
            await fetchSnapshotDiff(targetProjectId, newest, previous);
          } else {
            setSnapshotDiff(null);
          }
        } else {
          setSelectedBaseSnapshot(null);
          setSelectedCompareSnapshot(null);
          setSnapshotDiff(null);
        }
      } catch (error) {
        if (error?.response?.status === 404) {
          setSnapshots([]);
          setSnapshotDiff(null);
        } else {
          console.warn('Failed to load snapshots', error);
          setSnapshotError('Unable to load snapshot history.');
        }
      } finally {
        setSnapshotLoading(false);
      }
    },
    [authHeaders, fetchSnapshotDiff]
  );

  const handleSnapshotDiffRequest = useCallback(
    async (baseId, compareId) => {
      setSelectedBaseSnapshot(baseId);
      setSelectedCompareSnapshot(compareId);
      if (!projectId) {
        return;
      }
      await fetchSnapshotDiff(projectId, baseId, compareId);
    },
    [projectId, fetchSnapshotDiff]
  );

  const handleSnapshotRefresh = useCallback(async () => {
    if (!projectId) {
      return;
    }
    await fetchSnapshots(projectId);
  }, [projectId, fetchSnapshots]);

  const handleToggleFindingIgnored = useCallback(
    async (findingId, ignored) => {
      if (!projectId || !findingId) {
        return;
      }
      try {
        setPiiLoading(true);
        const response = await axios.patch(
          `/project/${projectId}/pii-pci/${findingId}`,
          { ignored },
          {
            headers: {
              'Content-Type': 'application/json',
              ...authHeaders()
            }
          }
        );
        setPiiFindings(response.data?.findings ?? []);
      } catch (error) {
        console.warn('Failed to update finding', error);
        setErrorState(buildFriendlyError(error, repoUrl));
      } finally {
        setPiiLoading(false);
      }
    },
    [authHeaders, projectId, repoUrl]
  );

  const pollAnalysisJob = useCallback(
    async (jobId, cancelToken) => {
      let delayMs = 1000;
      while (true) {
        if (cancelToken.cancelled) {
          throw createAbortError();
        }
        const response = await axios.get(`/analyze/${jobId}`, {
          headers: {
            ...(apiKey ? { 'X-API-KEY': apiKey } : {})
          }
        });
        if (cancelToken.cancelled) {
          throw createAbortError();
        }
        const job = response.data;
        setAnalysisJob(job);
        if (job?.status === JOB_STATUS.FAILED) {
          const jobError = new Error(job.errorMessage || 'Analysis job failed');
          jobError.isJobFailure = true;
          jobError.job = job;
          throw jobError;
        }
        if (job?.status === JOB_STATUS.SUCCEEDED && job.projectId) {
          return job;
        }
        await sleep(delayMs);
        delayMs = Math.min(delayMs + 500, 3000);
      }
    },
    [apiKey]
  );


  const loadExportPreview = useCallback(
    (force = false) => {
      if (!projectId) {
        return;
      }
      if (!force && exportPreviewProjectId === projectId && exportPreviewHtml) {
        return;
      }
      setExportPreviewLoading(true);
      axios
        .get(`/project/${projectId}/export/confluence.html`, {
          headers: {
            ...authHeaders()
          },
          responseType: 'text'
        })
        .then((response) => {
          const payload = typeof response.data === 'string' ? response.data : new TextDecoder().decode(response.data);
          setExportPreviewHtml(payload);
          setExportPreviewProjectId(projectId);
        })
        .catch((error) => {
          console.warn('Failed to load export preview', error);
          setExportPreviewHtml('');
          setExportPreviewProjectId(null);
        })
        .finally(() => setExportPreviewLoading(false));
    },
    [apiKey, exportPreviewHtml, exportPreviewProjectId, projectId]
  );

  useEffect(() => {
    if (diagrams.length === 0) {
      setActiveDiagramId(null);
      return;
    }
    const availableTypes = Object.keys(diagramsByType);
    if (availableTypes.length === 0) {
      setActiveDiagramId(null);
      return;
    }
    if (!diagramsByType[activeDiagramType] || diagramsByType[activeDiagramType].length === 0) {
      const nextType = availableTypes[0];
      setActiveDiagramType(nextType);
      setActiveDiagramId(diagramsByType[nextType][0]?.diagramId ?? null);
      return;
    }
    if (
      diagramsByType[activeDiagramType] &&
      !diagramsByType[activeDiagramType].some((diagram) => diagram.diagramId === activeDiagramId)
    ) {
      setActiveDiagramId(diagramsByType[activeDiagramType][0]?.diagramId ?? null);
    }
  }, [diagrams, diagramsByType, activeDiagramType, activeDiagramId]);

  useEffect(() => {
    if (activeDiagramType !== 'SEQUENCE') {
      return;
    }
    const sequences = diagramsByType.SEQUENCE || [];
    if (sequences.length === 0) {
      return;
    }
    const matching = sequences.filter(
      (diagram) => Boolean(diagram.metadata?.includeExternal) === sequenceIncludeExternal
    );
    if (matching.length === 0) {
      const fallback = sequences[0];
      setSequenceIncludeExternal(Boolean(fallback.metadata?.includeExternal));
      setActiveDiagramId(fallback.diagramId ?? null);
      return;
    }
    if (!matching.some((diagram) => diagram.diagramId === activeDiagramId)) {
      setActiveDiagramId(matching[0]?.diagramId ?? null);
    }
  }, [activeDiagramType, diagramsByType, sequenceIncludeExternal, activeDiagramId]);

  useEffect(() => {
    if (!activeDiagram || !activeDiagram.svgAvailable || !activeDiagram.svgDownloadUrl) {
      return;
    }
    if (diagramSvgContent[activeDiagram.diagramId]) {
      return;
    }
    let cancelled = false;
    axios
      .get(activeDiagram.svgDownloadUrl, {
        headers: {
          ...authHeaders()
        },
        responseType: 'text'
      })
      .then((response) => {
        if (cancelled) {
          return;
        }
        const payload = typeof response.data === 'string' ? response.data : new TextDecoder().decode(response.data);
        setDiagramSvgContent((prev) => ({
          ...prev,
          [activeDiagram.diagramId]: payload
        }));
      })
      .catch((error) => {
        if (!cancelled) {
          console.warn('Failed to load diagram SVG', error);
        }
      });
    return () => {
      cancelled = true;
    };
  }, [activeDiagram, diagramSvgContent, apiKey]);

  useEffect(() => {
    if (result?.status === STATUS_ANALYZED) {
      setSidebarCollapsed(true);
    }
  }, [result]);

  useEffect(() => {
    if (isCompactLayout) {
      setSidebarCollapsed(true);
    }
  }, [isCompactLayout]);

  useEffect(() => {
    if (errorState) {
      setSidebarCollapsed(false);
    }
  }, [errorState]);

  useEffect(() => {
    if (activeTab !== 'metadata' || !projectId) {
      return;
    }
    if (metadataPayload && metadataPayload.projectId === projectId) {
      return;
    }
    setMetadataLoading(true);
    axios
      .get(`/project/${projectId}/metadata`, {
        headers: {
          ...authHeaders()
        }
      })
      .then((response) => {
        setMetadataPayload(response.data);
      })
      .catch((metadataError) => {
        console.warn('Failed to load metadata payload', metadataError);
        setMetadataPayload(null);
      })
      .finally(() => setMetadataLoading(false));
  }, [activeTab, projectId, apiKey, metadataPayload]);

  useEffect(() => {
    if (activeTab !== 'export' || !projectId) {
      return;
    }
    loadExportPreview(false);
  }, [activeTab, projectId, loadExportPreview]);

  useEffect(() => {
    return () => {
      jobPollRef.current.cancelled = true;
    };
  }, []);

  const handleSubmit = async (event) => {
    event.preventDefault();
    if (jobPollRef.current) {
      jobPollRef.current.cancelled = true;
    }
    const pollToken = { cancelled: false };
    jobPollRef.current = pollToken;
    setLoading(true);
    setErrorState(null);
    setResult(null);
    setOverview(null);
    setActiveTab('overview');
    setApiCatalog(null);
    setApiLoading(false);
    setDbAnalysis(null);
    setDbLoading(false);
    setLoggerInsights([]);
    setLoggerLoading(false);
    setPiiFindings([]);
    setPiiLoading(false);
    setProjectId(null);
    setAnalysisJob(null);
    setDiagrams([]);
    setDiagramSvgContent({});
    setDiagramLoading(false);
    setActiveDiagramId(null);
    setActiveDiagramType('CLASS');
    setSequenceIncludeExternal(false);
    setMetadataPayload(null);
    setMetadataLoading(false);
    setExportPreviewHtml('');
    setExportPreviewProjectId(null);
    setExportPreviewLoading(false);
    setSnapshots([]);
    setSnapshotDiff(null);
    setSnapshotError(null);
    setSelectedBaseSnapshot(null);
    setSelectedCompareSnapshot(null);
    startProgress();

    console.info('Submitting analysis request', {
      repoUrl,
      hasApiKey: Boolean(apiKey)
    });

    try {
      const response = await axios.post(
        '/analyze',
        { repoUrl, branchName: branchName?.trim() || undefined },
        {
          headers: {
            'Content-Type': 'application/json',
            ...authHeaders()
          }
        }
      );

      const jobDescriptor = response.data;
      console.info('Analysis job queued', jobDescriptor);
      if (!jobDescriptor?.jobId) {
        throw new Error('Backend did not return a job ID for the analysis request.');
      }
      setAnalysisJob(jobDescriptor);
      updateStepStatuses([{ id: 'analyze', status: 'active' }]);

      const completedJob = await pollAnalysisJob(jobDescriptor.jobId, pollToken);
      console.info('Analysis job completed', completedJob);
      if (!completedJob?.projectId) {
        throw new Error('Analysis completed, but the backend did not return a project ID.');
      }

      setAnalysisJob(completedJob);
      setProjectId(completedJob.projectId);
      setResult({
        jobId: completedJob.jobId,
        projectId: completedJob.projectId,
        projectName,
        repoUrl,
        status: STATUS_ANALYZED,
        analyzedAt: completedJob.completedAt || null
      });

      updateStepStatuses([
        { id: 'analyze', status: 'complete' },
        { id: 'overview', status: 'active' }
      ]);

      const targetProjectId = completedJob.projectId;
      try {
        const overviewResponse = await axios.get(`/project/${targetProjectId}/overview`, {
          headers: {
            ...authHeaders()
          }
        });
        console.info('Loaded project overview', {
          projectId: targetProjectId,
          classCount: overviewResponse.data?.classes?.length ?? 0
        });
        setOverview(overviewResponse.data);
        updateStepStatuses([
          { id: 'overview', status: 'complete' },
          { id: 'api', status: 'active' }
        ]);
        try {
          setApiLoading(true);
          const endpointsResponse = await axios.get(`/project/${targetProjectId}/api-endpoints`, {
            headers: {
              ...authHeaders()
            }
          });
          console.info('Loaded API endpoints', {
            projectId: targetProjectId,
            endpointCount: endpointsResponse.data?.endpoints?.length ?? 0
          });
          setApiCatalog(endpointsResponse.data);
          updateStepStatuses([
            { id: 'api', status: 'complete' },
            { id: 'db', status: 'active' }
          ]);
        } catch (catalogError) {
          console.warn('Failed to load API endpoint catalog', catalogError);
          setApiCatalog(null);
          updateStepStatuses([
            { id: 'api', status: 'error' },
            { id: 'db', status: 'active' }
          ]);
        } finally {
          setApiLoading(false);
        }

        try {
          setDbLoading(true);
          const dbResponse = await axios.get(`/project/${targetProjectId}/db-analysis`, {
            headers: {
              ...authHeaders()
            }
          });
          console.info('Loaded database analysis', {
            projectId: targetProjectId,
            entityCount: dbResponse.data?.dbAnalysis?.entities?.length ?? 0
          });
          setDbAnalysis(dbResponse.data?.dbAnalysis ?? null);
          updateStepStatuses([
            { id: 'db', status: 'complete' },
            { id: 'logger', status: 'active' }
          ]);
        } catch (dbError) {
          console.warn('Failed to load database analysis', dbError);
          setDbAnalysis(null);
          updateStepStatuses([
            { id: 'db', status: 'error' },
            { id: 'logger', status: 'active' }
          ]);
        } finally {
          setDbLoading(false);
        }

        try {
          setLoggerLoading(true);
          const loggerResponse = await axios.get(`/project/${targetProjectId}/logger-insights`, {
            headers: {
              ...authHeaders()
            }
          });
          console.info('Loaded logger insights', {
            projectId: targetProjectId,
            count: loggerResponse.data?.loggerInsights?.length ?? 0
          });
          setLoggerInsights(loggerResponse.data?.loggerInsights ?? []);
          updateStepStatuses([
            { id: 'logger', status: 'complete' },
            { id: 'pii', status: 'active' }
          ]);
        } catch (loggerError) {
          console.warn('Failed to load logger insights', loggerError);
          setLoggerInsights([]);
          updateStepStatuses([
            { id: 'logger', status: 'error' },
            { id: 'pii', status: 'active' }
          ]);
        } finally {
          setLoggerLoading(false);
        }

        try {
          setPiiLoading(true);
          const piiResponse = await axios.get(`/project/${targetProjectId}/pii-pci`, {
            headers: {
              ...authHeaders()
            }
          });
          console.info('Loaded PCI / PII findings', {
            projectId: targetProjectId,
            count: piiResponse.data?.findings?.length ?? 0
          });
          setPiiFindings(piiResponse.data?.findings ?? []);
          updateStepStatuses([
            { id: 'pii', status: 'complete' },
            { id: 'diagrams', status: 'active' }
          ]);
        } catch (piiError) {
          console.warn('Failed to load PCI / PII findings', piiError);
          setPiiFindings([]);
          updateStepStatuses([
            { id: 'pii', status: 'error' },
            { id: 'diagrams', status: 'active' }
          ]);
        } finally {
          setPiiLoading(false);
        }

        try {
          setDiagramLoading(true);
          const diagramsResponse = await axios.get(`/project/${targetProjectId}/diagrams`, {
            headers: {
              ...authHeaders()
            }
          });
          const diagramList = Array.isArray(diagramsResponse.data?.diagrams)
            ? diagramsResponse.data.diagrams
            : [];
          setDiagrams(diagramList);
          if (diagramList.length > 0) {
            const firstDiagram = diagramList[0];
            setActiveDiagramType((firstDiagram.diagramType || 'CLASS').toUpperCase());
            setActiveDiagramId(firstDiagram.diagramId ?? null);
            setSequenceIncludeExternal(Boolean(firstDiagram.metadata?.includeExternal));
          } else {
            setActiveDiagramId(null);
          }
          updateStepStatuses([{ id: 'diagrams', status: 'complete' }]);
        } catch (diagramError) {
          console.warn('Failed to load diagrams', diagramError);
          setDiagrams([]);
          setActiveDiagramId(null);
          updateStepStatuses([{ id: 'diagrams', status: 'error' }]);
        } finally {
          setDiagramLoading(false);
        }

        await fetchSnapshots(targetProjectId);
      } catch (fetchError) {
        console.warn('Failed to load project overview', fetchError);
        updateStepStatuses([
          { id: 'overview', status: 'error' },
          { id: 'api', status: 'error' },
          { id: 'db', status: 'error' },
          { id: 'logger', status: 'error' },
          { id: 'pii', status: 'error' },
          { id: 'diagrams', status: 'error' }
        ]);
        setErrorState(buildFriendlyError(fetchError, repoUrl));
      }
    } catch (err) {
      if (err?.name === 'AbortError') {
        return;
      }
      console.error('Repository analysis failed', err);
      updateStepStatuses([{ id: 'analyze', status: 'error' }]);
      if (err?.isJobFailure && err.job) {
        setErrorState({
          message: err.message || 'Analysis job failed.',
          raw: err.job.errorMessage || '',
          suggestions: ERROR_SUGGESTIONS.generic,
          repoUrl
        });
      } else {
        setErrorState(buildFriendlyError(err, repoUrl));
      }
    } finally {
      pollToken.cancelled = true;
      console.debug('Analysis request finalized');
      setLoading(false);
      setLoadingSteps([]);
    }
  };

  const handleExport = async (path, filename) => {
    if (!projectId) {
      return;
    }
    try {
      const response = await axios.get(path, {
        responseType: 'blob',
        headers: {
          ...authHeaders()
        }
      });
      const blob = new Blob([response.data]);
      const url = window.URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.setAttribute('download', filename);
      document.body.appendChild(link);
      link.click();
      link.remove();
      window.URL.revokeObjectURL(url);
    } catch (exportError) {
      console.warn(`Failed to download ${filename}`, exportError);
    }
  };

  const downloadLogsCsv = () =>
    projectId && handleExport(`/project/${projectId}/export/logs.csv`, `logger-insights-${projectId}.csv`);
  const downloadLogsPdf = () =>
    projectId && handleExport(`/project/${projectId}/export/logs.pdf`, `logger-insights-${projectId}.pdf`);
  const downloadPiiCsv = () =>
    projectId && handleExport(`/project/${projectId}/export/pii.csv`, `pii-findings-${projectId}.csv`);
  const downloadPiiPdf = () =>
    projectId && handleExport(`/project/${projectId}/export/pii.pdf`, `pii-findings-${projectId}.pdf`);
  const downloadHtmlExport = () =>
    projectId && handleExport(`/project/${projectId}/export/confluence.html`, `project-${projectId}.html`);
  const downloadSnapshotJson = () =>
    projectId && handleExport(`/project/${projectId}/export/snapshot`, `snapshot-${projectId}.json`);
  const refreshExportPreview = () => loadExportPreview(true);
  const fetchCompiledMermaid = async (analysisId, exportedFiles) => {
    if (!exportedFiles || exportedFiles.length === 0) {
      setCompiledMermaidSource('');
      return;
    }
    const mermaidFile = exportedFiles.find(
      (file) => typeof file.name === 'string' && file.name.toLowerCase().endsWith('.mmd')
    );
    if (!mermaidFile) {
      setCompiledMermaidSource('');
      return;
    }
    try {
      const response = await axios.get(mermaidFile.downloadUrl, {
        headers: authHeaders(),
        responseType: 'text'
      });
      const payload = typeof response.data === 'string' ? response.data : JSON.stringify(response.data, null, 2);
      setCompiledMermaidSource(payload);
    } catch (mermaidError) {
      console.warn(`Failed to load Mermaid ERD for analysis ${analysisId}`, mermaidError);
    }
  };
  const loadCompiledAnalysis = async () => {
    if (!projectId) {
      return;
    }
    setCompiledLoading(true);
    setCompiledError(null);
    try {
      const { data } = await axios.get(`/project/${projectId}/compiled-analysis`, {
        headers: authHeaders()
      });
      setCompiledAnalysis(data);
      const exportList = data.exports || [];
      setCompiledExports(exportList);
      try {
        const [entitiesRes, sequencesRes, endpointsRes] = await Promise.all([
          axios.get('/api/entities', { params: { page: 0, size: 10 }, headers: authHeaders() }),
          axios.get('/api/sequences', { params: { page: 0, size: 10 }, headers: authHeaders() }),
          axios.get('/api/endpoints', { params: { page: 0, size: 10 }, headers: authHeaders() })
        ]);
        setCompiledEntities(entitiesRes.data);
        setCompiledSequencesTable(sequencesRes.data);
        setCompiledEndpointsTable(endpointsRes.data);
      } catch (tableError) {
        console.warn('Failed to load compiled analysis tables', tableError);
      }
      await fetchCompiledMermaid(data.analysisId, exportList);
    } catch (analysisError) {
      if (analysisError?.response?.status === 404) {
        setCompiledAnalysis(null);
        setCompiledExports([]);
        setCompiledMermaidSource('');
      } else {
        setCompiledError(buildFriendlyError(analysisError));
      }
    } finally {
      setCompiledLoading(false);
    }
  };

  const refreshCompiledExports = () => {
    loadCompiledAnalysis();
  };

  useEffect(() => {
    if (!projectId) {
      setCompiledAnalysis(null);
      setCompiledExports([]);
      setCompiledMermaidSource('');
      return;
    }
    loadCompiledAnalysis();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [projectId]);
  const downloadCompiledExport = (exportFile) => {
    if (!exportFile || !exportFile.downloadUrl) {
      return;
    }
    const safeName = exportFile.name || 'compiled-analysis-export';
    handleExport(exportFile.downloadUrl, safeName);
  };

  const downloadDiagramSvg = (diagram) => {
    if (!diagram || !diagram.svgAvailable || !diagram.svgDownloadUrl) {
      return;
    }
    const safeType = (diagram.diagramType || 'diagram').toLowerCase();
    const fileName = `${safeType}-${diagram.diagramId || 'diagram'}.svg`;
    handleExport(diagram.svgDownloadUrl, fileName);
  };

  const tabItems = useMemo(
    () => [
      { value: 'overview', label: 'Overview', disabled: false },
      { value: 'compiled', label: 'Compiled Analysis', disabled: false },
      { value: 'api', label: 'API Specs', disabled: !overview && !apiCatalog },
      { value: 'db', label: 'Database', disabled: !overview && !dbAnalysis && !dbLoading },
      {
        value: 'logger',
        label: 'Logger Insights',
        disabled: !projectId && loggerInsights.length === 0 && !loggerLoading
      },
      { value: 'pii', label: 'PCI / PII Scan', disabled: !projectId && piiFindings.length === 0 && !piiLoading },
      { value: 'diagrams', label: 'Diagrams', disabled: diagrams.length === 0 && !diagramLoading },
      {
        value: 'gherkin',
        label: 'Gherkin',
        disabled: !overview || !overview.gherkinFeatures || overview.gherkinFeatures.length === 0
      },
      {
        value: 'snapshots',
        label: 'Snapshots',
        disabled: snapshots.length === 0 && !snapshotLoading
      },
      { value: 'metadata', label: 'Metadata', disabled: !projectId && !metadataPayload },
      { value: 'export', label: 'Export', disabled: !projectId }
    ],
    [
      overview,
      apiCatalog,
      dbAnalysis,
      dbLoading,
      projectId,
      loggerInsights,
      loggerLoading,
      piiFindings,
      piiLoading,
      diagrams,
      diagramLoading,
      snapshots,
      snapshotLoading,
      metadataPayload
    ]
  );
  const handleTabKeyDown = useCallback(
    (event, currentIndex) => {
      if (event.key !== 'ArrowRight' && event.key !== 'ArrowLeft') {
        return;
      }
      event.preventDefault();
      const direction = event.key === 'ArrowRight' ? 1 : -1;
      let nextIndex = currentIndex;
      for (let i = 0; i < tabItems.length; i += 1) {
        nextIndex = (nextIndex + direction + tabItems.length) % tabItems.length;
        if (!tabItems[nextIndex].disabled) {
          const nextTab = tabItems[nextIndex];
          setActiveTab(nextTab.value);
          if (tabRefs.current[nextTab.value]) {
            tabRefs.current[nextTab.value].focus();
          }
          break;
        }
      }
    },
    [tabItems]
  );

  return (
    <div className="app">
      <div className="grid">
        <form
          className={`card analyze-card ${sidebarCollapsed ? 'analyze-card--collapsed' : ''}`}
          onSubmit={handleSubmit}
        >
          <div className="analyze-card-header">
            <div>
              <h1>CodeVision Analyzer</h1>
              <p className="hint">Launch a repository analysis to capture structural metadata.</p>
            </div>
            {result?.status === STATUS_ANALYZED && (
              <button
                type="button"
                className="ghost-button collapse-toggle"
                onClick={() => setSidebarCollapsed((prev) => !prev)}
              >
                {sidebarCollapsed ? 'Show form' : 'Hide panel'}
              </button>
            )}
          </div>

          {sidebarCollapsed ? (
            <div className="analyze-summary">
              {result?.status === STATUS_ANALYZED ? (
                <>
                  <p className="summary-label">Latest analysis</p>
                  <h2 className="summary-title">{result?.projectName || projectName || 'Repository analysis'}</h2>
                  <dl className="summary-meta">
                    <div>
                      <dt>Project ID</dt>
                      <dd>{result?.projectId || '—'}</dd>
                    </div>
                    <div>
                      <dt>Repository</dt>
                      <dd>{overview?.repoUrl || repoUrl || result?.repoUrl || '—'}</dd>
                    </div>
                    <div>
                      <dt>Analyzed</dt>
                      <dd>
                        {overview?.analyzedAt
                          ? formatDate(overview.analyzedAt)
                          : result?.analyzedAt
                            ? formatDate(result.analyzedAt)
                            : 'Awaiting metadata'}
                      </dd>
                    </div>
                  </dl>
                  <div className="summary-actions">
                    <button type="button" onClick={() => setSidebarCollapsed(false)}>
                      Edit inputs
                    </button>
                    <button
                      type="button"
                      className="ghost-button"
                      onClick={() => setActiveTab('overview')}
                    >
                      View dashboard
                    </button>
                  </div>
                </>
              ) : (
                <>
                  <p className="summary-label">Analyzer hidden</p>
                  <p className="overview-hint">Expand the panel to configure a repository.</p>
                  <button type="button" onClick={() => setSidebarCollapsed(false)}>
                    Edit inputs
                  </button>
                </>
              )}
            </div>
          ) : (
            <div className="analyze-fields">
              <label htmlFor="repoUrl">Repository URL</label>
              <input
                id="repoUrl"
                type="url"
                placeholder="https://github.com/org/repo.git"
                value={repoUrl}
                onChange={(event) => setRepoUrl(event.target.value)}
                required
              />

              <label htmlFor="branchName">Branch</label>
              <input
                id="branchName"
                type="text"
                placeholder="main"
                value={branchName}
                onChange={(event) => setBranchName(event.target.value)}
              />

              <label htmlFor="apiKey">API Key</label>
              <input
                id="apiKey"
                type="text"
                placeholder="Optional if disabled"
                value={apiKey}
                onChange={(event) => setApiKey(event.target.value)}
              />

              <button type="submit" disabled={loading}>
                {loading ? 'Analyzing…' : 'Analyze'}
              </button>

              {errorState && (
                <div className="error-banner" role="alert">
                  <p className="error-message">{errorState.message}</p>
                  {errorState.repoUrl ? (
                    <p className="error-context">
                      Repository: <code>{errorState.repoUrl}</code>
                    </p>
                  ) : null}
                  {Array.isArray(errorState.suggestions) && errorState.suggestions.length > 0 ? (
                    <ul className="error-suggestions">
                      {errorState.suggestions.map((tip) => (
                        <li key={tip}>{tip}</li>
                      ))}
                    </ul>
                  ) : null}
                  {errorState.raw ? (
                    <details>
                      <summary>View technical details</summary>
                      <pre className="error-details">{errorState.raw}</pre>
                    </details>
                  ) : null}
                </div>
              )}
              {analysisJob && (
                <div
                  className="job-status"
                  aria-live="polite"
                  role={analysisJob.status === JOB_STATUS.FAILED ? 'alert' : 'status'}
                >
                  <p className="hint">
                    <strong>Job ID:</strong> <code>{analysisJob.jobId}</code>
                  </p>
                  <p className="hint">
                    {analysisJob.statusMessage || `Status: ${analysisJob.status || 'UNKNOWN'}`}
                  </p>
                </div>
              )}
              {result && result.status === STATUS_ANALYZED && (
                <div className="success">
                  <h2>Analysis complete!</h2>
                  <p>
                    <strong>Project:</strong> {result.projectName || projectName}
                  </p>
                  <p>
                    <strong>ID:</strong> {result.projectId}
                  </p>
                </div>
              )}
            </div>
          )}
          {loadingSteps.length > 0 && <LoadingTimeline steps={loadingSteps} />}
        </form>

        <section className="card overview-card">
          <GlobalSearchBar
            value={globalSearchQuery}
            onChange={setGlobalSearchQuery}
            onClear={() => setGlobalSearchQuery('')}
            resultCount={normalizedSearch ? globalSearchMatches.length : null}
          />
          <SearchResultsPanel
            query={globalSearchQuery}
            matches={globalSearchMatches}
            onNavigate={handleSearchNavigate}
          />
          <div className="tab-nav">
            <select
              className="tab-select"
              aria-label="Select a panel to view"
              value={activeTab}
              onChange={(event) => setActiveTab(event.target.value)}
            >
              {tabItems.map((tab) => (
                <option
                  key={tab.value}
                  value={tab.value}
                  disabled={tab.disabled && tab.value !== activeTab}
                >
                  {tab.label}
                </option>
              ))}
            </select>
            <div className="tab-bar" role="tablist" aria-label="Analysis panels">
              {tabItems.map((tab, index) => (
                <button
                  key={tab.value}
                  type="button"
                  className={`tab-button ${activeTab === tab.value ? 'active' : ''}`}
                  onClick={() => setActiveTab(tab.value)}
                  disabled={tab.disabled}
                  role="tab"
                  aria-selected={activeTab === tab.value}
                  aria-controls={`panel-${tab.value}`}
                  id={`tab-${tab.value}`}
                  aria-disabled={tab.disabled || undefined}
                  ref={(element) => {
                    if (element) {
                      tabRefs.current[tab.value] = element;
                    } else {
                      delete tabRefs.current[tab.value];
                    }
                  }}
                  onKeyDown={(event) => handleTabKeyDown(event, index)}
                >
                  {tab.label}
                </button>
              ))}
            </div>
          </div>
          <div
            role="tabpanel"
            id={`panel-${activeTab}`}
            aria-labelledby={`tab-${activeTab}`}
            tabIndex={0}
          >
            {activeTab === 'overview' ? (
              <OverviewPanel overview={overview} loading={loading && !overview} searchQuery={globalSearchQuery} />
            ) : activeTab === 'compiled' ? (
              <CompiledAnalysisPanel
                analysis={compiledAnalysis}
                exports={compiledExports}
                entities={compiledEntities}
                sequences={compiledSequencesTable}
                endpoints={compiledEndpointsTable}
                mermaidSource={compiledMermaidSource}
                loading={compiledLoading}
                error={compiledError}
                onDownloadExport={downloadCompiledExport}
                onRefresh={refreshCompiledExports}
              />
            ) : activeTab === 'api' ? (
              <ApiSpecsPanel
                overview={overview}
                apiCatalog={apiCatalog}
                loading={apiLoading && !apiCatalog}
                searchQuery={globalSearchQuery}
              />
            ) : activeTab === 'db' ? (
              <DatabasePanel analysis={dbAnalysis} loading={dbLoading && !dbAnalysis} />
            ) : activeTab === 'logger' ? (
              <LoggerInsightsPanel
                insights={loggerInsights}
                loading={loggerLoading && loggerInsights.length === 0}
                onDownloadCsv={downloadLogsCsv}
                onDownloadPdf={downloadLogsPdf}
                searchQuery={globalSearchQuery}
              />
            ) : activeTab === 'pii' ? (
              <PiiPciPanel
                findings={piiFindings}
                loading={piiLoading && piiFindings.length === 0}
                onDownloadCsv={downloadPiiCsv}
                onDownloadPdf={downloadPiiPdf}
                onToggleIgnored={handleToggleFindingIgnored}
                searchQuery={globalSearchQuery}
              />
            ) : activeTab === 'diagrams' ? (
              <DiagramsPanel
                diagramsByType={diagramsByType}
                loading={diagramLoading}
                activeType={activeDiagramType}
                onTypeChange={setActiveDiagramType}
                activeDiagram={activeDiagram}
                onSelectDiagram={setActiveDiagramId}
                svgContent={diagramSvgContent}
                onDownloadSvg={downloadDiagramSvg}
                sequenceIncludeExternal={sequenceIncludeExternal}
                onSequenceToggle={setSequenceIncludeExternal}
              />
            ) : activeTab === 'gherkin' ? (
              <GherkinPanel features={overview?.gherkinFeatures || []} loading={loading && !overview} />
            ) : activeTab === 'snapshots' ? (
              <SnapshotsPanel
                snapshots={snapshots}
                loading={snapshotLoading}
                error={snapshotError}
                onRefresh={handleSnapshotRefresh}
                selectedBase={selectedBaseSnapshot}
                selectedCompare={selectedCompareSnapshot}
                onSelectBase={setSelectedBaseSnapshot}
                onSelectCompare={setSelectedCompareSnapshot}
                onDiff={handleSnapshotDiffRequest}
                diff={snapshotDiff}
              />
            ) : activeTab === 'metadata' ? (
              <MetadataPanel metadata={metadataPayload} loading={metadataLoading} />
            ) : activeTab === 'export' ? (
              <ExportPanel
                projectId={projectId}
                onDownloadHtml={downloadHtmlExport}
                onDownloadSnapshot={downloadSnapshotJson}
                htmlPreview={exportPreviewHtml}
                loading={exportPreviewLoading}
                onRefreshPreview={refreshExportPreview}
              />
            ) : (
              <OverviewPanel overview={overview} loading={loading && !overview} searchQuery={globalSearchQuery} />
            )}
          </div>
        </section>
      </div>
    </div>
  );
}

export default App;
