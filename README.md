# CodeVision

CodeVision ingests a Git repository, extracts structural metadata, and surfaces a project overview through a local Spring Boot + React stack. It supports single-module and multi-module Maven repos (even when the root is only an aggregator) and persistently snapshots the analysis for later viewing.

## Backend (Spring Boot)

The backend lives in [`backend/`](backend/). It exposes a synchronous `/analyze` endpoint that clones a Git repository, extracts build/class metadata, and records a snapshot in an H2 database. A companion `/project/{id}/overview` endpoint returns the latest `ParsedDataResponse` for UI and integrations.

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
```

If you do not need authenticated cloning, leave the values blank. Update the `security.apiKey` to protect API calls. When the key is blank, the API key filter is effectively disabled for development.

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

### Overview panel behaviour today

- Build summary (group/artifact/version/java release).
- Class coverage (total vs. main vs. test source sets).
- Detected OpenAPI specifications (file name + download).
- Upcoming in Iteration 3:
  - SOAP WSDL/XSD viewer with service/port/operation breakdown.
  - Media asset inventory (PNG/JPG/SVG/GIF) for documentation handoff.

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

## Database

The backend uses an on-disk H2 database stored under `backend/data/`. Key tables:

- `project` – canonical project record (`repo_url` unique)
- `class_metadata` – flattened Java class inventory per project
- `project_snapshot` – serialized `ParsedDataResponse` plus naming metadata
- (Iteration 3 adds `api_endpoint` and `asset_image` as the API/asset work ships.)

Re-running `/analyze` with the same repository URL overwrites the project metadata and regenerates the snapshot/class records so the UI always reflects the latest scan.
