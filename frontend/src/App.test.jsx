import { render, screen, waitFor, act } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import App from './App';
import axios from './lib/apiClient';

vi.mock('./lib/apiClient', () => ({
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
        { repoUrl: 'https://example.com/org/repo.git', branchName: 'feature/login' },
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
});
