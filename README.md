# CodeVision

CodeVision ingests a Git repository, extracts structural metadata, and surfaces a project overview through a local Spring Boot + React stack. It supports single-module and multi-module Maven repos (even when the root is only an aggregator), fingerprints Maven modules so re-runs only rescan changed directories, and persistently snapshots each run per repo + branch so you can review or diff the entire history.

## Backend (Spring Boot)

The backend lives in [`backend/`](backend/). It now exposes an asynchronous `/analyze` job queue: `POST /analyze` enqueues a background analysis (returning a job ID immediately) and `GET /analyze/{jobId}` lets clients poll progress until the job succeeds or fails. Once the job completes, companion endpoints return the stored data for the UI and integrations:

- `GET /analyze/{jobId}` – fetch the latest status/timestamps/project ID for a queued analysis job.
- `GET /project/{id}/overview` – the latest `ParsedDataResponse`.
- `GET /project/{id}/api-endpoints` – the persisted API catalog (requires the API key when security is enabled).
- `GET /project/{id}/db-analysis` – entities, repositories, and CRUD intent summaries captured during analysis.
- `GET /project/{id}/diagrams` – enumerates every stored diagram (class/component/use-case/ERD/DB/schema/sequence) with PlantUML, Mermaid, metadata, and SVG availability.
- `GET /project/{id}/diagram/{diagramId}/svg` – streams the rendered SVG for a specific diagram when available.
- `GET /project/{id}/snapshots` – branch-aware history (ID, branch, commit hash, captured timestamp, fingerprints).
- `GET /project/{id}/snapshots/{snapshotId}/diff/{compareSnapshotId}` – diff payload describing added/removed classes, endpoints, and DB entities between any two snapshots.
- `PATCH /project/{id}/pii-pci/{findingId}` – toggle the ignore flag on a PCI/PII finding (used by the Logger + PCI panels).

### Prerequisites

- Java 21+
- Maven 3.9+

### Configuration

Set the desired credentials in [`backend/api/src/main/resources/application.yml`](backend/api/src/main/resources/application.yml) or through environment variables (`SPRING_*`, `GIT_*`, etc.). The repo ships with a `.env.example`; copy it to `.env` so Spring Boot automatically reads the datasource configuration when you run `mvn` locally.

```yaml
git:
  auth:
    username: your-username
    token: your-token
security:
  apiKey: your-api-key
diagram:
  storage:
    root: ./data/diagrams
  svg:
    enabled: true
```

If you do not need authenticated cloning, leave the values blank. Update the `security.apiKey` to protect API calls. When the key is blank, the API key filter is effectively disabled for development.
`diagram.storage.root` controls where rendered SVG assets are cached; the directory is created automatically if it does not exist.
Set `diagram.svg.enabled` to `false` (or the environment variable `DIAGRAM_SVG_ENABLED=false`) if you are deploying to a constrained runtime that cannot spare extra Metaspace for PlantUML rendering; the app will still persist PlantUML/Mermaid sources but will skip SVG generation.

> **Tip:** `diagram.svg.enabled=false` disables PlantUML SVG rendering (useful for constrained hosts) while still persisting PlantUML/Mermaid sources.

### Database setup

- **Runtime profile (PostgreSQL):** `application.yml` defaults to `jdbc:postgresql://localhost:5432/codevision`, but every property can be overridden via env vars. Point the service at the managed Render instance by exporting:

  ```bash
  export SPRING_DATASOURCE_URL=jdbc:postgresql://dpg-d480qabipnbc73d6felg-a.oregon-postgres.render.com:5432/codevision_postgres
  export SPRING_DATASOURCE_USERNAME=codevision_postgres_user
  export SPRING_DATASOURCE_PASSWORD='N7f455H9K9YZxicckzibPgLF29fHU4h3'
  export SPRING_DATASOURCE_MAX_POOL_SIZE=5
  export SPRING_DATASOURCE_MIN_IDLE=1
  ```

  These match the values documented in [`docs/render-postgres.md`](docs/render-postgres.md); switch to the internal hostname when running inside Render. Checking `env | grep SPRING_DATASOURCE` before launching ensures the app never tries to open `localhost:5432`.

