# CodeVision Backend

Spring Boot services that ingest repositories, persist analysis results in PostgreSQL, and expose the API surface consumed by the React frontend.

## Prerequisites

- Java 21 (or newer)
- Maven 3.9+
- PlantUML/Graphviz dependencies are downloaded automatically through Maven

## Local Configuration

The main configuration file lives at `backend/api/src/main/resources/application.yml`. Update it with:

- `git.auth.username` / `git.auth.token` for private repositories (leave blank for public clones)
- `security.apiKey` to protect every endpoint with `X-API-KEY`
- `diagram.storage.root` to control where rendered SVGs are written
- `security.scan` rules and `ignorePatterns` for PCI/PII detection

## Database (PostgreSQL)

The backend no longer writes to an embedded H2 file. Point it at PostgreSQL instead:

1. `cp .env.example .env`. The defaults already point at the managed Render database so local runs share the same data as production.
2. If you prefer a local container, uncomment the “local Docker” block inside `.env` and then run `docker compose up -d postgres`.
3. Launch the backend with `mvn -f backend/pom.xml -pl api spring-boot:run`. If you change any values, export `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`, etc., before starting the app.

Render (and other hosts) must provide the same variables so the backend can connect to the managed Postgres instance. You can also override `SPRING_DATASOURCE_MAX_POOL_SIZE` / `SPRING_DATASOURCE_MIN_IDLE` per environment.

## Run the API locally

```bash
# from repo root
mvn -f backend/pom.xml -pl api spring-boot:run
```

The service starts on `http://localhost:8080`. The analyzer is synchronous; run `POST /analyze` with the repo URL plus the API key header (when enabled).

## Tests & Coverage

```bash
# run unit tests for every backend module
mvn -f backend/pom.xml test

# generate JaCoCo report and enforce ≥90% line/branch coverage (runs slower)
mvn -f backend/pom.xml -Pcoverage verify
```

Reports are written to `backend/api/target/site/jacoco` for the API module (and equivalent folders for others). The `verify` phase fails if bundle-wide line or branch coverage dips below 90%.
