Got it. We'll explicitly design for cyclic relationships in code graphs (class A → B → C → A, circular service calls, circular bean deps, etc.) by tracking visited nodes in all graph walks (call graph, dependency graph, ER graph) and by generating bounded diagrams (stop expansion after we've already seen a node in the current path). That will prevent infinite recursion and stack blowups during analysis or diagram generation.

Now I’ll give you the iteration plan.

This plan is meant to be handed to an engineering agent (“codex agent”) to start building. Each iteration has:

* Goal / outcome
* Key backend work
* Key UI work
* Persistence impact
* Done criteria

We’ll build in vertical slices so that each iteration produces something usable.

---

## Iteration 1 – Project bootstrap & repo ingestion

### Goal

Be able to submit a repo URL, clone it (private or public), identify it as a project, and persist a shell record.

### Backend

* Create Spring Boot app with:

  * `GitCloneService` that:

    * Reads `git.auth.username` and `git.auth.token` from `application.yml`.
    * Clones repo to a temp working dir.
    * Derives `projectName` from repo URL.
  * `ProjectService` that:

    * Creates/updates a `project` row in H2 using `repoUrl` as unique key.
    * Stores `projectName`, `lastAnalyzedAt`.
    * Overwrite behavior: if that repoUrl exists, wipe and reinsert (Option B).
* Add API key check for protected endpoints via a filter using `X-API-KEY` and config property `security.apiKey`.
* Add `/analyze` endpoint (sync for now):

  * Input: `{ "repoUrl": "..." }`
  * Output: `{ "projectId": "...", "status": "ANALYZED_BASE" }`
* No parsing yet. No diagrams yet.

### UI

* Simple React form:

  * Text input: repo URL
  * “Analyze” button
  * After submit, show success state with project name and project ID.

### Persistence

* H2 schema:

  * `project(id, repo_url UNIQUE, project_name, last_analyzed_at)`
* Wire Spring Data JPA repos for `ProjectRepository`.

### Done Criteria

* User can point to a public or private repo URL in the UI.
* Repo is cloned locally.
* Project row exists in H2.
* Re-running with same repo URL overwrites the row cleanly without error.

---

## Iteration 2 – Source discovery & metadata scan (no diagrams yet)

**Status:** ✅ Completed – see [`docs/iteration-2-completion.md`](docs/iteration-2-completion.md) for delivery notes.

### Goal

Extract basic structural metadata from the repo and persist it. Handle cyclic references safely in graphs.

### Backend

* Introduce `JavaSourceScanner`:

  * Recursively walk `/src/main/java` and `/src/test/java`.
  * Parse classes using JavaParser (Java 7–21).
  * Collect:

    * FQN
    * Package
    * Annotations
    * Implemented interfaces
    * Stereotype guess (Controller / Service / Repository / Entity / Config / Utility / Test / Other)

* Add cyclic safety:

  * When building relationships (e.g. class A depends on B depends on A), store edges in an adjacency map but maintain a `visited`/`visiting` set per traversal so we do not recurse forever.
  * This logic will later be reused by call flow and diagrams.

* Store class metadata into new table `class_metadata`.

* Add `YamlScanner`:

  * Load `application.yml`, `application-*.yml`, and `openapi*.yml`.
  * Save discovered OpenAPI YAML text as part of a `metadataDump` (in memory for now).

* Add build metadata extraction:

  * Read root `pom.xml`:

    * groupId, artifactId, version
    * java.version / maven-compiler-plugin target
  * Detect multi-module:

    * Parse the root POM to list modules.
    * Traverse all modules instead of just current module.
  * No Gradle support.

* Update `ProjectService` to also save build info to `project` row.

* Add `ParsedDataResponse` (partial version):

  * projectName, repoUrl, analyzedAt
  * buildInfo (groupId/artifactId/version/javaVersion)
  * classes (list of basic class metadata)
  * metadataDump.openApiSpecs (if any)

* Add `project_snapshot` table:

  * `project_id`
  * `snapshot_json` (full ParsedDataResponse as JSON blob)

* Add `GET /project/{id}/overview`

  * Returns ParsedDataResponse right now (partial).

### UI

* Add “Overview” page in React:

  * Show project name, repo URL, build info, counts of classes.
* Add API key header in Axios.

### Persistence