- **Test profile (in-memory H2):** Repository and service tests activate the `test` profile via `@ActiveProfiles("test")`. The matching [`application-test.yml`](backend/api/src/test/resources/application-test.yml) connects to H2 in PostgreSQL compatibility mode and disables Hibernate DDL so we can control the schema. `ProjectSnapshotServiceTest` and related slices execute [`schema-h2.sql`](backend/api/src/test/resources/schema-h2.sql) before every method to create the lightweight tables they need. When you add a new JPA entity that participates in slice or repository tests, mirror its table definition in that script (including foreign keys) so H2 stays in sync with the production schema.

- **Maven repo location:** This environment blocks remote downloads and guards `~/.m2`. When running tests locally, either fix your `.m2` permissions or pass `-Dmaven.repo.local=/path/you/own` so Maven can cache dependencies without elevated privileges.

### Run the backend

```bash
# from repository root
mvn -f backend/pom.xml spring-boot:run
```

The service starts on `http://localhost:8080`.

### Run the backend tests

Repository and slice tests run entirely against the embedded H2 database. Invoke them from the repo root:

```bash
SPRING_PROFILES_ACTIVE=test mvn -f backend/pom.xml -pl api test
```

Surefire will pick up `application-test.yml`, connect to the in-memory datasource, and execute `schema-h2.sql` ahead of each method so tables such as `project`, `project_snapshot`, and `class_metadata` exist before the repositories run. If you introduce a new table that participates in these tests, mirror the DDL in that script—H2 runs in PostgreSQL compatibility mode, so most DDL can be copied directly from your migration.

> **Graphviz/PlantUML note:** `DiagramSvgRendererTest` shells out to PlantUML/Graphviz and can crash the forked JVM on macOS when the underlying native binaries are missing or unstable. If you hit `Abort trap: 6` while running the suite locally, re-run the command with `-Dtest=!DiagramSvgRendererTest` until Graphviz is installed, or point `PLANTUML_LIMIT_SIZE` to a smaller value so the renderer stays within local memory limits.

### Package the React frontend into the backend

Running `mvn -f backend/pom.xml -pl api -am package` now:

- Executes `npm install` and `npm run build` inside `frontend/`.
- Copies the generated Vite `dist/` bundle (including `index.html` and hashed assets) into `backend/api/target/classes/static`, which is the Spring Boot resources folder.
- Bakes the SPA into the runnable jar/containers so hitting `http://localhost:8080/` serves the React UI and delegates API calls to the same origin.

This requires Node.js 18+ and npm 10+ to be available on your PATH; the Docker build installs them automatically, but local Maven builds should have Node installed as well.

Pass `-Dskip.frontend=true` if you only want to build the backend (for example, while iterating on ingestion logic).

### Run the backend with Docker

Build the container image defined in [`Dockerfile`](Dockerfile); it installs Node.js 18, builds the React bundle, and bakes the static assets into the runnable jar. When you run the container you must provide the Postgres connection details (either pointing at Render or at the local Compose service):

```bash
# (optional) start the local database defined in docker-compose.yml
docker compose up -d postgres

# build + run the backend image
docker build -t codevision-backend .
docker run --rm -p 8080:8080 \
  --env-file .env \
  -e SECURITY_APIKEY=your-api-key \
  codevision-backend
```

Using `--env-file .env` keeps the container’s datasource settings in sync with your local runs. To target Render, either edit `.env` or pass the three datasource variables explicitly:

```bash
docker run --rm -p 8080:8080 \
  -e SPRING_DATASOURCE_URL=jdbc:postgresql://... \
  -e SPRING_DATASOURCE_USERNAME=... \
  -e SPRING_DATASOURCE_PASSWORD=... \
  codevision-backend
```

Every property in `application.yml` can be overridden the same way (for example `-e GIT_AUTH_USERNAME=...`).

### Analyze API (`POST /analyze` + `GET /analyze/{jobId}`)

1. **Enqueue a job (`POST /analyze`)**
   - Headers:
     - `Content-Type: application/json`
     - `X-API-KEY: <value>` (required only when `security.apiKey` is populated)
   - Request body:

     Include `branchName` to analyze a non-default branch; if omitted, `main` is assumed.

     ```json
     {
       "repoUrl": "https://github.com/org/repo.git",
       "branchName": "release/1.1"
     }
     ```

   - Response body:

     ```json
     {
       "jobId": "27981d4a-fadc-45dc-872d-f5fa6f9a531a",
       "repoUrl": "https://github.com/org/repo.git",
       "status": "QUEUED",
       "statusMessage": "Queued for analysis",
       "createdAt": "2025-03-01T15:32:11.034Z"
     }
     ```

