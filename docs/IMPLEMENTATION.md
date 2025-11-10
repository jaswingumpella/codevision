## Compiled Artefact & Graph Analysis

The compiled analysis pipeline lives under `backend/api/src/main/java/com/codevision/codevisionbackend/analysis`. It is split into small, testable components so we can plug different sources (source parser vs. bytecode scanner) without changing the orchestration code.

### Runtime flow

1. `CompiledAnalysisController` accepts `POST /api/analyze` requests with a local `repoPath`.
2. The controller builds a `CompiledAnalysisParameters` instance and forwards it to `CompiledAnalysisService`.
3. `CompiledAnalysisService` orchestrates the steps:
   - `ClasspathBuilder` ensures `target/classes` exists (running `mvn -q -DskipTests compile` when necessary) and resolves the compile-scope classpath.
   - `JavaSourceScanner` (existing component) parses `src/main/java` for context so we can tag merged nodes with `Origin.SOURCE`.
   - `BytecodeEntityScanner` (ClassGraph) walks the compiled classpath collecting class metadata, Spring stereotypes, endpoints, JPA entities, and sequence generators without classloading user code.
   - `BytecodeCallGraphScanner` (ASM) traverses every `.class`/jar to record method call edges; edges are aggregated into class-level dependencies.
   - `GraphMerger` merges source + bytecode nodes so the downstream writers operate on one canonical graph.
   - `TarjanScc` annotates each node with `sccId` / `inCycle` so diagrams and traversals can short-circuit cycles deterministically.
   - `DiagramWriter` emits PlantUML + Mermaid diagrams (`class-diagram.puml`, `erd.puml`, `erd.mmd`, `seq_*.puml`) while `ExportWriter` writes `analysis.json`, `entities.csv`, `sequences.csv`, `endpoints.csv`, and `dependencies.csv`.
   - `PersistService` bulk-upserts summary tables (`entity`, `entity_field`, `sequence`, `entity_uses_sequence`, `class_dep`, `compiled_endpoint`) so the UI can filter/search without reloading the flat files.
   - The service saves a `CompiledAnalysisRun` row (status, timestamps, counts, output directory) that powers download links.
4. The response returns the run metadata plus all export URLs so the frontend can render the “Compiled Analysis” tab immediately.

### Key classes

| Class | Responsibility |
| --- | --- |
| `ClasspathBuilder` | Builds/validates `target/classes`, invokes Maven to build the dependency classpath, and filters jars using `analysis.filters.excludeJars`. |
| `GraphModel` | Single in-memory representation for classes, endpoints, sequences, dependencies, SCC metadata, and call graphs (serialised to `analysis.json`). |
| `BytecodeEntityScanner` | Uses ClassGraph to discover stereotypes, endpoints, Spring beans, JPA metadata, Kafka listeners, schedulers, and generator annotations from compiled bytecode. |
| `BytecodeCallGraphScanner` | Uses ASM to record `INVOKE*` instructions and aggregate them to class-level `DependencyEdge` instances of type `CALL`. |
| `TarjanScc` | Detects strongly connected components so diagram writers can collapse cycles safely. |
| `DiagramWriter` | Emits PlantUML/Mermaid diagrams plus per-endpoint sequence diagrams that honour SCC loops and `analysis.maxCallDepth`. |
| `ExportWriter` | Writes deterministic JSON/CSV exports (sorted stably for reproducible diffs). |
| `PersistService` | Batch upserts the relational summary tables and keeps them in sync with `analysis.json`. |
| `CompiledAnalysisService` | High-level orchestrator that ties everything together, records run metadata, and exposes helper methods for controllers (list runs, locate export files). |

### REST + UI integration

* `POST /api/analyze` runs the compiled analysis synchronously and returns `analysisId`, timings, counts, and download URLs.
* `GET /api/analyze/{id}/exports` + `GET /api/analyze/{id}/exports/{file}` power the download buttons in the new “Compiled Analysis” UI tab.
* `GET /api/entities`, `/api/sequences`, `/api/endpoints` surface the Postgres-backed summaries for table views/search.
* The React panel (`CompiledAnalysisPanel`) calls these endpoints, displays Mermaid/PlantUML sources inline, and offers “Run Analysis” + download actions.

### Configuration and safety

`application.yml` now includes an `analysis` block with defaults for `acceptPackages`, `maxCallDepth`, compile behavior, output formats, safety limits, and jar filters. `CompiledAnalysisProperties` binds these values for the components above. Maven commands run with `MAVEN_OPTS=-Xmx{analysis.safety.maxHeapMb}m` and a wall-clock timeout derived from `analysis.safety.maxRuntimeSeconds`. All bytecode parsing relies on ClassGraph/ASM so we never classload user code.
