# CodeVision

Project bootstrap for the CodeVision repository ingestion workflow.

## Backend (Spring Boot)

The backend lives in [`backend/`](backend/). It exposes a synchronous `/analyze` endpoint that clones a Git repository and records the project metadata in an H2 database.

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

If you do not need authenticated cloning, leave the values blank. Update the `security.apiKey` to protect API calls.

### Run the backend

```bash
cd backend
mvn spring-boot:run
```

The service starts on `http://localhost:8080`.

### Analyze API

- Endpoint: `POST /analyze`
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
    "status": "ANALYZED_BASE"
  }
  ```

## Frontend (React + Vite)

The frontend lives in [`frontend/`](frontend/). It provides a single-page form to submit repository URLs to the backend.

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

The app is served on `http://localhost:5173` and proxies API calls to the backend.

### Build for production

```bash
npm run build
```

## Iteration Documentation

Detailed notes for the first delivery slice are in [`docs/iteration-1-completion.md`](docs/iteration-1-completion.md).

## Database

The backend uses an on-disk H2 database stored under `backend/data/`. The `project` table schema:

```
project(id, repo_url UNIQUE, project_name, last_analyzed_at)
```

Re-running `/analyze` with the same repository URL overwrites the project row to keep the latest clone metadata.