2. **Poll for completion (`GET /analyze/{jobId}`)**
   - Response body when the job succeeds:

     ```json
     {
       "jobId": "27981d4a-fadc-45dc-872d-f5fa6f9a531a",
       "repoUrl": "https://github.com/org/repo.git",
       "branchName": "release/1.1",
       "commitHash": "4d9cf4fd8a601d2a2ef7d7cb7e83c9a02c3a3d02",
       "snapshotId": 42,
       "status": "SUCCEEDED",
       "statusMessage": "Snapshot saved",
       "projectId": 1,
       "createdAt": "2025-03-01T15:32:11.034Z",
       "startedAt": "2025-03-01T15:32:12.500Z",
       "completedAt": "2025-03-01T15:33:01.207Z"
     }
     ```

   - Failed jobs use the same schema but report `status: "FAILED"` and populate `errorMessage`.

### Overview API (`GET /project/{id}/overview`)

- Headers: include `X-API-KEY` when configured.
- Response body (truncated example):

  ```json
  {
    "projectId": 1,
    "projectName": "spring-petclinic",
    "repoUrl": "https://github.com/spring-projects/spring-petclinic",
    "analyzedAt": "2025-10-26T20:40:28.123-04:00",
    "buildInfo": {
      "groupId": "org.springframework.samples",
      "artifactId": "spring-petclinic",
      "version": "4.0.0-SNAPSHOT",
      "javaVersion": "25"
    },
    "classes": [
      {
        "fullyQualifiedName": "org.springframework.samples.petclinic.owner.OwnerController",
        "stereotype": "CONTROLLER",
        "sourceSet": "MAIN",
        "userCode": false
      }
    ],
    "metadataDump": {
      "openApiSpecs": []
    }
  }
  ```

## Frontend (React + Vite)

The frontend lives in [`frontend/`](frontend/). It provides a single-page workflow to submit repository URLs and, after analysis completes, renders the project overview (build metadata, class totals, OpenAPI summaries).

### Structure

- `src/components/panels` – feature-specific panels (Overview, API, Database, Logger, PCI/PII, Diagrams, Gherkin, Metadata, Export) that stay presentation-only.
- `src/components/search` – the global search bar/results tray and class directory implementations that share styling/interaction patterns.
- `src/components/progress` – analyzer timeline + progress widgets shared across cards.
- `src/hooks` – shared hooks like `useMediaQuery` so layout logic is reusable.
- `src/utils` – formatting helpers, constants, and friendly error builders that keep `App.jsx` focused on orchestration.

### Overview experience

- Analyzer card auto-collapses into a "Latest analysis" summary after a successful run (and automatically hides on compact screens), exposing project ID, repo URL, analyzed timestamp, and quick actions (Edit inputs, View dashboard, Hide/Show panel). Errors pop the form back open so inputs stay editable.
- The analyze form now includes a Branch input (default `main`); values are normalized and echoed back in the summary so it’s obvious whether you’re inspecting `main`, `release/1.1`, etc.
- A deterministic analysis timeline now includes a live progress bar that shows how far along the analyzer is across `Cloning`, `Overview`, `API`, `Database`, `Logger`, `PCI / PII`, and `Diagrams`, with `Queued / In progress… / Done / Skipped` states.
- Global search sits above the tab rail; it streams filtered results across classes, endpoints, log statements, and sensitive-data findings and pipes the term into the Overview/API/Logger/PII tabs so they filter in-place.
- The tab rail stays sticky on desktop/tablet, exposes a `<select>` fallback on narrow screens, and now ships with ARIA roles + keyboard navigation so every panel is reachable without horizontal scrolling.
- Overview panel now highlights build coordinates, Java version, and external service counts (OpenAPI, SOAP/WSDL, XSD) alongside the class coverage grid. The class directory is capped at a scrollable height so it stays usable even with large projects.
- API specs, database, OpenAPI/WSDL viewers, media inventory, and diagrams behave as before, but tables reuse the global search filters so you can jump directly to the insights you care about.
- Errors surface as actionable banners that explain the failure (invalid URL, clone/auth issues, timeouts, out-of-memory) and include remediation tips (verify credentials, limit repository size, retry during quieter hours).

