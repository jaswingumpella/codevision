# CodeVision Backend

Spring Boot services that ingest repositories, persist analysis results in H2, and expose the API surface consumed by the React frontend.

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