* Add `class_metadata` table.
* Add `project_snapshot` table.
* On `/analyze`, after scanning, wipe+replace `class_metadata` rows for this project.

### Done Criteria

* After Analyze, the Overview page shows build info + class list count + openapi presence.
* H2 contains structured class metadata.
* Snapshot JSON is stored once per project.
* No infinite recursion, even if classes refer to each other in cycles.

---

## Iteration 3 – API & asset surfacing *(Status: 🚧 In progress)*

### Goal

Discover all API surfaces (REST + SOAP + legacy), surface their specifications, and inventory binary assets needed by downstream documentation.

### Backend

* Add `ApiScanner`:

  * REST endpoints:

    * Detect `@RestController`, `@Controller`, mapping annotations.
    * For methods missing annotations but implementing an interface method that has them: walk the interface.
  * SOAP endpoints:

    * Detect `@Endpoint`, extract operation names.
    * If a WSDL/WSDD is present on disk, parse basic service/port metadata to enrich the response.
  * Legacy servlet:

    * Detect classes extending `HttpServlet`, expose `doGet/doPost/...` as endpoints.
  * JAX-RS:

    * Detect `@Path`, `@GET`, etc.

* Normalize into `ApiEndpointInfo` with:

  * `protocol` (REST/SOAP/SERVLET/JAXRS)
  * `httpMethod` (or null for SOAP)
  * `pathOrOperation`
  * `controllerClass`
  * `controllerMethod`
  * `specArtifacts` (references into metadata dump for OpenAPI/WSDL/XSD)

* Persist into `api_endpoint` table (wipe+replace each analyze run).

* Add to `ParsedDataResponse.apiEndpoints`.

* Add `GET /project/{id}/api-endpoints` (secured by API key):

  * Returns full list of endpoints for UI and export.

### UI

* Add “API Specs” page in sidebar:

  * Tabs or sections:

    * REST
    * SOAP
    * Legacy (Servlet / JAX-RS)
  * Show table columns: Method | Path/Op | Class | Method | Spec

* Spec viewers:

  * Render OpenAPI YAML with syntax highlighting and download link.
  * Render WSDL/XSD schema summary (service → port → operation) plus raw source viewer.
  * Provide copy/download affordances for each spec artifact.

* If WSDL/XSD detected:

  * Show a dedicated SOAP view that lists discovered services/ports/operations and surfaces the raw WSDL/XSD schema text in a read-only panel (similar to the OpenAPI viewer).

* Static asset discovery:

  * Add `AssetScanner` that inventories non-code documentation assets (PNG/JPG/SVG/GIF).
  * Persist asset metadata (file name, relative path, size) and add `ParsedDataResponse.assets.images`.
  * UI: add “Media Assets” subsection listing discovered images with download links.

### Persistence

* Add `api_endpoint` table.
* Add `asset_image` table (projectId, path, size, hash).
* Extend snapshot JSON to include `apiEndpoints`.

### Done Criteria

* UI shows all endpoints (REST/SOAP/Legacy) with linked specs.
* Interface-based controller methods are correctly resolved.
* SOAP `@Endpoint` operations appear.
* WSDL/XSD content is viewable alongside OpenAPI files.
* Image assets from the repository are discoverable and listed in the UI/snapshot.
* Snapshot JSON contains the same info.

---

## Iteration 4 – Database analysis (entities, repos/DAOs, CRUD classification)

### Goal

Show how the app talks to the database.

### Backend

* Add `JpaEntityScanner`:

  * Detect `@Entity`.
  * Extract:

    * Entity name
    * Table name (from `@Table` or default)
    * Primary keys
    * Fields + column names
    * Relationships (@OneToMany, etc.)

* Add `DaoAnalysisServiceImpl`:

  * Detect Spring Data repositories (`extends JpaRepository`, etc.) and classic DAOs.
  * Extract CRUD intent:

    * Infer SELECT/INSERT/UPDATE/DELETE from method names, annotations, or inline queries (`@Query`).
    * Map to entity/table.
  * Build `DbAnalysisResult`:

    * `classesByEntity`: map entity/table → list of dao/repo classes interacting with it.
    * `operationsByClass`: map dao/repo class → list of `{ methodName, operationType, entity/table, querySnippet }`.
  * Filter redundant interfaces before writing to DB.

* Persist:

  * `db_entity` rows.
  * `dao_operation` rows.

