## Compiled Artefact & Graph Analysis

The compiled analysis pipeline lives under `backend/api/src/main/java/com/codevision/codevisionbackend/analysis`. It is split into small, testable components so we can plug different sources (source parser vs. bytecode scanner) without changing the orchestration code.

### Runtime flow

1. `AnalysisService` invokes `CompiledAnalysisService` automatically (after JavaParser/diagram generation) while the repo clone still exists. `CompiledAnalysisController` keeps a `POST /api/analyze` escape hatch for rare “run on arbitrary checkout” scenarios.
2. `CompiledAnalysisService` orchestrates the steps:
   - `ClasspathBuilder` ensures `target/classes` exists for every detected Maven module (running `mvn -q -DskipTests compile` when necessary) and resolves a merged compile-scope classpath so module-specific dependencies are included.
   - `JavaSourceScanner` (existing component) parses `src/main/java` for context so we can tag merged nodes with `Origin.SOURCE`.
   - `BytecodeEntityScanner` (ClassGraph) walks the compiled classpath collecting class metadata, Spring stereotypes, endpoints, JPA entities, and sequence generators without classloading user code.
   - `BytecodeCallGraphScanner` (ASM) traverses every `.class`/jar to record method call edges; edges are aggregated into class-level dependencies.
   - `GraphMerger` merges source + bytecode nodes so the downstream writers operate on one canonical graph.
   - `TarjanScc` annotates each node with `sccId` / `inCycle` so diagrams and traversals can short-circuit cycles deterministically.
   - `DiagramWriter` emits PlantUML + Mermaid diagrams (`class-diagram.puml`, `erd.puml`, `erd.mmd`, `seq_*.puml`) while `ExportWriter` writes `analysis.json`, `entities.csv`, `sequences.csv`, `endpoints.csv`, and `dependencies.csv`.
   - `PersistService` bulk-upserts summary tables (`entity`, `entity_field`, `sequence`, `entity_uses_sequence`, `class_dep`, `compiled_endpoint`) so the UI can filter/search without reloading the flat files.
   - The service saves a `CompiledAnalysisRun` row (status, timestamps, counts, output directory) that powers download links.
3. The service returns run metadata plus all export URLs so the frontend can render the “Compiled Analysis” tab immediately.

### Key classes

| Class | Responsibility |
| --- | --- |
| `ClasspathBuilder` | Builds/validates `target/classes` per module, invokes Maven to build each module’s dependency classpath, merges the entries, and filters jars using `analysis.filters.excludeJars`. |
| `GraphModel` | Single in-memory representation for classes, endpoints, sequences, dependencies, SCC metadata, and call graphs (serialised to `analysis.json`). |
| `BytecodeEntityScanner` | Uses ClassGraph to discover stereotypes, endpoints, Spring beans, JPA metadata, Kafka listeners, schedulers, and generator annotations from compiled bytecode. |
| `BytecodeCallGraphScanner` | Uses ASM to record `INVOKE*` instructions and aggregate them to class-level `DependencyEdge` instances of type `CALL`. |
| `TarjanScc` | Detects strongly connected components so diagram writers can collapse cycles safely. |
| `DiagramWriter` | Emits PlantUML/Mermaid diagrams plus per-endpoint sequence diagrams that honour SCC loops and `analysis.maxCallDepth`. |
| `ExportWriter` | Writes deterministic JSON/CSV exports (sorted stably for reproducible diffs). |
| `PersistService` | Batch upserts the relational summary tables and keeps them in sync with `analysis.json`. |
| `CompiledAnalysisService` | High-level orchestrator that ties everything together, records run metadata, and exposes helper methods for controllers (list runs, locate export files). |

### REST + UI integration

* `GET /api/project/{projectId}/compiled-analysis` returns the latest run for a project (status + download URLs) and powers the React tab. `POST /api/analyze` remains available for manual/local runs when needed (use `includeSecurity=false` to skip logger/PII scans).
* `GET /api/analyze/{id}/exports` + `GET /api/analyze/{id}/exports/{file}` power the download buttons in the new “Compiled Analysis” UI tab.
* `GET /api/entities`, `/api/sequences`, `/api/endpoints` surface the Postgres-backed summaries for table views/search.
* The React panel (`CompiledAnalysisPanel`) now loads results automatically after each repository analysis, offers a “Refresh” action, and displays Mermaid/PlantUML sources inline with download buttons.

### Configuration and safety

`application.yml` now includes an `analysis` block with defaults for `acceptPackages`, `maxCallDepth`, compile behavior, output formats, safety limits, and jar filters. `CompiledAnalysisProperties` binds these values for the components above. Maven commands run with `MAVEN_OPTS=-Xmx{analysis.safety.maxHeapMb}m` and a wall-clock timeout derived from `analysis.safety.maxRuntimeSeconds`. All bytecode parsing relies on ClassGraph/ASM so we never classload user code.
