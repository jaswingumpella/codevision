# Iteration 21 Completion – End-to-end & Regression Tests

## Summary

Iteration 21 adds the missing end-to-end safety net so the full “analyze → dashboards → compiled analysis exports” workflow is continuously exercised. A Playwright harness now orchestrates the backend and frontend against the deterministic fixture repo, drives the UI headlessly, enforces a performance SLA, and validates every compiled artifact via SHA-256 hashes. A lightweight `/healthz` endpoint and dedicated documentation round out the work so contributors can reproduce the checks locally.

## Key Deliverables

- **Playwright infrastructure** that prepares a Git-initialized copy of the compiled fixture, boots the backend on H2 (port `8090`) and the Vite dev server (`4173`), submits an analysis request, verifies the Overview/Compiled tabs, and fails if the run exceeds 120 s.
- **Regression hash guard** backed by `frontend/e2e/regression-hashes.json`; the suite downloads every export (`analysis.json`, CSVs, PlantUML/Mermaid artifacts, sequence diagrams) and compares their SHA-256 hashes to the canonical fixture versions. `CompiledFixtureSnapshotTest` can be enabled on demand to reprint hashes when fixtures evolve.
- **Operational niceties** including a `GET /healthz` readiness endpoint for harnesses, `.codevision-e2e/` workspace isolation, and README/Frontend guide updates covering `npm run test:unit`, `npm run test:e2e`, and required Playwright installs.

## Verification

- `cd frontend && npm run test:unit`
- `cd frontend && npm run test:coverage`
- `cd frontend && npm run test:e2e` (requires the ability to bind to localhost ports and a previously installed Playwright browser: `npx playwright install --with-deps chromium`)
