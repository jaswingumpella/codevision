# Iteration 19 Completion – Backend Integration & Pipeline Tests

## Summary

Iteration 19 adds the long-planned integration coverage for the backend. A reusable Testcontainers + Spring Boot harness now boots Postgres, runs the full `AnalysisService` pipeline against the deterministic fixture repo, and asserts that snapshots, diagrams, compiled tables, and exports all materialize exactly as expected. Additional integration tests exercise the compiled-analysis REST surface (including API-key enforcement and pagination) so the `/api/analyze` workflow is verified end-to-end. The Maven `verify` phase now executes these integration tests automatically alongside the existing unit/MVC suites and coverage gates.

## Key Deliverables

- **Postgres Testcontainer harness** (`AbstractPostgresIntegrationTest`) that wires datasource + property overrides, disables expensive Maven recompiles, and reuses the compiled fixture repo across integration tests.
- **`AnalysisPipelineIT`** runs `AnalysisService.analyze` against the fixture repo and asserts persisted `project_snapshot`, `class_metadata`, diagrams, compiled tables, and export artifacts (CSV/JSON) are written.
- **`CompiledAnalysisServiceIT`** exercises the bytecode-only workflow directly, ensuring entity, sequence, dependency, and endpoint tables plus disk exports are produced without a project context.
- **`AnalysisControllerIT`** boots the full Spring MVC stack and validates `/api/analyze`, `/api/analyze/{id}/exports`, `/project/{id}/compiled-analysis`, `/api/entities`, `/api/sequences`, and `/api/endpoints`, including API-key protection for POST requests.
- **Maven `verify` defaults** to running the Failsafe suite (property `skipITs` now `false`), so CI and local developers automatically obtain integration coverage; passing `-DskipITs=true` skips the containerized tests when Docker is unavailable.

## Verification

- `mvn -pl backend/api verify` (requires Docker) – runs unit, MVC, and Testcontainers-backed integration suites plus `jacoco:check` with the ≥90% line/branch gate.
- `mvn -pl backend/api verify -DskipITs=true` – optional escape hatch to run only the unit/MVC suites when containers are not available; used for quick local iteration.
