import { test, expect } from '@playwright/test';
const repoUrl = process.env.CODEVISION_E2E_REPO_URL;
const branchName = process.env.CODEVISION_E2E_BRANCH || 'main';
const slaMs = Number(process.env.CODEVISION_E2E_SLA_MS || 120000);

test.describe('Analyzer end-to-end flow', () => {
  test('runs analyzer and validates dashboard tabs', async ({ page }) => {
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

    const runtimeMs = Date.now() - analyzeStart;
    expect(
      runtimeMs,
      `analysis runtime ${runtimeMs}ms exceeded SLA ${slaMs}ms`
    ).toBeLessThanOrEqual(slaMs);

    await expect(page.getByRole('heading', { name: overviewData.projectName })).toBeVisible();
    await expect(page.getByText(overviewData.repoUrl)).toBeVisible();

    await page.getByRole('tab', { name: 'Database' }).click();
    await expect(page.getByRole('heading', { name: 'Database Analysis' })).toBeVisible();
    const entityHeading = page.getByRole('heading', { name: 'Entities and Interacting Classes' });
    await expect(entityHeading).toBeVisible();
    const entityTable = entityHeading.locator('xpath=..').locator('table');
    await expect(entityTable.locator('tbody tr').first()).toBeVisible();

    await page.getByRole('tab', { name: 'Diagrams' }).click();
    await expect(page.getByRole('button', { name: 'Class diagrams' })).toBeVisible();
    await expect(page.locator('.diagram-card').first()).toBeVisible();
  });
});
