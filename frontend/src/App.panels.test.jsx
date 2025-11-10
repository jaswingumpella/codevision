import { describe, expect, it, vi } from 'vitest';
import { render, screen, act } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { deriveProjectName, formatDate } from './utils/formatters';
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

describe('utility helpers', () => {
  it('derives project names from repo urls', () => {
    expect(deriveProjectName('https://github.com/org/repo.git')).toBe('repo');
    expect(deriveProjectName('https://github.com/org/repo/')).toBe('repo');
    expect(deriveProjectName('')).toBe('');
  });

  it('formats dates and falls back to placeholder', () => {
    const spy = vi.spyOn(Date.prototype, 'toLocaleString').mockReturnValue('May 01, 2024');
    expect(formatDate('2024-05-01T00:00:00Z')).toBe('May 01, 2024');
    spy.mockRestore();
    expect(formatDate('')).toBe('—');
  });
});

const sampleOverview = {
  projectName: 'demo-project',
  repoUrl: 'https://example.com/repo.git',
  analyzedAt: '2024-05-01T12:00:00.000Z',
  classes: [
    { sourceSet: 'MAIN', stereotype: 'CONTROLLER' },
    { sourceSet: 'TEST', stereotype: 'SERVICE' }
  ],
  buildInfo: {
    groupId: 'com.example',
    artifactId: 'demo',
    version: '1.0.0',
    javaVersion: '21'
  },
  metadataDump: {
    openApiSpecs: [{ fileName: 'openapi.yaml', content: 'openapi: 3.0.0' }],
    wsdlDocuments: [],
    xsdDocuments: [],
    soapServices: []
  },
  assets: {
    images: [{ fileName: 'diagram.png', relativePath: 'docs/diagram.png' }]
  }
};

describe('OverviewPanel', () => {
  it('shows loading state', () => {
    render(<OverviewPanel overview={null} loading searchQuery="" />);
    expect(screen.getByText(/Analyzing repository/i)).toBeInTheDocument();
  });

  it('renders overview details and stats', () => {
    render(<OverviewPanel overview={sampleOverview} loading={false} searchQuery="" />);
    expect(screen.getByText('demo-project')).toBeInTheDocument();
    expect(screen.getByText(/openapi\.yaml/i)).toBeInTheDocument();
    expect(screen.getByText('Total Classes')).toBeInTheDocument();
  });

  it('prompts for analysis when overview is missing', () => {
    render(<OverviewPanel overview={null} loading={false} searchQuery="" />);
    expect(screen.getByText(/Run an analysis/i)).toBeInTheDocument();
  });
});

describe('ApiSpecsPanel', () => {
  const catalog = {
    endpoints: [
      {
        protocol: 'REST',
        httpMethod: 'GET',
        pathOrOperation: '/demo',
        controllerClass: 'DemoController',
        controllerMethod: 'getDemo',
        specArtifacts: [{ type: 'OPENAPI', name: 'openapi.yaml' }]
      },
      {
        protocol: 'SOAP',
        pathOrOperation: 'GetCustomer',
        controllerClass: 'SoapEndpoint',
        controllerMethod: 'handle',
        specArtifacts: []
      }
    ]
  };

  it('renders endpoint tables with pagination controls', () => {
    const manyEndpoints = Array.from({ length: 12 }, (_, index) => ({
      protocol: 'REST',
      httpMethod: 'GET',
      pathOrOperation: `/resource/${index}`,
      controllerClass: 'Ctrl',
      controllerMethod: `method${index}`
    }));
    render(
      <ApiSpecsPanel
        overview={sampleOverview}
        apiCatalog={{ endpoints: manyEndpoints }}
        loading={false}
        searchQuery=""
      />
    );
    expect(screen.getByText(/REST Endpoints/i)).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /Next/i })).toBeEnabled();
  });

  it('shows helpful empty states', () => {
    render(<ApiSpecsPanel overview={null} apiCatalog={{ endpoints: [] }} loading={false} searchQuery="" />);
    expect(screen.getByText(/Run an analysis/i)).toBeInTheDocument();
  });

  it('displays loading indicator while fetching endpoints', () => {
    render(<ApiSpecsPanel overview={null} apiCatalog={{ endpoints: [] }} loading searchQuery="" />);
    expect(screen.getByText(/Loading endpoints/i)).toBeInTheDocument();
  });
});

