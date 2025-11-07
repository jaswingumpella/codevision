# CodeVision

CodeVision ingests a Git repository, extracts structural metadata, and surfaces a project overview through a local Spring Boot + React stack. It supports single-module and multi-module Maven repos (even when the root is only an aggregator) and persistently snapshots the analysis for later viewing.

## Backend (Spring Boot)

The backend lives in [`backend/`](backend/). It exposes a synchronous `/analyze` endpoint that clones a Git repository, extracts build/class/API metadata, and records a snapshot in an H2 database. Companion endpoints return the stored data for the UI and integrations:

- `GET /project/{id}/overview` – the latest `ParsedDataResponse`.
- `GET /project/{id}/api-endpoints` – the persisted API catalog (requires the API key when security is enabled).
- `GET /project/{id}/db-analysis` – entities, repositories, and CRUD intent summaries captured during analysis.
- `GET /project/{id}/diagrams` – enumerates every stored diagram (class/component/use-case/ERD/DB/schema/sequence) with PlantUML, Mermaid, metadata, and SVG availability.
- `GET /project/{id}/diagram/{diagramId}/svg` – streams the rendered SVG for a specific diagram when available.

### Prerequisites

- Java 21+
- Maven 3.9+

### Configuration

Set the desired credentials in [`backend/src/main/resources/application.yml`](backend/src/main/resources/application.yml):

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
```

If you do not need authenticated cloning, leave the values blank. Update the `security.apiKey` to protect API calls. When the key is blank, the API key filter is effectively disabled for development.
`diagram.storage.root` controls where rendered SVG assets are cached; the directory is created automatically if it does not exist.

### Run the backend

```bash
# from repository root
mvn -f backend/pom.xml spring-boot:run
```

The service starts on `http://localhost:8080`.

### Package the React frontend into the backend

Running `mvn -f backend/pom.xml -pl api -am package` now:

- Executes `npm install` and `npm run build` inside `frontend/`.
- Copies the generated Vite `dist/` bundle (including `index.html` and hashed assets) into `backend/api/target/classes/static`, which is the Spring Boot resources folder.
- Bakes the SPA into the runnable jar/containers so hitting `http://localhost:8080/` serves the React UI and delegates API calls to the same origin.

Pass `-Dskip.frontend=true` if you only want to build the backend (for example, while iterating on ingestion logic).

### Run the backend with Docker

Build the container image defined in [`Dockerfile`](Dockerfile) and start it with a volume for the persistent H2 database/diagram cache:

```bash
docker build -t codevision-backend .
docker run --rm -p 8080:8080 \
  -v codevision-data:/app/data \
  -e SECURITY_APIKEY=your-api-key \
  codevision-backend
```

Spring Boot reads environment variables using its relaxed binding, so any property in `application.yml` can be overridden the same way (for example `-e GIT_AUTH_USERNAME=... -e GIT_AUTH_TOKEN=...`). The container stores project data under `/app/data` (which includes `diagram.storage.root`), so keeping that directory on a named volume prevents losing state between restarts.
The Docker build runs the same Maven packaging command, so the compiled React assets are already embedded and available at `/`.

### Analyze API (`POST /analyze`)

- Headers:
  - `Content-Type: application/json`
  - `X-API-KEY: <value>` (required only when `security.apiKey` is populated)
- Request body:

  ```json
  {
    "repoUrl": "https://github.com/org/repo.git"
  }
  ```

- Response body:

  ```json
  {
    "projectId": 1,
    "status": "ANALYZED_METADATA"
  }
  ```

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

### Overview experience

- Analyzer card auto-collapses into a “Latest analysis” summary after a successful run, exposing project ID, repo URL, analyzed timestamp, and quick actions (Edit inputs, View dashboard, Hide/Show panel). Errors pop the form back open so inputs are immediately editable.
- A deterministic analysis timeline streams each backend step (`Cloning`, `Overview`, `API`, `Database`, `Logger`, `PCI / PII`, `Diagrams`) with `Queued / In progress… / Done / Skipped` states, replacing the old blue-dot ambiguity.
- The results tab rail is sticky on desktop/tablet and backed by a `<select>` dropdown on narrow screens so sections like **Diagrams** never drift off-screen; ARIA roles keep assistive tech informed of the active tab.
- Build summary (group/artifact/version/java release) and class coverage (total vs. main vs. test source sets) headline the Overview panel.
- API Specs tab with paginated tables for REST / SOAP / legacy endpoints, each annotated with spec artifacts and empty-state guidance on how to add missing OpenAPI/WSDL inputs.
- Database tab with entity ↔ repository mapping and DAO operation breakdowns (inferred CRUD intent, targets, inline queries).
- Embedded viewers for OpenAPI YAML, WSDL, and XSD definitions plus synthesized SOAP service summaries.
- Media asset inventory (PNG/JPG/SVG/GIF) listing repository diagrams and screenshots with size + relative path.
- Diagrams tab with type-level filtering, SVG previews, PlantUML/Mermaid source toggles, a sequence-external toggle, and one-click SVG downloads. Each REST/SOAP/legacy endpoint receives its own sequence diagram and call-flow summary (labelled with HTTP method + path); every arrow in that diagram comes from the method-level call graph so you see `Service.save()` instead of a generic “call”, self-invocations loop back onto the lifeline, and repository hops annotate the DAO methods executed before the shared “Database” participant. Class/component diagrams stay readable on large or small screens thanks to responsive layout tweaks.

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

## Database

The backend uses an on-disk H2 database stored under `backend/data/`. Key tables:

- `project` – canonical project record (`repo_url` unique)
- `class_metadata` – flattened Java class inventory per project
- `project_snapshot` – serialized `ParsedDataResponse` plus naming metadata
- `api_endpoint` – persisted REST/SOAP/legacy endpoint catalog
- `asset_image` – discovered documentation assets (path, size, hash)
- `db_entity` – extracted JPA entity metadata (tables, PKs, relationships)
- `dao_operation` – classified DAO/repository operations with inferred CRUD intent
- `diagram` – stored diagram definitions (type, title, source text, SVG path, metadata JSON)

Re-running `/analyze` with the same repository URL overwrites the project metadata and regenerates the snapshot/class records so the UI always reflects the latest scan.