### Snapshots panel

- A dedicated Snapshots tab lists every stored snapshot (ID, branch, commit hash, captured timestamp, module count) and paginates through long histories without hammering the API.
- Baseline/Comparison selectors default to “Latest” vs. “Previous” but allow any two versions (even cross-branch). The panel renders an immediate diff summary (e.g., “Classes: +3 / −0”) and links to raw JSON.
- Each row exposes export buttons for the snapshot JSON plus CSV/PDF downloads sized for auditors. Filters cover branch names, partial commit hashes, and captured date ranges for quick targeting.

### Logger & PCI panels

- Logger Insights adds message-level search, class filter presets, and “Expand All / Collapse All” toggles so large log sets remain scannable. The default class filter is empty now (no more stale `com.example.OrderService` placeholder).
- PCI/PII Scan hides ignored rows by default, provides inline Ignore/Restore buttons (backed by `PATCH /project/{id}/pii-pci/{findingId}`), and keeps the tables/exports in sync after every toggle.
- CSV/PDF exports for both panels stream directly from the backend so they still work on repositories with thousands of entries, and they mirror whatever filters are applied in the UI.

### Prerequisites

- Node.js 18+

### Install dependencies

```bash
cd frontend
npm install
```

### Run the frontend dev server

```bash
npm run dev
```

The app is served on `http://localhost:5173` and proxies both `/analyze` and `/project/**` calls to the backend.

### Build for production

```bash
npm run build
```

### Deploy to Vercel

The repo root now includes a [`vercel.json`](vercel.json) that tells Vercel to install dependencies, build, and serve the Vite frontend that lives under `frontend/`. Deploy by connecting the GitHub repository in the Vercel dashboard—no manual root-directory overrides are required.

Because the Spring Boot backend cannot run on Vercel, configure the frontend to talk to wherever the backend is hosted by setting a `VITE_API_BASE_URL` environment variable in the Vercel project settings (e.g., `https://your-backend.example.com`). The build step will bake this base URL into the static bundle so API calls reach the correct origin. For local development, copy [`frontend/.env.example`](frontend/.env.example) to `.env` and set the same variable if you are not running the backend on `localhost:8080`.

## Iteration Documentation

- Iteration 1 summary: [`docs/iteration-1-completion.md`](docs/iteration-1-completion.md)
- Iteration 2 summary: [`docs/iteration-2-completion.md`](docs/iteration-2-completion.md)
- Iteration 3 summary: [`docs/iteration-3-completion.md`](docs/iteration-3-completion.md)
- Iteration 4 summary: [`docs/iteration-4-completion.md`](docs/iteration-4-completion.md)
- Iteration 5 summary: [`docs/iteration-5-completion.md`](docs/iteration-5-completion.md)
- Iteration 6 summary: [`docs/iteration-6-completion.md`](docs/iteration-6-completion.md)
- Iteration 7 summary: [`docs/iteration-7-completion.md`](docs/iteration-7-completion.md)
- Iteration 8 summary: [`docs/iteration-8-completion.md`](docs/iteration-8-completion.md)
- Iteration 10 summary: [`docs/iteration-10-completion.md`](docs/iteration-10-completion.md)
- Iteration 11 summary: [`docs/iteration-11-completion.md`](docs/iteration-11-completion.md)

## Database (PostgreSQL)

