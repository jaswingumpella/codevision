# Iteration 18 Completion – Backend Unit Test Expansion

## Summary

Iteration 18 hardens the compiled-analysis backend by adding deterministic fixtures, deep unit and MVC test coverage, and fail-fast coverage gates. Classpath construction, bytecode scanners, graph writers, persistence, and the REST layer now have targeted tests backed by a reusable compiled test repo, driving JaCoCo coverage for `backend/api` above 90%.

## Key Deliverables

- Added a self-contained fixture repository and jar under `backend/api/src/test/resources/fixtures/compiled-app` so bytecode scanners and call-graph walkers run deterministically.
- Unit tests for `ClasspathBuilder`, `BytecodeEntityScanner`, `BytecodeCallGraphScanner`, `TarjanScc`, `GraphMerger`, `DiagramWriter`, `ExportWriter`, and `PersistService` covering happy paths, filtering, and failure propagation.
- MVC slice tests for `AnalysisController`, `ProjectMetadataController`, and `ProjectExportController` to verify request routing, pagination, and export streaming.
- Maven Surefire/Failsafe split plus JaCoCo checks (≥90% line/branch) so `mvn -pl backend/api verify` fails when coverage dips, and documentation updates describing the new commands.

## Verification

- `mvn -pl backend/api test` executes the expanded unit suite against the compiled fixtures without touching external repos.
- `mvn -pl backend/api verify` runs the unit suite plus the JaCoCo `check` goal, failing if line or branch coverage falls below 90%.
- Manual review of `target/site/jacoco/index.html` confirms coverage surpasses the required threshold.