describe('DatabasePanel', () => {
  it('renders entity and dao tables', () => {
    const analysis = {
      entities: [{ entityName: 'Customer', tableName: 'customers', primaryKeys: ['id'] }],
      classesByEntity: { Customer: ['CustomerRepository'] },
      operationsByClass: {
        CustomerRepository: [{ methodName: 'findAll', operationType: 'SELECT', target: 'Customer', querySnippet: '' }]
      }
    };
    render(<DatabasePanel analysis={analysis} loading={false} />);
    expect(screen.getAllByText('Customer')).not.toHaveLength(0);
    expect(screen.getAllByText('CustomerRepository')).not.toHaveLength(0);
  });

  it('shows loading placeholder while fetching analysis', () => {
    render(<DatabasePanel analysis={null} loading />);
    expect(screen.getByText(/Loading database metadata/i)).toBeInTheDocument();
  });

  it('prompts to run an analysis when no data yet', () => {
    render(<DatabasePanel analysis={null} loading={false} />);
    expect(screen.getByText(/Run an analysis to view entities/i)).toBeInTheDocument();
  });
});

describe('LoggerInsightsPanel', () => {
  it('renders loading copy while scanning logs', () => {
    render(
      <LoggerInsightsPanel
        insights={[]}
        loading
        onDownloadCsv={vi.fn()}
        onDownloadPdf={vi.fn()}
        searchQuery=""
      />
    );
    expect(screen.getByText(/Scanning log statements/i)).toBeInTheDocument();
  });

  it('filters by class, level, and risk toggles', async () => {
    const user = userEvent.setup();
    const insights = [
      {
        className: 'com.demo.Foo',
        logLevel: 'INFO',
        filePath: 'Foo.java',
        lineNumber: 10,
        messageTemplate: 'Hello',
        variables: [],
        piiRisk: false,
        pciRisk: false
      },
      {
        className: 'com.demo.Bar',
        logLevel: 'ERROR',
        filePath: 'Bar.java',
        lineNumber: 20,
        messageTemplate: 'Sensitive {}',
        variables: ['cardNumber'],
        piiRisk: true,
        pciRisk: true
      }
    ];
    render(
      <LoggerInsightsPanel
        insights={insights}
        loading={false}
        onDownloadCsv={vi.fn()}
        onDownloadPdf={vi.fn()}
        searchQuery=""
      />
    );

    await act(async () => {
      await user.type(screen.getByLabelText(/Class Filter/i), 'Bar');
      await user.selectOptions(screen.getByLabelText(/^Level$/i), 'ERROR');
      await user.click(screen.getByLabelText(/Only PII risk/i));
    });
    expect(screen.getByText('com.demo.Bar')).toBeInTheDocument();
    expect(screen.queryByText('com.demo.Foo')).not.toBeInTheDocument();
  });
});

