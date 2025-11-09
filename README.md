# CodeVision

CodeVision ingests a Git repository, extracts structural metadata, and surfaces a project overview through a local Spring Boot + React stack. It supports single-module and multi-module Maven repos (even when the root is only an aggregator) and persistently snapshots the analysis for later viewing.

## Backend (Spring Boot)

The backend lives in [`backend/`](backend/). It exposes a synchronous `/analyze` endpoint that clones a Git repository, extracts build/class/API metadata, and records a snapshot in PostgreSQL. Companion endpoints return the stored data for the UI and integrations:

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

## Iteration Documentation

- Iteration 1 summary: [`docs/iteration-1-completion.md`](docs/iteration-1-completion.md)
- Iteration 2 summary: [`docs/iteration-2-completion.md`](docs/iteration-2-completion.md)
- Iteration 3 summary: [`docs/iteration-3-completion.md`](docs/iteration-3-completion.md)
- Iteration 4 summary: [`docs/iteration-4-completion.md`](docs/iteration-4-completion.md)
- Iteration 5 summary: [`docs/iteration-5-completion.md`](docs/iteration-5-completion.md)
- Iteration 6 summary: [`docs/iteration-6-completion.md`](docs/iteration-6-completion.md)
- Iteration 7 summary: [`docs/iteration-7-completion.md`](docs/iteration-7-completion.md)

## Database (PostgreSQL)

CodeVision now persists all analysis results in PostgreSQL so history survives Render restarts. The backend reads its datasource settings from the standard Spring variables (`SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, etc.) and defaults them to the local Docker values.

### Local workflow

1. Copy the sample env file: `cp .env.example .env`. By default it points at the managed Render instance so your laptop and Render share the same data set.
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

### Schema

- `project` – canonical project record (`repo_url` unique)
- `class_metadata` – flattened Java class inventory per project
- `project_snapshot` – serialized `ParsedDataResponse` plus naming metadata
- `api_endpoint` – persisted REST/SOAP/legacy endpoint catalog
- `asset_image` – discovered documentation assets (path, size, hash)
- `db_entity` – extracted JPA entity metadata (tables, PKs, relationships)
- `dao_operation` – classified DAO/repository operations with inferred CRUD intent
- `diagram` – stored diagram definitions (type, title, source text, SVG path, metadata JSON)

Re-running `/analyze` with the same repository URL overwrites the project metadata and regenerates the snapshot/class records so the UI always reflects the latest scan.
