import { render, screen, waitFor, act } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import App from './App';
import axios from './lib/apiClient';

vi.mock('./lib/apiClient', () => ({
  __esModule: true,
  default: {
    post: vi.fn(),
    get: vi.fn(),
    patch: vi.fn()
  }
}));

const mockOverview = {
  projectId: 101,
  projectName: 'demo-project',
  repoUrl: 'https://example.com/org/repo.git',
  analyzedAt: new Date('2024-05-01T12:00:00Z').toISOString(),
  buildInfo: {
    groupId: 'com.barclays',
    artifactId: 'app',
    version: '1.0.0',
    javaVersion: '21'
  },
  classes: [
    {
      fullyQualifiedName: 'com.barclays.Controller',
      packageName: 'com.barclays',
      className: 'Controller',
      stereotype: 'CONTROLLER',
      userCode: true,
      sourceSet: 'MAIN',
      relativePath: 'src/main/java/com/barclays/Controller.java',
      annotations: ['RestController'],
      interfacesImplemented: []
    }
  ],
  metadataDump: {
    openApiSpecs: [
      {
        fileName: 'openapi.yml',
        content: 'openapi: 3.0.0'
      }
    ]
  }
};

const suppressActWarnings = () => {
  const originalError = console.error;
  const errorSpy = vi.spyOn(console, 'error').mockImplementation((message, ...args) => {
    if (typeof message === 'string' && message.includes('not wrapped in act')) {
      return;
    }
    originalError(message, ...args);
  });
  return () => errorSpy.mockRestore();
};

