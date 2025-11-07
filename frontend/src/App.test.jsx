import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import App from './App';
import axios from 'axios';

vi.mock('axios', () => ({
  __esModule: true,
  default: {
    post: vi.fn(),
    get: vi.fn()
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

describe('App', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('submits analysis request and renders overview details', async () => {
    axios.post.mockResolvedValueOnce({
      data: { projectId: 101, status: 'ANALYZED_METADATA' }
    });
    axios.get
      .mockResolvedValueOnce({ data: mockOverview })
      .mockResolvedValueOnce({ data: { endpoints: [] } })
      .mockResolvedValueOnce({ data: { dbAnalysis: null } })
      .mockResolvedValueOnce({ data: { loggerInsights: [] } })
      .mockResolvedValueOnce({ data: { findings: [] } })
      .mockResolvedValueOnce({ data: { diagrams: [] } });

    render(<App />);

    const repoInput = screen.getByLabelText(/repository url/i);
    const apiKeyInput = screen.getByLabelText(/api key/i);
    const submitButton = screen.getByRole('button', { name: /analyze/i });

    await userEvent.type(repoInput, 'https://example.com/org/repo.git');
    await userEvent.type(apiKeyInput, 'super-secret');
    await userEvent.click(submitButton);

    await waitFor(() =>
      expect(axios.post).toHaveBeenCalledWith(
        '/analyze',
        { repoUrl: 'https://example.com/org/repo.git' },
        {
          headers: {
            'Content-Type': 'application/json',
            'X-API-KEY': 'super-secret'
          }
        }
      )
    );

    await waitFor(() => expect(axios.get).toHaveBeenCalledWith('/project/101/overview', {
      headers: {
        'X-API-KEY': 'super-secret'
      }
    }));

    expect(await screen.findByText(/latest analysis/i)).toBeInTheDocument();
    expect(screen.getByText(/demo-project/)).toBeInTheDocument();
    expect(screen.getByText(/com\.barclays/)).toBeInTheDocument();
    expect(screen.getByText(/openapi\.yml/)).toBeInTheDocument();
  });

  it('shows an error message when analysis fails', async () => {
    axios.post.mockRejectedValueOnce({ response: { data: 'Failed to analyze repository' } });

    render(<App />);

    const repoInput = screen.getByLabelText(/repository url/i);
    await userEvent.type(repoInput, 'https://example.com/org/repo.git');
    await userEvent.click(screen.getByRole('button', { name: /analyze/i }));

    expect(await screen.findByText(/failed to analyze repository/i)).toBeInTheDocument();
  });
});
