import { describe, expect, it, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
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
  ExportPanel
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
});

describe('LoggerInsightsPanel', () => {
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

    await user.type(screen.getByLabelText(/Class Filter/i), 'Bar');
    await user.selectOptions(screen.getByLabelText(/^Level$/i), 'ERROR');
    await user.click(screen.getByLabelText(/Only PII risk/i));
    expect(screen.getByText('com.demo.Bar')).toBeInTheDocument();
    expect(screen.queryByText('com.demo.Foo')).not.toBeInTheDocument();
  });
});

describe('PiiPciPanel', () => {
  it('applies filters and hide ignored toggle', async () => {
    const user = userEvent.setup();
    const findings = [
      { filePath: 'a.txt', lineNumber: 1, snippet: 'card', matchType: 'PCI', severity: 'HIGH', ignored: false },
      { filePath: 'b.txt', lineNumber: 2, snippet: 'email', matchType: 'PII', severity: 'LOW', ignored: true }
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

    await user.selectOptions(screen.getByLabelText(/Match Type/i), 'PCI');
    await user.selectOptions(screen.getByLabelText(/Severity/i), 'HIGH');
    await user.click(screen.getByLabelText(/Hide ignored/i));
    expect(screen.getByText('a.txt')).toBeInTheDocument();
    expect(screen.queryByText('b.txt')).not.toBeInTheDocument();
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
    await user.click(screen.getByRole('button', { name: /View PlantUML/i }));
    expect(screen.getByText(/@startuml/i)).toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: /View Mermaid/i }));
    expect(screen.getByText(/sequenceDiagram/i)).toBeInTheDocument();
  });
});

describe('GherkinPanel', () => {
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
    await user.click(screen.getByRole('button', { name: /Download Project HTML/i }));
    await user.click(screen.getByRole('button', { name: /Download ParsedDataResponse\.json/i }));
    await user.click(screen.getByRole('button', { name: /Refresh Preview/i }));
    expect(onDownloadHtml).toHaveBeenCalled();
    expect(onDownloadSnapshot).toHaveBeenCalled();
    expect(onRefreshPreview).toHaveBeenCalled();
    expect(screen.getByText(/Preview will appear here/i)).toBeInTheDocument();
  });
});