CodeVision now persists all analysis results in PostgreSQL so history survives Render restarts. The backend reads its datasource settings from the standard Spring variables (`SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, etc.) and defaults them to the local Docker values.

### Local workflow

1. Copy the sample env file: `cp .env.example .env`. By default it points at the managed Render instance so your laptop and Render share the same data set. Spring Boot automatically loads this file when you run `mvn spring-boot:run`.
2. If you prefer to run Postgres locally, uncomment the “local Docker” block in `.env` and start the container: `docker compose up -d postgres`.
3. Run `mvn -f backend/pom.xml spring-boot:run` and analyze repos as usual. When using the managed instance, your local runs immediately populate the Render database.

### Render / production

Point `SPRING_DATASOURCE_URL` at the managed Render Postgres connection string (e.g., `jdbc:postgresql://${RENDER_DB_HOST}:${RENDER_DB_PORT}/${RENDER_DB_NAME}`) and set the matching username/password. Set `SPRING_DATASOURCE_MAX_POOL_SIZE` and `SPRING_DATASOURCE_MIN_IDLE` to align with your plan’s connection limits. No filesystem persistence is required anymore.

For the Render instance described in `docs/render-postgres.md`, the variables would be:

```bash
SPRING_DATASOURCE_URL=jdbc:postgresql://dpg-d480qabipnbc73d6felg-a.oregon-postgres.render.com:5432/codevision_postgres
SPRING_DATASOURCE_USERNAME=codevision_postgres_user
SPRING_DATASOURCE_PASSWORD=<Render-provided password>
SPRING_DATASOURCE_MAX_POOL_SIZE=5   # or any limit below your plan’s max connections
SPRING_DATASOURCE_MIN_IDLE=1
```

Use the internal hostname (`dpg-d480qabipnbc73d6felg-a`) when wiring another Render service in the same region to avoid egress charges; the external hostname (ending in `.oregon-postgres.render.com`) works everywhere else (local laptops, CI).

After the schema has been created (either by running locally with `SPRING_JPA_HIBERNATE_DDL_AUTO=update` once or by applying the SQL in [`backend/api/src/test/resources/schema-h2.sql`](backend/api/src/test/resources/schema-h2.sql)), set `SPRING_JPA_HIBERNATE_DDL_AUTO=validate` or `none` in Render so production deploys never attempt destructive DDL.

### Schema

- `project` – canonical project record keyed by `(repo_url, branch_name)` plus derived build metadata and timestamps.
- `analysis_job` – asynchronous `/analyze` queue with repo URL, branch, status/error fields, resolved commit hash, and the resulting `snapshot_id`.
- `project_snapshot` – append-only history including branch, commit hash, module fingerprints, captured timestamp, and the serialized `ParsedDataResponse`.
- `class_metadata` – flattened Java class inventory per project (FQN, stereotype, package, source set, annotations, interfaces, relative path, userCode flag).
- `api_endpoint` – persisted REST/SOAP/legacy endpoint catalog.
- `asset_image` – discovered documentation assets (path, size, hash).
- `db_entity` – extracted JPA entity metadata (tables, PKs, relationships) with JSON blobs for fields/relationships.
- `dao_operation` – classified DAO/repository operations with inferred CRUD intent.
- `diagram` – stored diagram definitions (type, title, PlantUML/Mermaid sources, SVG path, metadata JSON).
- `log_statement` – every logger call plus level, template, variables, and PCI/PII flags.
- `pii_pci_finding` – sensitive-data hits with path/line/severity plus the `ignored` flag toggled via the REST API.

Re-running `/analyze` with the same repo + branch simply enqueues a new job; once that job reaches `SUCCEEDED`, the backend overwrites the structured tables for that tuple and appends a new snapshot (unless the commit already exists, in which case it reuses the cached snapshot).

### Migrating legacy H2 data

If you previously ran CodeVision against the embedded H2 files, run the migration helper once to push that history into the managed Postgres instance:

```bash
# ensure the destination DB variables point at Render
export PG_URL=jdbc:postgresql://dpg-d480qabipnbc73d6felg-a.oregon-postgres.render.com:5432/codevision_postgres
export PG_USER=codevision_postgres_user
export PG_PASSWORD='N7f455H9K9YZxicckzibPgLF29fHU4h3'

# pull the H2 jar if you don't already have it
mvn dependency:get -Dartifact=com.h2database:h2:2.2.224

# run the migrator (idempotent; uses ON CONFLICT DO NOTHING)
scripts/migration/run-h2-to-postgres.sh
```

`scripts/migration/H2ToPostgresMigrator.java` reads the on-disk H2 database, inserts every row into Postgres while preserving primary keys, and then bumps each identity sequence so new analyses continue incrementing correctly. After it succeeds you can delete `backend/api/data/codevision.mv.db`.

### Testing

Backend tests now target PostgreSQL via Testcontainers. Make sure Docker is reachable before running:

```bash
mvn -f backend/pom.xml test
```

If Docker (or outgoing network access for Testcontainers images) is unavailable, the suite will fail to start.