describe('PiiPciPanel', () => {
  it('renders loading state while findings load', () => {
    render(
      <PiiPciPanel
        findings={[]}
        loading
        onDownloadCsv={vi.fn()}
        onDownloadPdf={vi.fn()}
        searchQuery=""
      />
    );
    expect(screen.getByText(/Collecting sensitive data findings/i)).toBeInTheDocument();
  });

  it('applies filters and hide ignored toggle', async () => {
    const user = userEvent.setup();
    const findings = [
      {
        findingId: 1,
        filePath: 'a.txt',
        lineNumber: 1,
        snippet: 'card',
        matchType: 'PCI',
        severity: 'HIGH',
        ignored: false
      },
      {
        findingId: 2,
        filePath: 'b.txt',
        lineNumber: 2,
        snippet: 'email',
        matchType: 'PII',
        severity: 'LOW',
        ignored: true
      }
    ];
    render(
      <PiiPciPanel
        findings={findings}
        loading={false}
        onDownloadCsv={vi.fn()}
        onDownloadPdf={vi.fn()}
        searchQuery=""
      />
    );

    await act(async () => {
      await user.selectOptions(screen.getByLabelText(/Match Type/i), 'PCI');
      await user.selectOptions(screen.getByLabelText(/Severity/i), 'HIGH');
      await user.click(screen.getByLabelText(/Hide ignored/i));
    });
    expect(screen.getByText('a.txt')).toBeInTheDocument();
    expect(screen.queryByText('b.txt')).not.toBeInTheDocument();
  });

  it('invokes ignore toggle callbacks', async () => {
    const user = userEvent.setup();
    const onToggleIgnored = vi.fn();
    render(
      <PiiPciPanel
        findings={[
          { findingId: 42, filePath: 'a.txt', lineNumber: 1, snippet: 'card', matchType: 'PCI', severity: 'HIGH', ignored: false }
        ]}
        loading={false}
        onDownloadCsv={vi.fn()}
        onDownloadPdf={vi.fn()}
        onToggleIgnored={onToggleIgnored}
        searchQuery=""
      />
    );
    await user.click(screen.getByRole('button', { name: /Ignore/i }));
    expect(onToggleIgnored).toHaveBeenCalledWith(42, true);
  });
});

describe('SnapshotsPanel', () => {
  it('renders snapshot history and diff controls', () => {
    render(
      <SnapshotsPanel
        snapshots={[
          { snapshotId: 2, branchName: 'main', commitHash: 'abc', createdAt: '2024-05-01T00:00:00Z' },
          { snapshotId: 1, branchName: 'main', commitHash: 'def', createdAt: '2024-04-30T00:00:00Z' }
        ]}
        loading={false}
        error={null}
        onRefresh={vi.fn()}
        selectedBase={2}
        selectedCompare={1}
        onSelectBase={vi.fn()}
        onSelectCompare={vi.fn()}
        onDiff={vi.fn()}
        diff={{
          baseSnapshotId: 2,
          compareSnapshotId: 1,
          addedClasses: [{ fullyQualifiedName: 'com.demo.New', stereotype: 'SERVICE' }],
          removedClasses: [],
          addedEndpoints: [],
          removedEndpoints: [],
          addedEntities: [],
          removedEntities: []
        }}
      />
    );
    expect(screen.getByText(/Snapshot History/i)).toBeInTheDocument();
    expect(screen.getAllByText(/abc/i).length).toBeGreaterThan(0);
  });

  it('surfaces errors and loading hints', () => {
    render(
      <SnapshotsPanel
        snapshots={[]}
        loading
        error="Failed to load"
        onRefresh={vi.fn()}
        selectedBase={null}
        selectedCompare={null}
        onSelectBase={vi.fn()}
        onSelectCompare={vi.fn()}
        onDiff={vi.fn()}
        diff={null}
      />
    );
    expect(screen.getByText(/Failed to load/i)).toBeInTheDocument();
    expect(screen.getByText(/Loading snapshots/i)).toBeInTheDocument();
  });
});

