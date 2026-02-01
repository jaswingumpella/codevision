# CodeVision Frontend

React + Vite single-page app that drives repository analysis, progress tracking, and every dashboard panel surfaced in CodeVision.

## Prerequisites

- Node.js 18+
- npm 9+

## Environment Variables

The UI defaults to issuing relative requests to `/analyze` and `/project/**`. In hosted deployments (e.g., Vercel) set `VITE_API_BASE_URL` to the fully-qualified backend URL so Axios prefixes every call:

```bash
VITE_API_BASE_URL=https://codevision.example.com
```

Create a `.env` file or configure the variable inside your hosting provider.

## Local Development

```bash
cd frontend
npm install
npm run dev
```

The dev server runs on `http://localhost:5173` and proxies `/analyze` and `/project/**` to `http://localhost:8080` by default. Supply the API key in the Analyzer form when your backend requires it; use the security toggle there to include/exclude logger insights + PCI/PII scans.

## Tests & Coverage

```bash
# unit tests
npm run test:unit

# enforce ≥90% statements/branches/functions/lines using Vitest + V8 coverage
npm run test:coverage
```

Coverage reports are emitted to `frontend/coverage`. The Vitest config blocks the run if any metric drops below 90%.

## End-to-end Workflow Guard

The Playwright suite spins up the backend on an in-memory H2 profile, starts the Vite dev server, drives the “analyze → dashboard → compiled analysis” flow, and validates the exported artifact hashes.

```bash
# one-time browser download
npx playwright install --with-deps chromium
npm run test:e2e
```

The harness copies the deterministic fixture repo into `.codevision-e2e/`, initializes it as a local Git remote, and verifies the SHA-256 hashes listed in `frontend/e2e/regression-hashes.json`. Run the suite in an environment that allows binding to localhost ports 8090 and 4173.