describe('App', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('submits analysis request and renders overview details', async () => {
    axios.post.mockResolvedValueOnce({
      data: {
        jobId: 'job-123',
        repoUrl: 'https://example.com/org/repo.git',
        status: 'QUEUED',
        statusMessage: 'Queued for analysis',
        createdAt: new Date('2024-05-01T11:59:00Z').toISOString()
      }
    });
    axios.get
      .mockResolvedValueOnce({
        data: {
          jobId: 'job-123',
          projectId: 101,
          repoUrl: 'https://example.com/org/repo.git',
          status: 'SUCCEEDED',
          completedAt: new Date('2024-05-01T12:00:00Z').toISOString()
        }
      })
      .mockResolvedValueOnce({ data: mockOverview })
      .mockResolvedValueOnce({ data: { endpoints: [] } })
      .mockResolvedValueOnce({ data: { dbAnalysis: null } })
      .mockResolvedValueOnce({ data: { loggerInsights: [] } })
      .mockResolvedValueOnce({ data: { findings: [] } })
      .mockResolvedValueOnce({ data: { diagrams: [] } })
      .mockRejectedValueOnce({ response: { status: 404 } });

    render(<App />);

    const repoInput = screen.getByLabelText(/repository url/i);
    const branchInput = screen.getByLabelText(/branch/i);
    const apiKeyInput = screen.getByLabelText(/api key/i);
    const submitButton = screen.getByRole('button', { name: /analyze/i });

    await act(async () => {
      await userEvent.type(repoInput, 'https://example.com/org/repo.git');
      await userEvent.clear(branchInput);
      await userEvent.type(branchInput, 'feature/login');
      await userEvent.type(apiKeyInput, 'super-secret');
      await userEvent.click(submitButton);
    });

    await waitFor(() =>
      expect(axios.post).toHaveBeenCalledWith(
        '/analyze',
        { repoUrl: 'https://example.com/org/repo.git', branchName: 'feature/login', includeSecurity: true },
        {
          headers: {
            'Content-Type': 'application/json',
            'X-API-KEY': 'super-secret'
          }
        }
      )
    );

    await waitFor(() =>
      expect(axios.get).toHaveBeenCalledWith('/analyze/job-123', {
        headers: {
          'X-API-KEY': 'super-secret'
        }
      })
    );

    await waitFor(() =>
      expect(axios.get).toHaveBeenCalledWith('/project/101/overview', {
        headers: {
          'X-API-KEY': 'super-secret'
        }
      })
    );
    await waitFor(() =>
      expect(axios.get).toHaveBeenCalledWith('/project/101/snapshots', {
        headers: {
          'X-API-KEY': 'super-secret'
        }
      })
    );

    expect(await screen.findByText(/latest analysis/i)).toBeInTheDocument();
    expect(screen.getByText(/demo-project/)).toBeInTheDocument();
    expect(screen.getAllByText(/com\.barclays/).length).toBeGreaterThan(0);
    expect(screen.getByText(/openapi\.yml/)).toBeInTheDocument();
  });

  it('shows an error message when analysis fails', async () => {
    axios.post.mockRejectedValueOnce({ response: { data: 'Failed to analyze repository' } });

    render(<App />);

    const repoInput = screen.getByLabelText(/repository url/i);
    await act(async () => {
      await userEvent.type(repoInput, 'https://example.com/org/repo.git');
      await userEvent.click(screen.getByRole('button', { name: /analyze/i }));
    });

    expect(await screen.findByText(/analysis could not be completed/i)).toBeInTheDocument();
    expect(screen.getByText(/Retry the analysis/i)).toBeInTheDocument();
  });

  it('surfaces job failures returned by the poll endpoint', async () => {
    axios.post.mockResolvedValueOnce({
      data: {
        jobId: 'job-failed',
        repoUrl: 'https://example.com/org/repo.git',
        status: 'QUEUED',
        statusMessage: 'Queued'
      }
    });
    axios.get.mockResolvedValueOnce({
      data: {
        jobId: 'job-failed',
        status: 'FAILED',
        errorMessage: 'Repository clone timed out',
        statusMessage: 'Failed'
      }
    });

    render(<App />);

    const repoInput = screen.getByLabelText(/repository url/i);
    await act(async () => {
      await userEvent.type(repoInput, 'https://example.com/org/repo.git');
      await userEvent.click(screen.getByRole('button', { name: /analyze/i }));
    });

    const failureNotices = await screen.findAllByText(/Repository clone timed out/i);
    expect(failureNotices.length).toBeGreaterThan(0);
  });

  it('loads metadata, export preview, and allows panel interactions', async () => {
    const user = userEvent.setup();
    const projectId = 202;
    const jobId = 'job-456';
    const jobResponse = {
      jobId,
      repoUrl: 'https://example.com/org/repo.git',
      status: 'QUEUED',
      statusMessage: 'Queued'
    };
    const overviewResponse = {
      ...mockOverview,
      projectId,
      projectName: 'demo-project',
      gherkinFeatures: [
        {
          featureTitle: 'Payments',
          featureFile: 'features/payments.feature',
          scenarios: [{ name: 'Process payment', scenarioType: 'SCENARIO', steps: ['Given a cart', 'When payment is submitted'] }]
        }
      ]
    };
    const snapshots = [
      { snapshotId: 2, branchName: 'main', commitHash: 'abc123', createdAt: '2024-05-01T00:00:00Z' },
      { snapshotId: 1, branchName: 'main', commitHash: 'fff999', createdAt: '2024-04-30T00:00:00Z' }
    ];
    const snapshotDiff = {
      baseSnapshotId: 2,
      compareSnapshotId: 1,
      addedClasses: [{ fullyQualifiedName: 'com.demo.NewService', stereotype: 'SERVICE' }],
      removedClasses: []
    };
    const apiCatalogResponse = {
      endpoints: [
        {
          protocol: 'REST',
          httpMethod: 'GET',
          pathOrOperation: '/orders',
          controllerClass: 'OrderController',
          controllerMethod: 'listOrders',
          specArtifacts: [{ type: 'OPENAPI', name: 'orders.yaml' }]
        }
      ]
    };
    const dbAnalysisResponse = {
      dbAnalysis: {
        entities: [{ entityName: 'Order', tableName: 'orders', primaryKeys: ['id'] }],
        classesByEntity: { Order: ['OrderRepository'] },
        operationsByClass: { OrderRepository: [{ methodName: 'findAll', operationType: 'SELECT', target: 'Order', querySnippet: '' }] }
      }
    };
    const loggerInsightsResponse = {
      loggerInsights: [
        {
          className: 'com.demo.Logger',
          filePath: 'Logger.java',
          logLevel: 'ERROR',
          lineNumber: 10,
          messageTemplate: 'Payment failed {}',
          variables: ['orderId'],
          piiRisk: false,
          pciRisk: true
        }
      ]
    };
    const piiFindingsResponse = {
      findings: [
        {
          findingId: 1,
          filePath: 'OrderController.java',
          lineNumber: 42,
          snippet: 'cardNumber',
          matchType: 'PCI',
          severity: 'HIGH',
          ignored: false
        }
      ]
    };
    const diagrams = [
      { diagramId: 11, diagramType: 'CLASS', title: 'Class Layout', svgAvailable: true, svgDownloadUrl: `/project/${projectId}/diagram/11/svg` }
    ];
    const metadataResponse = {
      projectId,
      projectName: 'demo-project',
      analyzedAt: new Date('2024-05-01T12:00:00Z').toISOString(),
      snapshotDownloadUrl: `/project/${projectId}/export/snapshot`,
      metadataDump: {
        openApiSpecs: [{ fileName: 'orders.yaml', content: 'openapi: 3.0.0' }],
        wsdlDocuments: [],
        xsdDocuments: [],
        soapServices: []
      }
    };
    let diffCallCount = 0;
    axios.post.mockResolvedValueOnce({ data: jobResponse });
    axios.get.mockImplementation((url) => {
      if (url.startsWith(`/analyze/${jobId}`)) {
        return Promise.resolve({
          data: {
            ...jobResponse,
            status: 'SUCCEEDED',
            statusMessage: 'Snapshot saved',
            projectId
          }
        });
      }
      if (url === `/project/${projectId}/overview`) {
        return Promise.resolve({ data: overviewResponse });
      }
      if (url === `/project/${projectId}/api-endpoints`) {
        return Promise.resolve({ data: apiCatalogResponse });
      }
      if (url === `/project/${projectId}/db-analysis`) {
        return Promise.resolve({ data: dbAnalysisResponse });
      }
      if (url === `/project/${projectId}/logger-insights`) {
        return Promise.resolve({ data: loggerInsightsResponse });
      }
      if (url === `/project/${projectId}/pii-pci`) {
        return Promise.resolve({ data: piiFindingsResponse });
      }
      if (url === `/project/${projectId}/diagrams`) {
        return Promise.resolve({ data: { diagrams } });
      }
      if (url === `/project/${projectId}/snapshots`) {
        return Promise.resolve({ data: { snapshots } });
      }
      if (url === `/project/${projectId}/snapshots/${snapshots[0].snapshotId}/diff/${snapshots[1].snapshotId}`) {
        diffCallCount += 1;
        return Promise.resolve({
          data: {
            ...snapshotDiff,
            addedClasses: [
              { fullyQualifiedName: `com.demo.NewService${diffCallCount}`, stereotype: 'SERVICE' }
            ]
          }
        });
      }
      if (url === `/project/${projectId}/metadata`) {
        return Promise.resolve({ data: metadataResponse });
      }
      if (url === `/project/${projectId}/export/confluence.html`) {
        return Promise.resolve({ data: '<html>preview</html>' });
      }
      if (url === diagrams[0].svgDownloadUrl) {
        return Promise.resolve({ data: '<svg>diagram</svg>' });
      }
      if (url.includes(`/project/${projectId}/export`) || url.startsWith('/compiled/')) {
        return Promise.resolve({ data: 'file-binary' });
      }
      return Promise.resolve({ data: {} });
    });
    axios.patch.mockResolvedValueOnce({
      data: {
        findings: [{ ...piiFindingsResponse.findings[0], ignored: true }]
      }
    });

    const originalCreate = window.URL.createObjectURL;
    const originalRevoke = window.URL.revokeObjectURL;
    window.URL.createObjectURL = vi.fn(() => 'blob:mock');
    window.URL.revokeObjectURL = vi.fn();
    const anchorClickSpy = vi.spyOn(HTMLAnchorElement.prototype, 'click').mockImplementation(() => {});

    const restoreConsole = suppressActWarnings();
    try {
      await act(async () => {
        render(<App />);
      });

      await act(async () => {
        await user.type(screen.getByLabelText(/repository url/i), 'https://example.com/org/repo.git');
        await user.clear(screen.getByLabelText(/branch/i));
        await user.type(screen.getByLabelText(/branch/i), 'feature/coverage');
        await user.type(screen.getByLabelText(/api key/i), 'key-123');
        await user.click(screen.getByRole('button', { name: /analyze/i }));
      });

      await waitFor(() => expect(screen.getByText(/demo-project/i)).toBeInTheDocument());

      const searchInput = screen.getByPlaceholderText(/Filter classes/i);
      await user.clear(searchInput);
      await user.type(searchInput, 'order');
      const viewApiButton = await screen.findByRole('button', { name: /View API Specs/i });
      await user.click(viewApiButton);
      expect(screen.getByRole('tab', { name: 'API Specs', selected: true })).toBeInTheDocument();

      await user.click(screen.getByRole('tab', { name: 'Metadata' }));
      await screen.findByText(metadataResponse.snapshotDownloadUrl);

      await user.click(screen.getByRole('tab', { name: 'Export' }));
      await screen.findByTitle(/Confluence Export Preview/i);
      await user.click(screen.getByRole('button', { name: /Refresh preview/i }));
      await screen.findByTitle(/Confluence Export Preview/i);
      await user.click(screen.getByRole('button', { name: /Download Snapshot JSON/i }));

      await user.click(screen.getByRole('tab', { name: 'Diagrams' }));
      await screen.findByRole('button', { name: /Download SVG/i });
      await user.click(screen.getByRole('button', { name: /Download SVG/i }));

      await user.click(screen.getByRole('tab', { name: 'Snapshots' }));
      await screen.findByText(/Snapshot History/i);
      await user.click(screen.getByRole('button', { name: /^Refresh$/i }));
      await user.click(screen.getByRole('button', { name: /Show Diff/i }));
      await waitFor(() => expect(diffCallCount).toBeGreaterThan(1));

      await user.click(screen.getByRole('tab', { name: /PCI \/ PII Scan/i }));
      await screen.findAllByText(/OrderController\.java/i);
      await user.click(screen.getByRole('button', { name: /Ignore/i }));
      expect(axios.patch).toHaveBeenCalledWith(
        `/project/${projectId}/pii-pci/1`,
        { ignored: true },
        expect.objectContaining({
          headers: expect.objectContaining({ 'Content-Type': 'application/json' })
        })
      );

      await user.click(screen.getByRole('tab', { name: /Logger Insights/i }));
      await screen.findByText(/com\.demo\.Logger/i);
      await user.click(screen.getByRole('button', { name: /Download CSV/i }));
      expect(window.URL.createObjectURL).toHaveBeenCalled();
    } finally {
      restoreConsole();
      anchorClickSpy.mockRestore();
      window.URL.createObjectURL = originalCreate;
      window.URL.revokeObjectURL = originalRevoke;
    }
  });
});