* Add to `ParsedDataResponse.dbAnalysis`.

* Add `GET /project/{id}/db-analysis`.

### UI

* Add “Database” page:

  * Section 1: “Entities and Interacting Classes”

    * Use `classesByEntity` (table: Entity/Table | Classes Using It).
  * Section 2: “Detailed Operations by DAO/Repository Class”

    * Columns: DAO Class | Method | Operation Type | Entity/Table | Query Snippet.
  * Note: This gives security/compliance and onboarding value immediately.

### Persistence

* Add `db_entity` and `dao_operation` tables.
* Extend snapshot JSON.

### Done Criteria

* User can see in UI which DAOs hit which tables/entities.
* CRUD types are auto-classified.
* Cycles in entity relationships (A↔B) do not crash anything:

  * We must traverse relationships with a cycle-safe visited set when computing ERD metadata.

---

## Iteration 5 – Logger Insights + PCI/PII scanning

### Goal

Security/compliance visibility.

### Backend

* Add `PiiPciInspector`:

  * Config via `@ConfigurationProperties("security.scan")` in `application.yml`.
  * Each rule:

    * keyword/regex
    * type (PII or PCI)
    * severity (LOW/MEDIUM/HIGH)
  * Also load `ignorePatterns` to suppress known false positives.
  * Scan ALL text files in repo:

    * `.java`, `.yml`, `.yaml`, `.xml`, `.properties`, `.sql`, `.log`, `.wsdl`, `.xsd`, `.feature`, etc.
  * Produce findings (file, line, snippet, matchType, severity, ignored).
  * Persist into `pii_pci_finding` table.

* Add `LoggerScanner`:

  * Find log statements in Java classes (including tests).
  * Capture:

    * file/class
    * line number
    * level (INFO/WARN/ERROR/DEBUG/TRACE)
    * message template
    * extracted variables
  * Run each log statement through `PiiPciInspector` patterns to set:

    * `piiRisk` boolean
    * `pciRisk` boolean
  * Persist rows in `log_statement`.

* Add to `ParsedDataResponse`:

  * `loggerInsights`
  * `piiPciScan`

* Add new endpoints:

  * `GET /project/{id}/logger-insights`
  * `GET /project/{id}/pii-pci`
  * `GET /project/{id}/export/logs.csv`
  * `GET /project/{id}/export/logs.pdf`
  * `GET /project/{id}/export/pii.csv`
  * `GET /project/{id}/export/pii.pdf`

### UI

* Add “Logger Insights” page:

  * Table with filters:

    * class name text filter
    * level dropdown
    * toggles: show only PII risk / show only PCI risk
  * Columns: Class | Level | Message | Vars | PII? | PCI? | Line
  * Buttons: Download CSV, Download PDF, Expand All / Collapse All.

* Add “PCI / PII Scan” page:

  * Columns: File | Line | Snippet | Type | Severity
  * Toggle: Hide ignored
  * Buttons: Download CSV, Download PDF.

### Persistence

* Add `log_statement` table.
* Add `pii_pci_finding` table.
* Extend snapshot JSON.

### Done Criteria

* PII/PCI findings are visible in UI and exportable.
* Log entries clearly marked for PII/PCI risk.
* CSV/PDF endpoints return valid downloadable files.

---

## Iteration 6 – Diagram generation (PlantUML + Mermaid) and visualization

### Goal

Generate and view diagrams, including call flows, ERDs, components, class diagrams, etc.

### Backend

* Add `DiagramBuilderService`:

  * Inputs:

    * class metadata (from Iteration 2)
    * API endpoints (Iteration 3)
    * dbAnalysis (Iteration 4)
    * call graph info (this iteration, see below)
  * Outputs for each diagram:

    * `plantumlSource`
    * `mermaidSource`
    * (optionally) rendered SVG via PlantUML + Graphviz
  * Diagram types:

    * Class diagram
    * Component diagram (show controllers, services, repos, external systems like SOAP endpoints, DB, other services)
    * Use Case diagram (actors → operations/endpoints)
    * ERD / DB schema diagram (entities and relationships)
    * Sequence diagrams (call flow):

      * Build from a call graph of method→method calls.
      * Include inter-service calls (RestTemplate/WebClient/etc.).
      * Include external classes only if package contains `codeviz2`.
      * Respect cycles:

        * Track current path stack; if we encounter a node that’s already on stack, insert a note like `... (cyclic reference to X)` instead of recursing. This is where your cycle requirement is enforced for diagrams.
  * Save all diagrams to `diagram` table:

    * `diagram_type`
    * `plantuml_source`
    * `mermaid_source`
    * `svg_path` (if rendered)
  * Attach these diagram descriptors to `ParsedDataResponse.diagrams`.

