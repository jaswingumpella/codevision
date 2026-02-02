## Bytecode Enrichment & Optional Compiled Exports

The primary analysis pipeline lives under `backend/api/src/main/java/com/codevision/codevisionbackend/analyze`. It parses sources, builds diagrams, and then enriches results with bytecode-scanned entities using the components under `backend/api/src/main/java/com/codevision/codevisionbackend/analysis`.

### Runtime flow (main analysis)

1. `AnalysisService` runs `JavaSourceScanner` plus API/DB/diagram extraction against `src/main/java`.
2. It uses `ClasspathBuilder` to build a merged compile-scope classpath across all detected modules; `BytecodeEntityScanner` scans compiled classes and dependency jars for JPA entities and sequence metadata without classloading user code.
3. Bytecode entities are merged into `DbAnalysis` so the Database tab and class/ERD diagrams include dependency entities. Module-aware diagrams are generated from the combined graph.

### Optional compiled export pipeline

`CompiledAnalysisService` still exists for generating the full export bundle (`analysis.json`, CSVs, PlantUML/Mermaid). It is not run automatically and the UI no longer exposes a Compiled Analysis tab. Use `POST /api/analyze` to run it against a local checkout when you need the compiled export artifacts.

### Key classes

| Class | Responsibility |
| --- | --- |
| `AnalysisService` | Orchestrates the main source analysis and merges bytecode entity enrichment into the database + diagram outputs. |
| `ClasspathBuilder` | Builds/validates `target/classes` per module, resolves a merged compile-scope classpath, and filters jars using `analysis.filters.excludeJars`. |
| `BytecodeEntityScanner` | Uses ClassGraph to discover stereotypes, endpoints, Spring beans, JPA metadata, Kafka listeners, schedulers, and generator annotations from compiled bytecode. |
| `DiagramBuilderService` | Emits module-aware class/component diagrams plus ERD/sequence outputs from the combined graph. |
| `ControlFlowSequenceBuilder` | Builds control-flow aware sequence flows (if/else, loops, try/catch/finally, short-circuit conditions) and inlines inter-method calls with cycle/depth guards. |
| `CompiledAnalysisService` | Optional pipeline that generates the full export bundle and persists compiled-analysis summary tables. |
| `BytecodeCallGraphScanner` | (Compiled analysis only) Uses ASM to record `INVOKE*` instructions and aggregate them to class-level `DependencyEdge` instances of type `CALL`. |
| `GraphMerger` | (Compiled analysis only) Merges source + bytecode nodes for export writers. |
| `ExportWriter` | (Compiled analysis only) Writes deterministic JSON/CSV exports (sorted stably for reproducible diffs). |
| `PersistService` | (Compiled analysis only) Batch upserts the relational summary tables and keeps them in sync with `analysis.json`. |

### REST + UI integration

* The React UI consumes the standard analyzer outputs and does not call compiled analysis endpoints.
* `POST /api/analyze` runs the optional compiled export pipeline on a local checkout.
* `GET /api/project/{projectId}/compiled-analysis` returns the latest compiled run (status + download URLs) when present.
* `GET /api/analyze/{id}/exports` + `GET /api/analyze/{id}/exports/{file}` power compiled export downloads.
* `GET /api/entities`, `/api/sequences`, `/api/endpoints` surface the compiled-analysis summary tables when a compiled run exists.

### Configuration and safety

`application.yml` includes an `analysis` block with defaults for `acceptPackages`, `maxCallDepth`, compile behavior, output formats, safety limits, and jar filters. `acceptPackages` is an optional allowlist; when empty, bytecode scanning considers all packages on the resolved classpath. `CompiledAnalysisProperties` binds these values for the bytecode scanners in both the enrichment flow and the optional compiled export pipeline. Maven commands run with `MAVEN_OPTS=-Xmx{analysis.safety.maxHeapMb}m` and a wall-clock timeout derived from `analysis.safety.maxRuntimeSeconds`. All bytecode parsing relies on ClassGraph/ASM so we never classload user code.