describe('DiagramsPanel', () => {
  it('renders diagram list and toggles source visibility', async () => {
    const user = userEvent.setup();
    const diagramsByType = {
      CLASS: [{ diagramId: 1, title: 'Class Diagram', metadata: {}, svgAvailable: false }],
      SEQUENCE: [
        {
          diagramId: 2,
          title: 'Flow Internal',
          metadata: { includeExternal: false, pathOrOperation: '/demo' },
          svgAvailable: true,
          plantumlSource: '@startuml@enduml',
          mermaidSource: 'sequenceDiagram'
        },
        {
          diagramId: 3,
          title: 'Flow External',
          metadata: { includeExternal: true },
          svgAvailable: true,
          plantumlSource: '@startuml@enduml',
          mermaidSource: 'sequenceDiagram'
        }
      ]
    };

    render(
      <DiagramsPanel
        diagramsByType={diagramsByType}
        loading={false}
        activeType="SEQUENCE"
        onTypeChange={vi.fn()}
        activeDiagram={diagramsByType.SEQUENCE[0]}
        onSelectDiagram={vi.fn()}
        svgContent={{ 2: '<svg>mock</svg>' }}
        onDownloadSvg={vi.fn()}
        sequenceIncludeExternal={false}
        onSequenceToggle={vi.fn()}
      />
    );

    expect(screen.getAllByText(/Flow Internal/)).not.toHaveLength(0);
    await act(async () => {
      await user.click(screen.getByRole('button', { name: /View PlantUML/i }));
    });
    expect(screen.getByText(/@startuml/i)).toBeInTheDocument();
    await act(async () => {
      await user.click(screen.getByRole('button', { name: /View Mermaid/i }));
    });
    expect(screen.getByText(/sequenceDiagram/i)).toBeInTheDocument();
  });

  it('shows helpful empty state when no diagrams exist', () => {
    render(
      <DiagramsPanel
        diagramsByType={{ CLASS: [] }}
        loading={false}
        activeType="CLASS"
        onTypeChange={vi.fn()}
        activeDiagram={null}
        onSelectDiagram={vi.fn()}
        svgContent={{}}
        onDownloadSvg={vi.fn()}
        sequenceIncludeExternal={false}
        onSequenceToggle={vi.fn()}
      />
    );
    expect(screen.getByText(/No diagrams available/i)).toBeInTheDocument();
  });
});

describe('GherkinPanel', () => {
  it('shows loading copy while fetching features', () => {
    render(<GherkinPanel features={[]} loading />);
    expect(screen.getByText(/Loading scenarios/i)).toBeInTheDocument();
  });

  it('shows empty explanation when no features are present', () => {
    render(<GherkinPanel features={[]} loading={false} />);
    expect(screen.getByText(/No \.feature files/i)).toBeInTheDocument();
  });

  it('renders captured scenarios and steps', () => {
    const features = [
      {
        featureTitle: 'User Login',
        featureFile: 'features/login.feature',
        scenarios: [
          { name: 'Happy path', scenarioType: 'SCENARIO', steps: ['Given a valid user', 'When credentials are submitted'] }
        ]
      }
    ];
    render(<GherkinPanel features={features} loading={false} />);
    expect(screen.getByText(/User Login/)).toBeInTheDocument();
    expect(screen.getByText(/Happy path/)).toBeInTheDocument();
    expect(screen.getByText(/Given a valid user/)).toBeInTheDocument();
  });
});

describe('MetadataPanel', () => {
  it('shows loading indicator while metadata loads', () => {
    render(<MetadataPanel metadata={null} loading />);
    expect(screen.getByText(/Collecting metadata artifacts/i)).toBeInTheDocument();
  });

  it('prompts to run analysis if metadata missing', () => {
    render(<MetadataPanel metadata={null} loading={false} />);
    expect(screen.getByText(/Switch to this tab after running an analysis/i)).toBeInTheDocument();
  });

  it('surfaces OpenAPI and WSDL artifacts', () => {
    const metadata = {
      projectName: 'demo',
      analyzedAt: '2024-05-01T00:00:00Z',
      snapshotDownloadUrl: '/project/1/export/snapshot',
      metadataDump: {
        openApiSpecs: [{ fileName: 'openapi.yaml', content: 'openapi: 3.0.0' }],
        wsdlDocuments: [{ fileName: 'legacy.wsdl', content: '<definitions />' }],
        xsdDocuments: [],
        soapServices: []
      }
    };
    render(<MetadataPanel metadata={metadata} loading={false} />);
    expect(screen.getByText(/openapi\.yaml/i)).toBeInTheDocument();
    expect(screen.getByText(/legacy\.wsdl/i)).toBeInTheDocument();
    expect(screen.getByText(metadata.snapshotDownloadUrl)).toBeInTheDocument();
  });
});

