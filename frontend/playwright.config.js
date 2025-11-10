import { defineConfig, devices } from '@playwright/test';
import fs from 'node:fs';
import path from 'node:path';
import { execSync } from 'node:child_process';
import { fileURLToPath, pathToFileURL } from 'node:url';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);
const repoRoot = path.resolve(__dirname, '..');
const workspaceRoot = path.resolve(repoRoot, '.codevision-e2e');
const fixtureSource = path.resolve(repoRoot, 'backend/api/src/test/resources/fixtures/compiled-app');
const fixtureRepoDir = path.join(workspaceRoot, 'fixture-repo');
const compiledOutputDir = path.join(workspaceRoot, 'compiled-output');
const diagramOutputDir = path.join(workspaceRoot, 'diagram-output');

prepareFixtureRepository();
prepareOutputDirectories();

const repoUrl = pathToFileURL(fixtureRepoDir).toString();
process.env.CODEVISION_E2E_REPO_PATH ??= fixtureRepoDir;
process.env.CODEVISION_E2E_REPO_URL ??= repoUrl;
process.env.CODEVISION_E2E_BRANCH ??= 'main';
process.env.CODEVISION_E2E_BACKEND_BASE ??= 'http://127.0.0.1:8090';
process.env.CODEVISION_E2E_UI_BASE_URL ??= 'http://127.0.0.1:4173';
process.env.CODEVISION_E2E_SLA_MS ??= '120000';

const resolvedBackendBase = process.env.CODEVISION_E2E_BACKEND_BASE;
const backendEnv = {
  SPRING_DATASOURCE_URL:
    'jdbc:h2:mem:codevision-e2e;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE;NON_KEYWORDS=VALUE',
  SPRING_DATASOURCE_DRIVER_CLASS_NAME: 'org.h2.Driver',
  SPRING_DATASOURCE_USERNAME: 'sa',
  SPRING_DATASOURCE_PASSWORD: '',
  SPRING_JPA_HIBERNATE_DDL_AUTO: 'create-drop',
  ANALYSIS_ACCEPT_PACKAGES: 'com.codevision.fixtures',
  ANALYSIS_INCLUDE_DEPENDENCIES: 'false',
  ANALYSIS_COMPILE_AUTO: 'false',
  ANALYSIS_OUTPUT_ROOT: compiledOutputDir,
  ANALYSIS_MAX_RUNTIME_SECONDS: '180',
  DIAGRAM_STORAGE_ROOT: diagramOutputDir,
  DIAGRAM_SVG_ENABLED: 'false',
  SERVER_PORT: '8090'
};

const frontendEnv = {
  VITE_API_BASE_URL: resolvedBackendBase
};

export default defineConfig({
  testDir: './e2e',
  fullyParallel: false,
  timeout: 240_000,
  expect: {
    timeout: 60_000
  },
  use: {
    baseURL: process.env.CODEVISION_E2E_UI_BASE_URL ?? 'http://127.0.0.1:4173',
    headless: true,
    video: 'retain-on-failure',
    trace: 'retain-on-failure'
  },
  webServer: [
    {
      command:
        'mvn spring-boot:run -Dspring-boot.run.profiles=default -Dspring-boot.run.skip=false -DskipTests -Dskip.frontend=true',
      cwd: path.resolve(repoRoot, 'backend/api'),
      env: backendEnv,
      stdout: 'pipe',
      stderr: 'pipe',
      reuseExistingServer: !process.env.CI,
      timeout: 240_000,
      url: `${resolvedBackendBase}/healthz`
    },
    {
      command: 'npm run dev -- --host 127.0.0.1 --port 4173',
      cwd: path.resolve(repoRoot, 'frontend'),
      env: frontendEnv,
      reuseExistingServer: !process.env.CI,
      timeout: 60_000,
      url: process.env.CODEVISION_E2E_UI_BASE_URL ?? 'http://127.0.0.1:4173'
    }
  ],
  projects: [
    {
      name: 'chromium',
      use: { ...devices['Desktop Chrome'] }
    }
  ]
});

function prepareFixtureRepository() {
  fs.rmSync(fixtureRepoDir, { recursive: true, force: true });
  fs.mkdirSync(workspaceRoot, { recursive: true });
  fs.cpSync(fixtureSource, fixtureRepoDir, { recursive: true });
  runGitCommand('init -b main');
  runGitCommand('config user.email "codevision-e2e@example.com"');
  runGitCommand('config user.name "CodeVision E2E"');
  runGitCommand('config commit.gpgsign false');
  runGitCommand('add .');
  runGitCommand('commit -m "Seed fixture"');
}

function prepareOutputDirectories() {
  fs.rmSync(compiledOutputDir, { recursive: true, force: true });
  fs.rmSync(diagramOutputDir, { recursive: true, force: true });
  fs.mkdirSync(compiledOutputDir, { recursive: true });
  fs.mkdirSync(diagramOutputDir, { recursive: true });
}

function runGitCommand(args) {
  try {
    execSync(`git ${args}`, { cwd: fixtureRepoDir, stdio: 'ignore' });
  } catch (error) {
    if (args.startsWith('init -b')) {
      execSync('git init', { cwd: fixtureRepoDir, stdio: 'ignore' });
      execSync('git branch -M main', { cwd: fixtureRepoDir, stdio: 'ignore' });
      return;
    }
    throw error;
  }
}
