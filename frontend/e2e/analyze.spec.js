import { test, expect } from '@playwright/test';
import fs from 'node:fs';
import path from 'node:path';
import crypto from 'node:crypto';

const hashesPath = path.resolve(__dirname, 'regression-hashes.json');
const regressionHashes = JSON.parse(fs.readFileSync(hashesPath, 'utf-8'));
const repoUrl = process.env.CODEVISION_E2E_REPO_URL;
const branchName = process.env.CODEVISION_E2E_BRANCH || 'main';
const backendBase = process.env.CODEVISION_E2E_BACKEND_BASE || 'http://127.0.0.1:8090';
const slaMs = Number(process.env.CODEVISION_E2E_SLA_MS || 120000);

test.describe('Analyzer end-to-end flow', () => {
  test('runs analyzer and validates compiled exports', async ({ page, request }) => {
    test.slow();
    if (!repoUrl) {
      throw new Error('CODEVISION_E2E_REPO_URL is not available – fixture preparation failed.');
    }

    await page.goto('/');
    await page.getByLabel('Repository URL').fill(repoUrl);
    const branchField = page.getByLabel('Branch');
    await branchField.fill('');
    await branchField.fill(branchName);

    const overviewResponsePromise = page.waitForResponse((response) => {
      try {
        const url = new URL(response.url());
        return (
          url.pathname.endsWith('/overview') &&
          url.pathname.includes('/project/') &&
          response.status() === 200
        );
      } catch {
        return false;
      }
    });

    const analyzeStart = Date.now();
    await page.getByRole('button', { name: /Analyze/i }).click();
    const overviewResponse = await overviewResponsePromise;
    const overviewData = await overviewResponse.json();
    const overviewUrl = new URL(overviewResponse.url());
    const projectIdMatch = overviewUrl.pathname.match(/\/project\/(\d+)\/overview$/);
    expect(projectIdMatch).not.toBeNull();
    const projectId = Number(projectIdMatch[1]);

    const runtimeMs = Date.now() - analyzeStart;
    expect(
      runtimeMs,
      `analysis runtime ${runtimeMs}ms exceeded SLA ${slaMs}ms`
    ).toBeLessThanOrEqual(slaMs);

    await expect(page.getByRole('heading', { name: overviewData.projectName })).toBeVisible();
    await expect(page.getByText(overviewData.repoUrl)).toBeVisible();

    await page.getByRole('tab', { name: 'Compiled Analysis' }).click();
    await expect(page.getByRole('heading', { name: 'Compiled Analysis' })).toBeVisible();
    await expect(page.getByText(/Status:\s+SUCCEEDED/)).toBeVisible();
    await expect(page.getByRole('cell', { name: /FixtureEntity/ })).toBeVisible();

    const compiledResponse = await request.get(`${backendBase}/project/${projectId}/compiled-analysis`);
    expect(compiledResponse.ok()).toBeTruthy();
    const compiledJson = await compiledResponse.json();
    const exportsList = compiledJson.exports || [];

    for (const [fileName, expectedHash] of Object.entries(regressionHashes.exports)) {
      const exportFile = exportsList.find((file) => file.name === fileName);
      expect(exportFile, `Missing export ${fileName}`).toBeTruthy();

      const artifactResponse = await request.get(exportFile.downloadUrl);
      expect(artifactResponse.ok(), `Failed to download ${fileName}`).toBeTruthy();
      const buffer = await artifactResponse.body();
      const digest = crypto.createHash('sha256').update(buffer).digest('hex');
      expect(digest, `${fileName} hash mismatch`).toBe(expectedHash);
    }
  });
});