describe('ExportPanel', () => {
  it('disables download buttons until a project is available', () => {
    render(
      <ExportPanel
        projectId={null}
        onDownloadHtml={vi.fn()}
        onDownloadSnapshot={vi.fn()}
        htmlPreview=""
        loading
        onRefreshPreview={vi.fn()}
      />
    );
    expect(screen.getByRole('button', { name: /Download HTML/i })).toBeDisabled();
    expect(screen.getByRole('button', { name: /Download Snapshot JSON/i })).toBeDisabled();
    expect(screen.getByRole('button', { name: /Refresh preview/i })).toBeDisabled();
    expect(screen.getByText(/Loading export preview/i)).toBeInTheDocument();
  });
  it('triggers download and refresh callbacks', async () => {
    const user = userEvent.setup();
    const onDownloadHtml = vi.fn();
    const onDownloadSnapshot = vi.fn();
    const onRefreshPreview = vi.fn();
    render(
      <ExportPanel
        projectId={123}
        onDownloadHtml={onDownloadHtml}
        onDownloadSnapshot={onDownloadSnapshot}
        htmlPreview=""
        loading={false}
        onRefreshPreview={onRefreshPreview}
      />
    );
    await user.click(screen.getByRole('button', { name: /Download HTML/i }));
    await user.click(screen.getByRole('button', { name: /Download Snapshot JSON/i }));
    await user.click(screen.getByRole('button', { name: /Refresh preview/i }));
    expect(onDownloadHtml).toHaveBeenCalled();
    expect(onDownloadSnapshot).toHaveBeenCalled();
    expect(onRefreshPreview).toHaveBeenCalled();
    expect(screen.getByText(/Run an analysis and refresh to view the Confluence-ready HTML/i)).toBeInTheDocument();
  });
});

describe('CompiledAnalysisPanel', () => {
  it('renders compiled stats, export links, tables, and ERD source', async () => {
    const user = userEvent.setup();
    const downloadMock = vi.fn();
    render(
      <CompiledAnalysisPanel
        analysis={{
          status: 'SUCCEEDED',
          entityCount: 3,
          endpointCount: 2,
          dependencyCount: 5,
          sequenceCount: 1,
          outputDirectory: '/tmp/out'
        }}
        exports={[{ name: 'entities.csv', size: 256 }]}
        entities={{ items: [{ className: 'CustomerEntity', tableName: 'customers', origin: 'JPA', inCycle: true }] }}
        sequences={{ items: [{ generatorName: 'custGen', sequenceName: 'cust_seq', allocationSize: 50, initialValue: 1 }] }}
        endpoints={{ items: [{ httpMethod: 'GET', path: '/customers', controllerClass: 'CustomerController', framework: 'Spring' }] }}
        mermaidSource="erDiagram"
        loading={false}
        error={null}
        onDownloadExport={downloadMock}
        onRefresh={vi.fn()}
      />
    );
    expect(screen.getByText(/Compiled Analysis/i)).toBeInTheDocument();
    expect(screen.getByText(/Status:/i)).toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: /Download/i }));
    expect(downloadMock).toHaveBeenCalledWith({ name: 'entities.csv', size: 256 });
    expect(screen.getByText(/erDiagram/i)).toBeInTheDocument();
  });

  it('shows loading state, empty tables, and error banner', () => {
    render(
      <CompiledAnalysisPanel
        analysis={null}
        exports={[]}
        entities={{ items: [] }}
        sequences={{ items: [] }}
        endpoints={{ items: [] }}
        mermaidSource=""
        loading
        error="Unable to fetch compiled analysis"
        onDownloadExport={vi.fn()}
        onRefresh={vi.fn()}
      />
    );
    expect(screen.getByText(/Unable to fetch compiled analysis/i)).toBeInTheDocument();
    expect(screen.getByText(/Loading compiled analysis/i)).toBeInTheDocument();
    expect(screen.getByText(/No exports yet/i)).toBeInTheDocument();
  });

  it('encourages running a new analysis when compiled data is missing', () => {
    render(
      <CompiledAnalysisPanel
        analysis={null}
        exports={[]}
        entities={{ items: [] }}
        sequences={{ items: [] }}
        endpoints={{ items: [] }}
        mermaidSource=""
        loading={false}
        error={null}
        onDownloadExport={vi.fn()}
        onRefresh={vi.fn()}
      />
    );
    expect(screen.getByText(/Compiled analysis runs automatically/i)).toBeInTheDocument();
  });
});