* Add `GET /project/{id}/diagrams`

* Add `GET /project/{id}/diagram/{diagramId}/svg`

### UI

* Add “Diagrams” page with tabs:

  * Class / Component / Use Case / ERD / DB Schema / Sequence
* For each tab:

  * Show SVG if available.
  * Buttons:

    * “View PlantUML Source”
    * “View Mermaid Source”
    * “Download SVG”
* Add a toggle for “Show external codeviz2 calls” on the Sequence tab.
* Add basic filtering UI (e.g. by package prefix) where data supports it.

### Persistence

* Add `diagram` table.

### Done Criteria

* User can navigate diagrams in the UI.
* Cyclic class/service dependencies do NOT crash the generator and are clearly represented (we stop traversal when repetition is detected).
* Diagram sources are stored and retrievable.
* SVGs are downloadable.

---

## Iteration 7 – Full project export (Confluence-style HTML) + Metadata dump

### Goal

Generate a complete technical document and expose everything for downstream consumers (GitLab Duo, auditors, consultants).

### Backend

* Add `ExportService`:

  * Build a single HTML doc with sections:

    * Project Overview (build info, counts)
    * API Endpoints
    * Sequence/Class/Component/Use Case/ERD/DB diagrams

      * Embed diagram sources as `<pre><code>` PlantUML/Mermaid text
    * Database analysis summary:

      * Entities and interacting classes
      * DAO operations table
    * Logger Insights summary
    * PCI/PII summary
    * Gherkin feature list
    * Tech stack + Maven coords
    * Footer: “Generated by CodeDocGen for <repoUrl> at <timestamp>”
  * No SVG embedding required.
  * Cycles should not explode the HTML (same traversal safety applies).

* Add:

  * `GET /project/{id}/export/confluence.html`

    * Returns `text/html` body (downloadable).
  * `GET /project/{id}/metadata`

    * Returns:

      * All “metadataDump” info (like OpenAPI text, WSDL summary, XSD info, YAMLs, etc.)
      * Direct link to download full ParsedDataResponse snapshot JSON.
  * `GET /project/{id}/export/snapshot`

    * Returns ParsedDataResponse (the big JSON blob) for GitLab Duo / Jenkins / ServiceNow.

### UI

* Add “Export” page:

  * Button: “Download Project HTML (Confluence-ready)”
  * Button: “Download ParsedDataResponse.json”
  * Read-only preview panel of the generated HTML.

* Add “Metadata” page:

  * Show raw OpenAPI YAML if present.
  * Show WSDL/XSD summary text.
  * Show Maven and Java runtime details.
  * Show note that this payload is intended for external AI assistants.

### Done Criteria

* Single click gives an HTML doc you can paste into Confluence.
* CSV/PDF for Logger / PCI-PII still work.
* The giant JSON bundle is downloadable.
* All content reflects the most recent `/analyze` run for that repo.

---

## Recap of cycle safety across iterations

We explicitly bake in cycle handling in multiple places:

1. Class scanning & relationship graph (Iteration 2)

   * We build reference graphs but never recurse without a `visited` set.
   * We store adjacency instead of doing deep recursion at scan time.

2. Entity relationships & ERD (Iteration 4)

   * `classesByEntity` / `operationsByClass` is computed via lookups, not DFS, so cycles in entity mappings (bi-directional JPA) won't blow up.

3. Call flow / Sequence diagrams (Iteration 6)

   * We generate diagrams using bounded DFS:

     * Maintain `currentPath` stack.
     * If we try to revisit a method already in `currentPath`, we output a placeholder like `... -> [cycle to com.barclays.CustomerService.updateCustomer()]` and stop that branch.
   * This guarantees no infinite loop and protects both PlantUML/Mermaid generation.

4. Export (Iteration 7)

   * Export is just reading persisted structures. We don’t re-traverse graphs at export time, so no chance of infinite loops there.

---
