# CodeDocGen – Product Requirements Document (PRD)

## 1. Overview

**CodeDocGen** is a local analysis and documentation generator for Java-based services.
It scans a Git repository (public or private), analyzes the codebase, and produces:

* Architectural documentation
* API inventory (REST, SOAP, legacy)
* Database interaction mapping
* Diagrams (class, component, use case, ERD, sequence)
* Security insights (logger review + PCI/PII scan)
* Compliance-friendly exports (CSV/PDF/HTML)
* A full JSON snapshot suitable for external assistants such as GitLab Duo

CodeDocGen targets both modern Spring Boot services and legacy Java services across Java 7–21, including single-module and multi-module Maven projects.

The product runs locally. There is no AI integration in the runtime. All logic is static analysis.

---

## 2. Objectives

1. Accept a Git repository URL (public or private), clone it, and identify it as a “project”.
2. Parse and analyze the repository:

   * Controllers, services, repositories, DAOs, entities
   * JPA mappings
   * WSDL/XSD and SOAP `@Endpoint`s
   * OpenAPI YAML and generated interface contracts
   * Static media/documentation assets (PNG/JPG/SVG/GIF) for downstream documentation bundles
    * Application YAML/properties
    * Gherkin `.feature` files
    * Logging statements
    * DAO/Repository database access patterns
3. Generate:

   * REST/SOAP/legacy API endpoint catalog
   * Class diagrams, component diagrams, ERDs, sequence diagrams, use case diagrams
   * Database mapping view (entity ↔ DAO ↔ table)
   * Spec viewers for OpenAPI and SOAP contracts (highlighting WSDL/XSD structure)
   * Logger Insights table (with PII/PCI risk flags)
   * PCI/PII scan report across all text files
   * Media asset inventory (images, diagrams stored in the repo)
   * Full “project tech doc” export (HTML)
   * CSV and PDF security exports
4. Persist results in PostgreSQL:

   * Structured tables for the UI and exports
   * A full project snapshot JSON blob (`ParsedDataResponse`) for external tooling
   * Production uses Render's managed PostgreSQL to avoid data loss on restarts; local dev runs the same schema via Docker or any developer-hosted Postgres instance.
5. Expose everything through:

   * A React + Material UI dashboard
   * Downloadable artifacts (SVG, CSV, PDF, HTML, JSON)
   * Raw JSON REST endpoints guarded by API key authentication

---

## 3. Non-Goals

* No multi-user tenancy, login, or RBAC.
* No SaaS deployment or background workers/queuing.
* No Gradle support (Maven only).
* No Kotlin/Groovy parsing (Java only).
* No runtime DB connection or schema extraction from a running DB.
* No AI-driven reasoning in-app (outputs may later be fed to an external agent manually).

---

## 4. Tech Stack

### Backend

* Java 21
* Spring Boot
* Maven
* PostgreSQL (managed in production, Dockerized locally) for persistence
* JavaParser for static code analysis (Java 7–21 compatible)
* PlantUML + Graphviz for diagram rendering (to SVG)
* Mermaid output generation (text source)
* PDF/CSV generation libraries (for compliance exports)

### Frontend

* React
* Material UI
* Axios for API calls
* Light/Dark mode support

### Security / Access

* Git credentials (username/token) stored in `application.yml`
* API key (for REST endpoints) configured in `application.yml`
* PII/PCI scan rules + ignore patterns configured under `security.scan` in `application.yml`
* Local runtime only (no internet dependency at analysis time beyond cloning the repo)

---

## 5. Core Concepts

### 5.1 Project

A "project" = one Git repo URL **and** a branch name. The tuple `(repoUrl, branchName)` is the natural key the analyzer uses everywhere so teams can keep parallel histories for `main`, release trains, and long‑lived feature branches.

Behavior:

* `/analyze` accepts `repoUrl` plus an optional `branchName` (defaults to `main`). Branch inputs are normalized (trimmed, refs removed, lowercased) so `Main`, `refs/heads/main`, and `main` all map to one record.
* Parsed results (class metadata, endpoints, diagrams, logger findings, PCI/PII findings, etc.) are replaced per `(repoUrl, branchName)` so the dashboard always reflects the latest run for that tuple without clobbering other branches.
* The live `project` row keeps the newest build metadata and analyzed timestamp for that branch, while immutable snapshot rows preserve every commit. Branch + commit context is also stamped onto asynchronous `analysis_job` records so operators can audit exactly what was queued.

### 5.2 Snapshots & Historical Diff

Each successful run produces a `ParsedDataResponse` blob plus metadata:

* Auto-incremented `snapshot_id`
* Branch name & repo URL
* Resolved commit hash
* Module fingerprint JSON (Git tree IDs for each Maven module)
* Captured timestamp

Snapshots are append-only. The latest snapshot feeds the UI, yet older versions stay downloadable (JSON, CSV, PDF) for audit flows. The backend exposes:

* `GET /project/{id}/snapshots` → history table with branch, commit, fingerprint summary, and created-at values.
* `GET /project/{id}/snapshots/{snapshotId}/diff/{compareSnapshotId}` → structured diff describing the classes, endpoints, and database entities that were added or removed between any two snapshots (same branch or cross-branch).

The React dashboard surfaces this data through a Snapshots panel with a timeline table, commit/branch badges, diff selectors, and a summary widget (“+3 REST endpoints”, “−1 JPA entities”). CSV/PDF exports from this panel are designed to handle thousands of rows so auditors can take the findings offline.

### 5.3 Incremental analysis cache

To avoid full repo re-parses on every commit, the analyzer fingerprints each Maven module by hashing the Git tree objects for its source directories:

1. Clone the requested branch/ref and resolve the commit.
2. If a snapshot already exists for that `(repoUrl, branchName, commitHash)`, return it immediately (no extra work).
3. When the commit differs, compare module fingerprints against the latest snapshot on that branch:
   * Unchanged modules reuse their previously persisted metadata, logger insights, and PCI/PII findings.
   * Only fingerprint deltas are re-scanned; diagrams and aggregated tables rehydrate from the blended dataset so outputs stay consistent.

This incremental cache dramatically reduces rerun time on large mono-repos where a single module changed between commits.

### 5.4 Cyclic safety

Codebases commonly contain cycles:

* Service A → Service B → Service A
* Entity A ↔ Entity B (bi-directional JPA)
* Recursive method references
* Beans calling each other in loops

All analyzers and diagram builders must:

* Track visited / visiting nodes per traversal path
* Stop recursion when a node is already on the path
* Insert a placeholder in sequence diagrams such as `... (cyclic reference to com.barclays.CustomerService.updateCustomer())`
* Never infinite-loop or stack overflow due to cycles

This applies to:

* Class relationship walks
* Method call graph walks
* ERD / relationship graph walks
* Sequence diagram construction

---

## Compiled Artefact & Graph Analysis

### Scope

* Ingest compiled bytecode from `target/classes` and every compile-scope jar.
* Detect classes, interfaces, enums, and records including extends/implements relationships, fields, Spring stereotypes, beans/injections, HTTP endpoints, messaging listeners, schedulers, and all JPA entities/sequences.
* Build method-level call graphs and class-level dependency graphs, detect Tarjan SCCs, and tag nodes with cycle metadata for traversal safety.
* Produce PlantUML + Mermaid diagrams plus JSON/CSV exports that downstream tooling can consume.
* Persist summarized metadata into Postgres so the UI can search/filter without re-reading the flat files.

### Non-goals

* No runtime or reflective analysis (the app never boots user code).
* No attempt to resolve dynamic URLs or expressions—only literal/static metadata is extracted.

### Functional requirements

* The compiled scanner executes automatically as part of the standard `/analyze` workflow (after the JavaParser/metadata passes). A manual `POST /api/analyze` endpoint may exist for advanced scenarios but should never be required for normal usage.
* During the compiled step:
  * Run `mvn -q -DskipTests compile` automatically when `target/classes` is missing.
  * Build the classpath via `mvn -q -DincludeScope=compile -DoutputFile=target/classpath.txt dependency:build-classpath`.
  * Scan compiled classes + jars and merge results with the existing source scan.
* Outputs:
  * `analysis.json`, `entities.csv`, `sequences.csv`, `endpoints.csv`, `dependencies.csv`.
  * `class-diagram.puml`, `erd.puml`, `erd.mmd`, and per-endpoint `seq_*.puml`.
* The UI must provide download buttons for these exports and inline Mermaid rendering for ERDs, with PlantUML text views for copy/paste workflows.

### Quality

* Performance target: analyze ≤2k classes / ~200 jars within 90 seconds on a 2 vCPU development host.
* Outputs must be deterministic (consistent ordering) to simplify diffing.

### Security

* Never classload user bytecode; everything happens through ASM/ClassGraph.
* Run Maven/scanners with timeouts and constrained heap (`analysis.safety.*`), writing artifacts into sandboxed dirs under `analysis.output.root`.

---

## Test Coverage & Automation Roadmap

### Objectives

* Achieve ≥90% line and branch coverage across backend and frontend modules.
* Provide layered confidence: fast unit suites, focused integration tests (service + persistence), end-to-end regression coverage (API + UI) plus cross-cutting performance/regression gates.
* Keep coverage from regressing by enforcing JaCoCo thresholds (backend) and Vite/Jest coverage thresholds (frontend) in CI.

### Scope

1. **Backend unit tests**  
   * Target every service/component with deterministic boundaries (scanners, builders, graph utilities, persistence helpers, controllers).  
   * Use Mockito + Spring slices where appropriate; rely on fixture repositories/jars under `backend/api/src/test/resources`.

2. **Backend integration tests**  
   * Exercise the full analysis pipeline (source + compiled) against fixture repos using TempDir clones and H2/Postgres (Testcontainers) to verify persistence, exports, and REST endpoints.

3. **Frontend unit/component tests**  
   * Cover panel components, hooks, and state orchestration (App.jsx) with Vitest + React Testing Library.  
   * Mock network calls via MSW/test doubles; assert loading/error states for every panel, especially compiled analysis, snapshots, export flows.

4. **End-to-end/UI smoke tests**  
   * Add Playwright/Cypress suite that boots the dev server (pointed to fixture backend), runs through the analyzer workflow, and verifies key UI flows (Overview, Diagrams, Compiled Analysis tab, exports).

5. **Performance/regression checks**  
   * Add optional “heavy fixture” pipeline verifying the analyzer finishes within configured SLAs and produces consistent artifact hashes (stable ordering).

### Deliverables

* Dedicated `backend/api` test fixtures (mini Spring projects, bytecode jars) committed under `backend/api/src/test/resources/fixtures/**`.
* Coverage reports published during CI (JaCoCo XML + HTML, Vite coverage summarized in console).  
* Failing builds when coverage drops below 90% globally or below module-specific thresholds.
* README / Contributing guide updates detailing how to run the suites locally (`mvn test`, `npm run test:unit`, `npm run test:e2e`).
---

## 6. Detailed Feature Areas

### 6.1 Repo ingestion

**Inputs**

* Git repo URL (public or private)
* Optional branch name (defaults to `main`)

**Private repo support**

* PAT/credentials come from `application.yml`:

  ```yaml
  git:
    auth:
      username: ...
      token: ...
  ```
* Not entered through UI
* Not persisted in DB

**Behavior**

* Repo is cloned into a temporary working directory and the requested branch/ref is checked out. If the caller omits `branchName`, we default to `main`.
* Branch names are normalized before persistence so `refs/heads/main` and `main` become the same record.
* Project name derived from repo URL
* Multi-module Maven supported:

  * Parse root `pom.xml`
  * Collect modules and analyze all of them
  * If the root is an aggregator (no code), walk nested directories for additional `pom.xml` files and treat those as modules automatically
* After cloning we resolve the commit hash. If the commit already exists in the snapshot history for that branch we short-circuit and reuse the stored snapshot; otherwise we compute module fingerprints so unchanged modules can be skipped downstream.

**Outcome**

* Project entry created/updated in PostgreSQL:

  * `project_name`
  * `repo_url`
  * `branch_name`
  * `last_analyzed_at`
  * Maven group/artifact/version
  * Java version (from pom / maven-compiler-plugin)
* Append-only snapshot rows capture `snapshot_id`, `branch_name`, `commit_hash`, module fingerprints, and the serialized `ParsedDataResponse`.

---

### 6.2 Code parsing & metadata extraction

**Targets**

* Java source in:

  * `/src/main/java`
  * `/src/test/java`
* Only Java (no Kotlin/Groovy)
* Only Maven projects

**User code heuristic**

* All Java sources are ingested; no packages are excluded.
* We flag classes whose package starts with `com.barclays` or `com.codeviz2` as “first-party” (`userCode = true`) so diagrams and tables can highlight them.
* Additional prefixes can be introduced via configuration in later iterations.

**Class Metadata**
For each class found:

* Fully Qualified Name
* Package
* List of annotations
* Implemented interfaces
* Stereotype classification:

  * Controller / RestController / Endpoint
  * Service
  * Repository / DAO
  * Entity
  * Config
  * Utility
  * Test
  * Other

**Storage**

* `class_metadata` table in PostgreSQL
* Included in `ParsedDataResponse.classes`

**Metadata Dump**

* Capture OpenAPI YAML (`openapi*.yml`)
* Capture YAML configs (including `application.yml`, `application-*.yml`)
* Capture WSDLs, XSDs
* Capture Gherkin `.feature` files
* These raw contents/structural summaries are stored in the snapshot blob and made viewable in the “Metadata” page / export.

---

### 6.3 API Surface Extraction

**Goal**
Identify every externally callable API surface in the project, across modern and legacy styles.

**Sources/Heuristics**

1. Spring MVC / Spring WebFlux:

   * `@RestController`, `@Controller`
   * `@RequestMapping`, `@GetMapping`, `@PostMapping`, etc.
2. Interface-generated controllers from OpenAPI:

   * Controllers with no HTTP annotations but implementing an interface whose methods are annotated
   * Must resolve method mappings from implemented interfaces
3. SOAP:

   * Spring-WS `@Endpoint` classes and their `@PayloadRoot` / operation-style methods
4. Legacy Servlets:

   * `HttpServlet` subclasses exposing `doGet`, `doPost`, `doPut`, etc.
5. JAX-RS:

   * Classes/methods annotated with `@Path`, `@GET`, `@POST`, etc.

**Captured Fields**

* Protocol: `REST`, `SOAP`, `SERVLET`, `JAXRS`
* HTTP method (if applicable)
* Path or operation name
* Controller class
* Controller method signature

**Storage**

* `api_endpoint` table in PostgreSQL
* Added to `ParsedDataResponse.apiEndpoints`
* Returned by `/project/{id}/api-endpoints`

**UI**

* “API Specs” page:

  * REST endpoints table
  * SOAP operations table
  * Legacy servlet / JAX-RS table
  * Read-only viewer for discovered OpenAPI YAML content
  * WSDL/XSD basic operation listing

---

### 6.4 Database Analysis

**Goal**
Document how code interacts with persistence.

**Components**

1. Entity scanning (`@Entity`):

   * Entity name
   * Table name (via `@Table` or fallback)
   * Primary key fields
   * Attributes
   * Relationships (`@OneToMany`, `@ManyToOne`, etc.)

2. DAO / Repository scanning:

   * Spring Data Repositories (`extends JpaRepository`, etc.)
   * Classic DAO classes (naming conventions, e.g. `*Dao`, `*Repository`)
   * For each method:

     * Infer CRUD intent:

       * SELECT / INSERT / UPDATE / DELETE / CALL_STORED_PROC
       * Derived from method name (“findBy…”, “save”, etc.), `@Query` text, etc.
     * Infer entity/table touched
     * Extract query snippet if available

3. Result structure (`DbAnalysisResult`):

   * `entities`:

     * Array of entity metadata `{ entityName, fullyQualifiedName, tableName, primaryKeys[], fields[], relationships[] }`
     * Each field entry captures `{ name, type, columnName }`; relationships capture `{ fieldName, targetType, relationshipType }`.
   * `classesByEntity`:

     * Map: entity/table → list of DAO/repository classes that interact with it
   * `operationsByClass`:

     * Map: DAO/repo class → list of `{ methodName, operationType, target, querySnippet }`

4. Cycles:

   * Entities that reference each other bi-directionally must not break analysis.
   * We compute mappings from already-collected metadata, not by deep recursion, to avoid loops.

**Storage**

* `db_entity` table
* `dao_operation` table
* `DbAnalysisResult` embedded in snapshot as `ParsedDataResponse.dbAnalysis`

**UI**

* “Database” page:

  * Section 1: “Entities and Interacting Classes” (renders `entities` + `classesByEntity`)
  * Section 2: “Detailed Operations by DAO/Repository Class” (renders `operationsByClass`)
  * Schema/ERD diagram (see Diagrams section)

---

### 6.5 Logger Insights

**Goal**
Centralize all logging statements and highlight risk.

**Scanner**

* Identify logging calls:

  * `log.info("...")`, `LOGGER.error("... {}", var)`, etc.
* Capture:

  * Class/file
  * Line number
  * Log level (INFO/WARN/ERROR/DEBUG/TRACE)
  * Message template
  * Referenced variables

**Risk analysis**

* Run message + variables against PCI/PII patterns (see next section)
* Set:

  * `piiRisk` boolean
  * `pciRisk` boolean

**Storage**

* `log_statement` table
* Also surfaced under `ParsedDataResponse.loggerInsights`

**UI**

* “Logger Insights” page:

  * Table columns:

    * Class
    * Level
    * Message
    * Variables
    * PII Risk?
    * PCI Risk?
    * Line #
  * Filters:

    * Class name text filter (defaults blank; no more placeholder OrderService value)
    * Message search that matches substrings in either the message template or variables
    * Log level dropdown
    * Toggles for “show only PII risk” / “show only PCI risk”
  * “Expand All” / “Collapse All” to show variable details for large tables
  * Download buttons:

    * CSV
    * PDF

**Exports**

* `/project/{id}/export/logs.csv`
* `/project/{id}/export/logs.pdf`

---

### 6.6 PCI/PII Scan

**Goal**
Detect potentially sensitive data exposure beyond just logging.

**Scanner behavior**

* Load configurable rules from `application.yml` via `@ConfigurationProperties("security.scan")`. Teams can optionally point this config at additional YAML/JSON rule files so org-wide patterns (e.g., proprietary token formats) can be rolled out without redeploying. The default rule set already includes credit-card regexes, JWT token shapes, common credential keywords (`password`, `secret`, `apiKey`), and markers in `application.properties`.

  * Each rule:

    * `pattern` (keyword and/or regex)
    * `type`: `PII` or `PCI`
    * `severity`: `LOW`, `MEDIUM`, `HIGH`
* Also load `ignorePatterns` to suppress known false positives
* Scan ALL text-based files in the repo:

  * `.java`, `.yml`, `.yaml`, `.properties`, `.xml`, `.sql`,
  * `.log`, `.wsdl`, `.xsd`, `.feature`, `.env`, etc.
  * Custom module roots can be supplied so monorepos can restrict or expand what gets scanned.

**Captured Fields**

* file path
* line number
* matched snippet
* match type (PII / PCI)
* severity level
* ignored? (true if suppressed by ignore rules)

**Storage**

* `pii_pci_finding` table
* Added to `ParsedDataResponse.piiPciScan`

**UI**

* “PCI / PII Scan” page:

  * Table columns:

    * File
    * Line
    * Snippet
    * Type (PII/PCI)
    * Severity (LOW/MEDIUM/HIGH)
    * Ignored? (hidden by default so analysts can focus on new issues)
    * Actions (Ignore / Restore) wired to `PATCH /project/{id}/pii-pci/{findingId}` for inline false-positive management
  * Toggle: Hide or Show ignored matches (defaults to hiding)
  * Download buttons:

    * CSV
    * PDF

**Exports**

* `/project/{id}/export/pii.csv`
* `/project/{id}/export/pii.pdf`

Note: Logger Insights uses this same pattern engine to flag PII/PCI in logs, so rules only live in one place.

---

### 6.7 Diagrams

**Goal**
Produce rich architectural visualizations for onboarding, audit, and documentation.

**Diagram types**

1. **Class Diagram**

   * Classes, interfaces, relationships
   * Includes all parsed classes; “user code” nodes can be emphasized using the heuristic flag.
2. **Component Diagram**

   * High-level service wiring
   * Controllers, services, repositories
   * External systems (DB, downstream services, SOAP endpoints)
3. **Use Case Diagram**

   * Actors interacting with operations/endpoints
   * REST, SOAP operations modelled as use cases
4. **ERD / DB Schema Diagram**

   * Entities, tables, PKs, relationships
5. **Sequence Diagrams (Call Flow)**

   * Call chains traced across individual methods (no more class-only fallbacks). Every arrow label is derived from the invoked method name (e.g., `OrderService.validate()`), and self-calls render as loops on the originating lifeline.
   * Internal calls between first-party classes (highlighted) and external classes (dimmed)
   * Outbound service calls (e.g. via `RestTemplate`, `WebClient`, Feign)
   * Cycle-safe:

     * If a method call repeats within the current traversal path, insert a `"(cyclic reference...)"` node rather than recurse
   * Generated per endpoint so every REST/SOAP/legacy operation gets its own visualisation and call-flow summary (labelled `HTTP_METHOD pathOrOperation`). Metadata captures whether the diagram includes codeviz2 externals, making it easy for the UI to offer a toggle between internal-only and “full” flows. DAO arrows into the shared “Database” participant display the concrete repository method list (e.g., `findById(), save(), delete()`) so reviewers understand which persistence operations fire inside the step.
   * If the call graph truly lacks a method edge (reflection or dynamic proxies), we stop the branch instead of emitting placeholder “call” arrows so diagrams stay trustworthy.

**Outputs**

* For each diagram:

  * PlantUML source text
  * Mermaid source text
  * Optional rendered SVG (using PlantUML+Graphviz for class/component/etc.)

**Persistence**

* `diagram` table:

  * `diagram_type`
  * `plantuml_source`
  * `mermaid_source`
  * `svg_path` (for preview/download)

**UI**

* “Diagrams” page, with tabs:

  * Class
  * Component
  * Use Case
  * ERD / DB
  * Sequence
* Each tab shows:

  * SVG viewer (zoom/pan friendly)
  * “View PlantUML Source” button (modal shows raw source)
  * “View Mermaid Source” button (modal shows raw source)
  * “Download SVG” button
* For Sequence tab:

  * Toggle “Show external calls” to hide/show interactions that leave first-party packages
  * Sidebar lists every endpoint; selecting a row swaps the SVG/source panes so users can jump between flows quickly.
* For large codebases:

  * Support basic filtering / grouping (e.g. filter by package prefix)
  * Goal is to reduce noise without crashing the browser
* Layout is responsive: on narrow screens, the analyzer form stacks above the diagrams tab, and SVG panes scroll within their cards rather than forcing the page to overflow horizontally.

---

### 6.8 Call Flow / Sequence Data Model

A call flow is derived from static method-to-method call analysis:

* Build a graph of caller → callee
* Track outbound service calls for inter-service nodes
* Persist method-level invocation metadata (source FQN + method, target FQN + method, `external` flag) to drive both diagram arrows and the `callFlows` summary strings (formatted as `Class.method() -> OtherClass.otherMethod()`).
* During diagramgen DFS:

  * Maintain `currentPath` stack
  * On re-entry to a method already in `currentPath`, emit a “cyclic reference” note instead of continuing
  * This prevents infinite recursion in diagrams

This call flow view is included in `ParsedDataResponse.callFlows`, and also used to generate the Sequence Diagram entry in `diagram`. Each summary entry lists the controller/service/repository method pairs rather than unnamed class hops, mirroring the PlantUML/Mermaid output.

---

### 6.9 Gherkin Feature Extraction

**Behavior**

* Find `.feature` files (BDD/Gherkin) under:

  * `src/test/resources`
  * `src/main/resources`
* Read feature titles, scenarios, steps
* Persist into snapshot under `ParsedDataResponse.gherkinFeatures`
* UI:

  * “Gherkin” page listing each feature
  * Expand to view steps for auditors / QA

No DB table needed unless we want to, but can be included in snapshot only. (Optional table can be added for querying later.)

---

### 6.10 Export / Publishing

#### Confluence-style Export (HTML)

**Goal**
Generate a single large technical document that can be directly pasted into Confluence.

**Endpoint**

* `GET /project/{id}/export/confluence.html`

**Content**

1. Overview (project summary, repo URL, Java/Maven info)
2. API Endpoints (tables for REST, SOAP, legacy)
3. Diagrams:

   * Class / Component / Use Case / ERD / DB Schema / Sequence
   * Embedded as `<pre><code>` blocks containing the PlantUML/Mermaid source (not images)
4. Database Analysis:

   * Entities and Interacting Classes
   * DAO Operations by Class
5. Logger Insights summary (risky log statements)
6. PCI/PII summary (findings table)
7. Gherkin summary
8. Build Info
9. Footer with timestamp and repo URL

No images are required for Confluence export. We include sources instead of SVG/PNG.

#### Full Snapshot JSON

**Endpoint**

* `GET /project/{id}/export/snapshot`
* Returns `ParsedDataResponse` JSON for external tooling (GitLab Duo, Jenkins, ServiceNow, etc.)

---

### 6.11 Reference Validation Repositories

To keep regression coverage concrete, we maintain a curated list of GitHub projects that represent different workloads (Spring Petclinic for day-to-day sanity checks, Apache ServiceMix for multi-module + SOAP stress, and OWASP WebGoat for security-heavy scenarios). The list lives in `docs/test-repositories.md` and documents:

* Clone URLs and quick background on each repo
* Which iteration deliverables they exercise (API catalog, DB analysis, diagrams, logger insights, PCI/PII exports)
* Step-by-step instructions for running `/analyze` locally and validating UI/export behavior
* A checklist covering snapshot verification, `log_statement` / `pii_pci_finding` replacements, and CSV/PDF exports

Every release of the Logger Insights + PCI/PII capability should re-run against these repositories so we can capture timing, snapshot size, and any false positives that might require tuning the `security.scan` configuration.

---

## 7. API Surface (Backend)

All secured with `X-API-KEY: <value from application.yml>` unless noted.

### Analysis

* `POST /analyze`

  * Body: `{ "repoUrl": "<git url>" }`
  * Flow:

    * Clone repo
    * Analyze codebase
    * Persist all data (overwrite existing rows for that repoUrl)
    * Return: `{ "projectId": "...", "status": "DONE" }`
  * Synchronous request/response

### Project data

* `GET /project`

  * Returns all known projects:

    * projectId
    * repoUrl
    * projectName
    * lastAnalyzedAt
* `GET /project/{projectId}/overview`

  * Returns high-level `ParsedDataResponse` fields (projectName, buildInfo, counts, etc.)
* `GET /project/{projectId}/api-endpoints`
* `GET /project/{projectId}/db-analysis`
* `GET /project/{projectId}/logger-insights`
* `GET /project/{projectId}/pii-pci`
* `PATCH /project/{projectId}/pii-pci/{findingId}` (ignore / restore a finding)
* `GET /project/{projectId}/diagrams`
* `GET /project/{projectId}/gherkin`
* `GET /project/{projectId}/metadata`
* `GET /project/{projectId}/snapshots`
* `GET /project/{projectId}/snapshots/{snapshotId}/diff/{compareSnapshotId}`

  * Returns metadata dumps:

    * OpenAPI YAML content
    * WSDL/XSD summaries
    * Application YAMLs
    * Useful for AI/context seeding

### Diagram/media downloads

* `GET /project/{projectId}/diagram/{diagramId}/svg`

  * Returns diagram SVG
  * Used by “Download SVG” in the UI

### Export / audit

* `GET /project/{projectId}/export/confluence.html`

  * Returns the full HTML technical document
* `GET /project/{projectId}/export/snapshot`

  * Returns the full ParsedDataResponse JSON
* `GET /project/{projectId}/export/logs.csv`
* `GET /project/{projectId}/export/logs.pdf`
* `GET /project/{projectId}/export/pii.csv`
* `GET /project/{projectId}/export/pii.pdf`

---

## 8. Frontend (React + MUI)

### Navigation (sidebar)

1. Overview
2. Snapshots
3. API Specs
4. Call Flow
5. Diagrams
6. Database
7. Classes
8. Gherkin
9. Logger Insights
10. PCI / PII Scan
11. Metadata
12. Export

### Global UI elements

* API key injection in Axios
* Dark/light theme toggle
* Progress feedback while `/analyze` jobs are queued/running

  * The frontend posts to `/analyze`, polls `/analyze/{jobId}`, and keeps the analyzer timeline updated (Queued → Running → Done) until the worker reports `SUCCEEDED` or `FAILED`.

### Per-page behavior

**Overview**

* Project name, repo URL
* Java version, Maven coords
* Counts:

  * #controllers
  * #services
  * #entities
  * #DAOs
  * #endpoints
  * #piiFindings
  * #logsFlagged
* Timestamp of last analysis

**Snapshots**

* Timeline table lists every snapshot (ID, branch, commit hash, captured timestamp, module fingerprint summary) with pagination for large histories.
* Baseline and comparison dropdowns (defaulting to “latest” vs. “previous”) drive the backend diff endpoint and surface summary chips such as “+3 classes / −1 endpoint”.
* Search/filter inputs support branch names, commit hashes, or date ranges so users can zero in on specific releases.
* Action buttons trigger JSON + CSV exports of the selected snapshot or diff.

**API Specs**

* Tabbed or sectioned list:

  * REST
  * SOAP
  * Legacy (Servlet/JAX-RS)
* Table columns per method:

  * HTTP Method
  * Path/Operation
  * Class
  * Method name
* Embedded read-only OpenAPI YAML viewer (if discovered)
* Basic WSDL/XSD operation list

**Call Flow**

* Show generated sequence diagram SVG (if available)
* List of detected outbound service calls (e.g. RestTemplate/WebClient targets)
* Toggle:

  * “Show external calls” (hide or show interactions that touch non-first-party packages)

**Diagrams**

* Tabs:

  * Class
  * Component
  * Use Case
  * ERD / DB
  * Sequence
* Each tab:

  * SVG viewer (zoom/pan)
  * Buttons:

    * View PlantUML Source (modal)
    * View Mermaid Source (modal)
    * Download SVG
* Filtering capability:

  * Simple filter by package prefix or layer to reduce clutter on large services

**Database**

* Top: ERD / DB Schema diagram SVG
* Section 1: Entities and Interacting Classes

  * Table from `classesByEntity`
* Section 2: Detailed Operations by DAO/Repository Class

  * Table from `operationsByClass`
  * Columns: DAO Class | Method | Operation Type | Entity/Table | Query Snippet

**Classes**

* Table of all classes from `class_metadata`

  * Columns: Class FQN, Stereotype, Package
* Filter chips for stereotype (Controller, Service, Repo, Entity, etc.)
* Click row → drawer with annotations and interfaces

**Gherkin**

* List all `.feature` files
* Expand a feature to read scenarios and steps

**Logger Insights**

* Table with filters:

  * Class filter (text)
  * Log level dropdown
  * PII risk toggle
  * PCI risk toggle
* Columns:

  * Class
  * Level
  * Message
  * Vars
  * PII?
  * PCI?
  * Line
* Buttons:

  * “Download CSV”
  * “Download PDF”
  * “Expand All / Collapse All”

**PCI / PII Scan**

* Table columns:

  * File
  * Line
  * Snippet
  * Type (PII/PCI)
  * Severity (LOW/MEDIUM/HIGH)
* Toggle: Hide ignored
* Buttons:

  * “Download CSV”
  * “Download PDF”

**Metadata**

* Scrollable panels:

  * OpenAPI YAML content
  * WSDL/XSD summaries
  * application.yml configs
  * Any other captured structured metadata (e.g., headers or schema fragments)
* Button: “Download ParsedDataResponse.json” (calls `/export/snapshot`)

**Export**

* Button: “Download Technical Doc (HTML)”

  * Calls `/export/confluence.html`
* Preview region:

  * Renders that HTML so user knows what’s going into Confluence
  * Uses lazy loading + fixed-height scroll container so even 10k+ line HTML exports do not freeze the layout
* Button: “Download ParsedDataResponse.json” (again, for convenience)

---

## 9. Persistence / PostgreSQL Schema (Logical)

Render deployments attach to a managed PostgreSQL service (non-ephemeral disk) so project history survives restarts. Developers run the same schema locally via Docker Compose or any Postgres 15+ instance by pointing `SPRING_DATASOURCE_URL` at it. The logical shape of the schema is unchanged:

**project**

* `id` (PK)
* `repo_url`
* `branch_name`
* `project_name`
* `last_analyzed_at` (timestamp)
* `build_group_id`
* `build_artifact_id`
* `build_version`
* `build_java_version`
* Unique constraint on (`repo_url`, `branch_name`) so multiple branches from the same repo can coexist.

**analysis_job**

* `id` (UUID PK)
* `repo_url`
* `branch_name`
* `status`, `status_message`, `error_message`
* `project_id` (FK)
* `commit_hash`
* `snapshot_id` (FK → project_snapshot.id)
* `created_at`, `updated_at`, `started_at`, `completed_at`

**project_snapshot**

* `id` (PK)
* `project_id` (FK → project.id)
* `project_name`
* `repo_url`
* `branch_name`
* `commit_hash`
* `module_fingerprints_json` (CLOB)
* `snapshot_json` (CLOB, full `ParsedDataResponse`)
* `created_at` timestamp

**class_metadata**

* `id`
* `project_id` (FK)
* `class_name` (FQN)
* `package_name`
* `stereotype`
* `implements_interfaces` (text/CSV)
* `annotations` (text/CSV)

**api_endpoint**

* `id`
* `project_id` (FK)
* `protocol` (`REST` / `SOAP` / `SERVLET` / `JAXRS`)
* `http_method` (nullable for SOAP)
* `path_or_operation`
* `controller_class`
* `controller_method`
* `spec_artifacts_json` (serialized references to OpenAPI/WSDL/XSD artifacts)

**db_entity**

* `id`
* `project_id` (FK)
* `entity_name`
* `table_name`
* `primary_keys` (CSV)
* `fields_json` (CLOB)
* `relationships_json` (CLOB)

**dao_operation**

* `id`
* `project_id` (FK)
* `class_name` (DAO/repository class FQN)
* `method_name`
* `operation_type` (`SELECT`, `INSERT`, `UPDATE`, `DELETE`, `CALL_STORED_PROC`)
* `entity_or_table`
* `raw_query_snippet` (CLOB)

**diagram**

* `id`
* `project_id` (FK)
* `diagram_type` (`CLASS`, `COMPONENT`, `USECASE`, `SEQUENCE`, `ERD`, `DB_SCHEMA`)
* `plantuml_source` (CLOB)
* `mermaid_source` (CLOB)
* `svg_path` (nullable, local filesystem reference for download)

**log_statement**

* `id`
* `project_id` (FK)
* `class_name`
* `file_path`
* `line_number`
* `log_level`
* `message_template` (CLOB)
* `variables_json` (CLOB)
* `pii_risk` (BOOLEAN)
* `pci_risk` (BOOLEAN)

**pii_pci_finding**

* `id`
* `project_id` (FK)
* `file_path`
* `line_number`
* `matched_text_snippet` (CLOB)
* `match_type` (`PII` / `PCI`)
* `severity` (`LOW` / `MEDIUM` / `HIGH`)
* `ignore_applied` (BOOLEAN)

---

## 10. Security

1. **API Key Enforcement**

   * All non-health endpoints require header:
     `X-API-KEY: <value configured in application.yml>`
   * If missing or invalid, return 401

2. **Git Credentials**

   * Stored only in `application.yml`
   * Used only at clone time for private repos
   * Never persisted or exposed by API

3. **Local Runtime**

   * Application is intended to run locally on developer/consultant/security engineer machine
   * PostgreSQL runs via Docker (compose) or any developer-supplied instance that mirrors production
   * No user auth/multi-tenancy

---

## 11. Iteration Plan / Delivery Phases

### Iteration 1 – Bootstrap & Repo ingestion

* Implement Spring Boot app skeleton
* Implement `GitCloneService`
* Implement `/analyze` to clone + register project
* Create `project` table
* React: input repo URL, call `/analyze`, display project basic info

### Iteration 2 – Source discovery & metadata

* Parse Java sources (main + test), ingesting all packages (flag first-party heuristically)
* Extract class metadata + build info from POM
* Add `class_metadata` + `project_snapshot`
* Add `/project/{id}/overview`
* React: “Overview” page
* Add cyclic safety guards in traversal logic

### Iteration 3 – API surface extraction

* Extract REST, SOAP (`@Endpoint`), servlet (`HttpServlet`), JAX-RS, interface-inherited mappings
* Persist to `api_endpoint` table
* Add `/project/{id}/api-endpoints`
* React: “API Specs” page w/ tables and OpenAPI/WSDL sections

### Iteration 4 – Database analysis

* Scan JPA entities and DAO/Repo classes
* Classify CRUD operations and map them to entities/tables
* Persist to `db_entity` + `dao_operation`
* Populate `DbAnalysisResult`
* Add `/project/{id}/db-analysis`
* React: “Database” page (Entities & Interacting Classes + Operations by DAO/Repository)

### Iteration 5 – Logger Insights + PCI/PII Scan

**Status:** ✅ Completed – see [`docs/iteration-5-completion.md`](docs/iteration-5-completion.md).

* Implement `PiiPciInspector` with patterns + severities + ignore rules from `application.yml`
* Scan all text files for PII/PCI
* Persist `pii_pci_finding`
* Scan log statements, mark piiRisk/pciRisk
* Persist `log_statement`
* Add `/logger-insights`, `/pii-pci`
* Add export endpoints for CSV/PDF
* React: “Logger Insights” and “PCI / PII Scan” pages with filters and download buttons

### Iteration 6 – Diagram generation & visualization

* Generate diagrams:

  * Class
  * Component
  * Use Case
  * ERD / DB Schema
* Sequence (call flow with cycle-safe traversal; external calls can be toggled/filtered in the UI)
* Persist in `diagram` table:

  * PlantUML source
  * Mermaid source
  * SVG path (if rendered)
* Add `/project/{id}/diagrams` and `/project/{id}/diagram/{diagramId}/svg`
* React: “Diagrams” page with tabs, toggles, download SVG, view PlantUML/Mermaid source
* Add “Call Flow” page for sequence diagrams and outbound service calls list

### Iteration 7 – Full export (HTML, CSV, PDF, JSON)

* Implement export service to generate:

  * Project technical document (HTML) for Confluence

    * Includes all sections (Overview, API, Diagrams [as text blocks], DB analysis, Logger Insights summary, PCI/PII summary, Gherkin, Build info)
  * CSV/PDF download for Logger Insights and PCI/PII (already exposed in Iteration 5)
  * Snapshot JSON download (`ParsedDataResponse`)
* Add `/project/{id}/export/confluence.html` and `/project/{id}/export/snapshot`
* React:

  * “Export” page with:

    * Download Confluence HTML
    * Preview of generated HTML
    * Download ParsedDataResponse.json
  * “Metadata” page with OpenAPI YAML, WSDL/XSD, application.yml, etc.

At Iteration 7, CodeDocGen is feature-complete according to the PRD.

---

## 12. Definition of Done (Global)

CodeDocGen is considered “functionally complete” when:

1. User can enter a repo URL and produce:

   * Overview
   * API catalog
   * Database interaction view
   * Logger Insights with PII/PCI flags
   * PCI/PII scan view
   * Diagrams (class, component, use case, ERD, DB schema, sequence)
   * Gherkin feature listing
   * Call flow view, with inter-service calls
2. All derived content is persisted to PostgreSQL for that repo, overwriting previous runs for that same repo.
3. Cycles in code do not crash or loop forever; instead, diagrams and call flows show a readable cyclic reference marker.
4. The React UI shows all sections in a left-nav dashboard, with light/dark mode, filters, toggles, downloads.
5. The backend exposes:

   * CSV/PDF exports for security review
   * HTML export for pasting into Confluence
   * A full JSON snapshot for feeding to external tooling (GitLab Duo, Jenkins, ServiceNow, audit bots, etc.).
6. All APIs except healthcheck require `X-API-KEY`.

This document is the authoritative product definition for CodeDocGen v1.


PRD – CodeDocGen v1
1. Overview

CodeDocGen is a comprehensive static analysis and documentation generator for Java-based codebases (Java 7 through 21+), supporting both single-module and multi-module Maven projects. It scans a given Git repository (public or private), analyzes the code (including user-designated library code), and produces a rich set of artifacts: architectural documentation, API inventories (covering RESTful, SOAP, and legacy endpoints), database interaction mappings, multiple types of diagrams, security insight reports, and exportable technical documentation. All analysis is performed locally via static analysis – no AI or external runtime analysis is involved. The tool can optionally include selected dependency libraries in the analysis (parsing their compiled classes for configured package prefixes) to provide a more complete view of frameworks or shared modules beyond the project’s own source. CodeDocGen is designed to work offline on a developer’s machine and outputs results both to an embedded database and as files for user consumption.

2. Objectives

Repository Ingestion: Accept a Git repository URL (with credentials for private repos configured in advance), clone the repo to a temporary directory, and register it as a “project” for analysis. For multi-module Maven repositories, detect and include all modules automatically (parsing the root pom.xml to find modules, including aggregator-only roots).

Static Code Analysis: Parse and analyze all Java source files in the repository (under src/main/java and src/test/java), targeting Java 7–21 syntax. No Kotlin or Groovy support – Java only. In addition, analyze compiled classes from Maven dependencies that match user-defined package prefixes (this allows including certain library code in the analysis for completeness). Use JavaParser for source files and a bytecode parser (e.g. ASM) for .class files in dependencies. Gather metadata on all discovered classes, including their fully-qualified names, packages, annotations, implemented interfaces, and an inferred stereotype (e.g. Controller, Service, Repository/DAO, Entity, Configuration, Utility, Test, Other). Flag classes as “first-party” (userCode=true) if they belong to designated top-level packages (e.g. the organization’s package prefix). This first-party marking is configurable so that teams can treat certain dependency packages as part of the analysis scope.

Metadata Extraction: Identify and collect ancillary project metadata: build information from Maven (groupId, artifactId, version, Java compiler target), configuration files (e.g. application.yml and other YAML/props), API specifications (OpenAPI .yml files, WSDL/XSD files for SOAP), and even BDD specifications (.feature files). Store raw content or parsed summaries of these artifacts as part of the project’s metadata snapshot for reference.

API Surface Identification: Detect all externally exposed API endpoints across modern and legacy Java approaches. This includes:

Spring MVC/WebFlux REST controllers: Classes annotated with @RestController or @Controller, capturing their @RequestMapping/@GetMapping/etc. methods and URL paths.

Interface-driven APIs: REST controllers that implement interface methods which carry the HTTP annotations (e.g. from generated OpenAPI interfaces) – resolve the mappings via the interface if not present on the class itself.

SOAP web service endpoints: Classes annotated with Spring-WS @Endpoint and their handler methods (identified by @PayloadRoot or similar), plus any WSDL files defining services/ports/operations to cross-reference.

Legacy servlets: HttpServlet subclasses (capture doGet, doPost, etc. as endpoints).

JAX-RS resources: Classes using @Path with methods annotated @GET, @POST, etc..

For each endpoint, capture the protocol (REST, SOAP, Servlet, JAX-RS), HTTP method (if applicable), the path or operation name, the implementing controller class and method, and link it to any relevant API spec artifact (OpenAPI or WSDL/XSD) for documentation.

Database Interaction Mapping: Analyze how the code interacts with databases. This involves:

Entity scanning: Find JPA @Entity classes and record their table names, primary key fields, attributes, and relationships (OneToMany, ManyToOne, etc.).

Repository/DAO scanning: Identify Spring Data repositories (interfaces extending JpaRepository, CrudRepository, etc.) and traditional DAO classes. For each data access method, infer the CRUD operation type (SELECT/INSERT/UPDATE/DELETE or calls to stored procedures) by method naming conventions or annotations (@Query text). Determine which entity or table is involved and extract any query snippet if available.

Using the above, compile a database analysis view that maps entities to the DAO/repository classes that use them, and lists each DAO method with its operation type and target entity/table. This provides a clear picture of data access patterns. Cyclic relationships (e.g. bi-directional JPA mappings) are handled gracefully by relying on stored metadata and avoiding infinite recursion in analysis.

Security Insights – Logging and Sensitive Data: Scan all Java code for logging statements and all text files for sensitive data patterns:

Logger analysis: Find usages of common logging frameworks (e.g. log.info("...") or LOGGER.error("... {}")). For each, capture the containing class and line number, the log level, the static message template, and any variables/parameters used. Use a configurable set of regex rules to flag logs that may contain PII (Personally Identifiable Information) or PCI (Payment Card Information) – for example, logging of emails, credit card numbers, etc. Each log statement in the results is annotated with piiRisk and/or pciRisk booleans if it matches any risky pattern.

PII/PCI scan: In addition to logs, perform a comprehensive scan of all repository text files (source code, config, SQL, XML, logs, feature files, etc.) for occurrences of sensitive data patterns. Use a set of rules (regex or keywords, categorized by PII vs PCI and severity) defined in the config. Record each finding with file path, line number, matched snippet, type (PII or PCI), and severity level. Allow an ignore-list for false positives and mark findings as ignored accordingly.

Diagram Generation: Produce multiple architectural diagrams from the analyzed code, in both PlantUML and Mermaid formats (textual definitions, with optional rendered SVG images):

Class Diagram: showing classes/interfaces and their relationships (inheritance, implementations, associations). All classes discovered are included; first-party classes can be visually emphasized (different color or style) relative to external ones.

Component Diagram: illustrating high-level system components and dependencies, e.g. controllers → services → repositories, plus external systems (databases, external API endpoints, etc.) as distinct nodes.

Use Case Diagram: depicting user actors and the use cases (operations) they trigger in the system. In this context, API endpoints (REST endpoints, SOAP operations, etc.) can be shown as use cases with actors like “User” or external systems interacting with them.

ERD (Entity Relationship Diagram): visualizing database entities, tables, primary keys, and relationships between entities (one-to-many, many-to-many links). This provides a schema-like view derived from the JPA entities.

Sequence Diagrams (Call Flows): for key flows in the code, showing method call sequences across objects. The tool will build a call graph of method-to-method calls through static analysis, then allow visualization of specific flows. First-party classes appear as distinct lifelines, and calls to external classes (outside the allowed prefixes) can be shown as external lifelines or noted differently. Outbound calls to other services (e.g. via RestTemplate or Feign clients) are represented as calls to an “external system” participant. Sequence generation is cycle-safe: if a recursive or cyclic call is detected (method A calls B, which eventually calls A), the diagram will include a placeholder note like “...(cyclic reference to ClassA.methodX())...” instead of infinite recursion. This ensures diagrams always terminate and remain readable.

For each diagram type, CodeDocGen produces the source definitions in both PlantUML and Mermaid syntax. Optionally, PlantUML diagrams can be auto-rendered to SVG images (using Graphviz) for quick preview. All diagram definitions and any generated images are stored in the project’s output (database or file system) for retrieval.

Comprehensive JSON Snapshot: After analysis, aggregate all extracted insights and metadata into a single JSON object (the ParsedDataResponse snapshot). This includes project info, build info, lists of classes, API endpoint catalog, database analysis results, logger statements, PII findings, call graph data, diagram specifications, Gherkin scenarios, and raw metadata text. This JSON is persisted in the embedded database and can be downloaded via an API. It is specifically structured to be compatible with external auditing or AI assistant tools (e.g. GitLab Duo) for further automated analysis.

User Interface & Exports: Provide a local web dashboard (React + Material UI) to present all the collected information in a navigable format. Key UI sections include: Overview, API Specs, Call Flows, Diagrams, Database, Classes, Gherkin (BDD), Logger Insights, PCI/PII Scan, and an Export section. Each section is designed with filtering and drill-down capabilities (e.g. filter classes by stereotype, search log messages, toggle external calls in sequence diagrams). Users can download various artifacts: diagram SVG images, CSV or PDF reports for logs and findings, the full JSON snapshot, and an HTML technical document suitable for pasting into Confluence. The HTML export consolidates the analysis into one document with all sections (overview, endpoint tables, diagrams (as code blocks), tables of findings, etc.). All download/export endpoints are protected by an API key for security.

3. Non-Goals and Constraints

CodeDocGen v1 has a focused scope. It will not: implement multi-user access or authentication (no multi-tenant or RBAC features), run as a cloud/SaaS service (local use only), or handle non-Maven build systems (Gradle is out of scope). Only Java source is parsed – no support for analyzing Kotlin or Groovy. The tool doesn’t execute the code or connect to live databases (no runtime validation of DB schema beyond the code definitions). AI or “intelligent” reasoning about the code is also out of scope – the tool gathers and presents data, but any interpretation (e.g. risk assessment beyond pattern matching) is left to the user or external AI agents. These constraints keep the complexity manageable and focused on static analysis.

4. Tech Stack

Backend: Java 21 (Spring Boot). Data is stored in PostgreSQL (managed in production, Dockerized locally) for each project’s results. JavaParser is used for parsing Java source code (ensuring compatibility with Java 7–21 syntax), and an ASM-based parser or similar is used for reading compiled .class files from dependencies. Diagram generation leverages PlantUML (with Graphviz) for rendering diagrams and also outputs Mermaid definitions for compatibility with other platforms. Miscellaneous: Lombok for data classes, a PDF library for report exports, and standard libraries for YAML/JSON processing.

Frontend: React + Material-UI, packaged via Vite. The UI is a single-page app communicating with the Spring Boot API (which is proxied in development). It offers responsive design with light/dark mode. Axios is used for API calls (automatically including the API key header).

Security: Git credentials for private repo access are read from config and never stored beyond runtime. All API endpoints (except a health check) require a configured X-API-KEY header; when running with no key configured, security can be disabled for convenience in local environments. The application is intended to run on a developer’s local machine, so network exposure is not recommended.

5. Core Concepts

Project: A “project” in CodeDocGen corresponds to a single Git repository (identified by its clone URL). Each analysis run operates at the project level. Running an analysis will clone the repo (or fetch updates), parse everything, then wipe and replace any previously stored analysis data for that project in the database. The project record includes the repository URL (unique key), a derived project name, last analyzed timestamp, and basic build info (Maven GAV coordinates and Java version). Historical runs are not retained – the focus is always on the latest state of the repository.

Parsed Data Snapshot: After analysis, the system compiles all findings into a ParsedDataResponse object, which is then saved (as JSON) in the database and can be retrieved via API. This snapshot contains structured sections for each category of data (classes, endpoints, etc.). It serves both the UI (for overview and data pages) and external integrations. For example, a security team could pull the JSON and feed it into an audit tool or AI assistant (like GitLab Duo) to ask higher-level questions about the codebase.

Cyclic Safety: Many codebases contain cyclic relationships (mutual class dependencies, recursive calls, circular service flows, etc.). CodeDocGen is built to detect and handle cycles gracefully in all analysis phases. Graph traversals (for class relationships, call graphs, ERDs) maintain visited sets to avoid infinite loops. If a cycle is encountered, the system stops deeper traversal and records a placeholder or reference rather than repeating indefinitely. This applies throughout: class hierarchy resolution, entity relationship mapping, and especially sequence diagram generation (which will note a cycle and break the loop). As a result, no analysis operation will hang or crash due to recursive structures, and outputs will explicitly indicate cyclic references where relevant.

6. Detailed Features

6.1 Repository Ingestion: (Iteration 1) The backend provides a POST /analyze endpoint to initiate analysis of a repo. It takes a JSON body with the repoUrl. On invocation, the service clones the repository to a temp location. If credentials are provided in config, they are used for the clone (otherwise assume public access). The root POM is parsed to detect if this is a multi-module project; if so, all sub-module directories are discovered and their source code will be included. A new entry is created in the project table (or an existing one is updated) with the project name (derived from the repo URL), the repo URL, and timestamp. Overwriting an existing project triggers deletion of old analysis data for that project (to avoid stale data). The /analyze call is synchronous for now – the client will receive a response when analysis is complete. (In the UI, a loading indicator is shown during this process.) Upon success, the response includes the projectId and a status. The API is secured by the API key filter (if a key is set) to prevent unauthorized triggering of analyses.

6.2 Code Parsing & Metadata Extraction: (Iteration 2) Once the repo is cloned, CodeDocGen scans through all Java source files in src/main/java (and also test sources in src/test/java). It uses JavaParser to parse each Java file into an AST and extract class definitions. For each class, a ClassMetadata record is created capturing: the fully-qualified name, package, list of annotation names, list of implemented interfaces, and an inferred stereotype role. Stereotype heuristics are based on naming and annotations (e.g. classes annotated @Controller are Controllers, those extending Spring Data interfaces are Repositories, classes annotated @Entity are Entities, etc.). We also note whether the class came from main or test source (to track test coverage), and mark it as userCode=true if it belongs to configured first-party packages (by default, example prefixes like com.barclays or the tool’s own com.codeviz2, modifiable via config). All classes in the repo are recorded; we do not exclude any packages by default.

In addition to source parsing, the system can augment this with library class parsing for selected dependencies. After building a list of direct dependency artifacts from the Maven POM, the tool will inspect those JARs for classes whose package matches a user-defined prefix list. For each such class, a limited metadata record is created (since source might not be available): at least the class name, package, and perhaps method signatures via bytecode analysis. These classes can be flagged as userCode=false (external) or even userCode=true if they are part of an internal shared library. This allows diagrams and call graphs to include important external classes instead of treating them as black boxes.

The build system metadata is also collected now: the root pom.xml is parsed to extract the Maven GroupId, ArtifactId, Version of the project, as well as the target Java version (from either the maven-compiler-plugin or <java.version> property). If the project is multi-module, the root POM’s <modules> section is used to find all module directories; each module’s POM may be parsed for its own artifact info if needed, but for simplicity we aggregate under one project. This info is stored in the project record and in the snapshot’s buildInfo.

The code parsing stage also gathers ancillary files of interest:

OpenAPI specs: any YAML or JSON files matching common OpenAPI spec naming (e.g. openapi.yaml, swagger.yaml) are read and stored (raw text) in the snapshot’s metadataDump.openApiSpecs.

WSDL/XSD files: if present (commonly under resources/), these are stored as well (the raw XML text or a summarized form focusing on service/port type definitions).

Application configs: e.g. application.yml and any variant (application-*.yml), as well as .properties config files, are collected into metadataDump.configs.

Gherkin feature files: (*.feature under src/test/resources or elsewhere) are parsed to extract the feature title and scenario names/steps, which are stored in gherkinFeatures in the snapshot for documentation and QA reference.

All the above extracted data is assembled into a partial ParsedDataResponse object. The class metadata is persisted in a new class_metadata table in PostgreSQL, and the full snapshot JSON is saved in a project_snapshot table for retrieval. A new API GET /project/{id}/overview returns the current snapshot (or an overview subset) so that the frontend can display high-level stats (counts of classes, etc.). At this point (Iteration 2), we have a basic overview available: project info, build info, number of classes, and any OpenAPI specs discovered (e.g. listing their file names). Cyclic references in class parsing (e.g. class A references B which references A) are safely handled by not traversing into already-seen classes; this lays the groundwork for cycle-safe processing in later analyses.

6.3 API Surface Extraction: (Iteration 3) The next step is to build a full inventory of APIs. CodeDocGen scans for known patterns that indicate an externally callable API: REST controllers, SOAP endpoints, servlets, JAX-RS resources, etc. Using the class and annotation data from earlier, it performs the following:

REST Controllers: For each class annotated @RestController or @Controller, gather all handler methods. Each method’s HTTP method (GET/POST/PUT/etc.) and path mapping (from @RequestMapping or the shorthand annotations) are resolved. If a controller method is not directly annotated but implements an interface method that is annotated (common in code generated from OpenAPI interfaces), the tool looks up the interface method’s annotations to determine the endpoint details. All found REST endpoints are recorded.

SOAP Web Services: Find classes annotated @Endpoint (Spring-WS). For each such class, identify methods with payload handling annotations (like @PayloadRoot specifying a SOAP operation or namespace). In addition, if WSDL files were collected, parse them to extract service names, port names, and operation names to enrich the SOAP endpoint information. The result is a list of SOAP operations provided by the service.

Legacy Servlets: Find any class extending HttpServlet. Record a pseudo-endpoint for each of the doGet/doPost/etc. methods that are overridden, using the URL pattern if available (e.g. from web.xml or annotations if any). If explicit mapping info isn’t easily available, just list them as servlet endpoints with the class name and method (the user can infer the deployment context).

JAX-RS Resources: Find classes or methods annotated with JAX-RS annotations (@Path at class or method, along with @GET, @POST, etc.). These indicate RESTful endpoints typically in Java EE or Jakarta EE projects outside of Spring. Capture the HTTP method and the URI path from @Path.

All the above are normalized into a common ApiEndpoint model with fields: protocol (REST, SOAP, SERVLET, JAX-RS), httpMethod (if applicable), pathOrOperation (URI path or SOAP operation name), controllerClass and controllerMethod (the implementing class and method signature). These entries are saved in an api_endpoint table in PostgreSQL and also added to the JSON snapshot (apiEndpoints list). A new API GET /project/{id}/api-endpoints returns this list for the UI.

On the UI side, a new “API Specs” page presents the API catalog. It is typically organized into tabs or sections for REST, SOAP, and Legacy (servlets/JAX-RS). Each section is a table listing endpoints (HTTP method, URL/path or operation, class, method). If OpenAPI spec files were found, the UI shows a link or embedded viewer for the OpenAPI YAML content. If WSDL/XSD files were found, a SOAP-specific view lists the services/ports/operations derived from them, and allows the user to view the raw WSDL or a summary (e.g. in a read-only text panel). This provides direct access to formal API specifications alongside the code-extracted info.

Additionally, Iteration 3 introduces an Asset Scanner to inventory non-code static assets in the repo (for documentation completeness). This includes finding image files (PNG, JPG, SVG, GIF) or other media in the repository. These are listed (with file path and maybe size) in a new asset_image table and included in the snapshot (assets.images). In the UI, a “Media Assets” section can list these files with options to view/download them. (This is useful for audits where documentation diagrams or images might be stored in the repo to be included in reports.)

6.4 Database Analysis: (Iteration 4) With the API layer done, the focus shifts to the persistence layer. CodeDocGen now performs deeper analysis of how the code interacts with the database:

Entity Details: All JPA entities (classes annotated @Entity) are processed. For each, the tool determines the corresponding table name (from @Table(name="...") if present, otherwise defaults based on naming conventions). It collects the primary key field(s) (fields annotated @Id, and if composite keys, notes those), and all fields with their types and column mappings (if @Column specifies a different name). Importantly, it captures relationships: for example, if an @OneToMany List in Entity A refers to Entity B, and B has the corresponding @ManyToOne back to A, these are noted in a relationships structure. This data can be used to draw an ER diagram and to understand bidirectional links. Each entity is saved as a row in db_entity table with a JSON blob of its fields and relationships for reference.

DAO/Repository Scanning: The tool identifies repository classes. This includes Spring Data repository interfaces (which extend JpaRepository or similar). For those, we know they correspond to an entity via generics (e.g. JpaRepository<Customer, Long> clearly ties to Customer entity). We enumerate the CRUD methods – Spring Data method names contain hints (e.g. findByEmail → SELECT, save → INSERT/UPDATE, deleteById → DELETE). Custom query methods or those annotated with @Query are also noted (we capture the query string if available). For each method, determine the target entity or table: by convention or explicit annotation. We also scan any custom DAO classes (concrete classes, often named *Dao or *Repository in older code) for database calls. This might involve looking for usages of JDBC templates or EntityManager calls in the method bodies – a bit heuristic, but method names and any SQL strings can guide classification. Each repository/DAO class and its operations are recorded as dao_operation entries (with fields: class, method, operation type, target entity/table, and an optional raw query snippet).

From these, a DbAnalysisResult is constructed (and included in the JSON snapshot as dbAnalysis): it carries an `entities` array (the canonical metadata captured from the JPA scanner), `classesByEntity` (mapping each entity/table to the repositories/DAOs that touch it), and `operationsByClass` (mapping each repository/DAO class to its inferred CRUD operations, target descriptor, and optional query snippet). This gives a dual perspective: “Which classes touch this table?” and “What does this class do to the database?” Both are valuable for understanding data access patterns and for compliance checks (e.g. ensuring all credit card data access is via certain approved methods, etc.). We ensure that any circular references in entity relationships (like Entity A ↔ Entity B) do not cause issues – since we build these structures from a static listing of fields rather than recursive traversal, we avoid infinite loops on bi-directional mappings.

A new API GET /project/{id}/db-analysis returns the database analysis payload (entities plus the two maps above). The UI “Database” page uses this to present: (1) an Entities & Classes table – each row is an entity with its table name, primary keys, and the list of DAO/Repo classes that use it; (2) a DAO Operations table – each row is a repository/DAO method with columns for class, method, operation (SELECT/INSERT/etc.), target entity/table, and an excerpt of the query or method logic. Additionally, this page can show a generated ERD diagram of the entities (if the diagram generation step has been done by now), giving a visual of table schemas and relationships. This iteration delivers immediate value by summarizing how data flows to the database.

6.5 Logger Insights: (Iteration 5) Building on the earlier logging scan, this feature centralizes all log usages and highlights those that may be risky. The backend’s LoggerScanner goes through all Java classes (including tests) to find logging statements. It normalizes different logging frameworks (e.g. calls to Log4j vs slf4j all captured similarly) and records each log call with: the class name (and file), line number, log level, the static message template, and any variables (placeholders) used. It then cross-references each log message and variables against the PII/PCI patterns from the security scanner. If a log message appears to include sensitive info (for example, logging a credit card number or personal data), the corresponding flags piiRisk or pciRisk are set to true on that log record. All log statements are stored in a new log_statement table and added to the snapshot (loggerInsights list).

At the same time, the PII/PCI scanning results from the security scanner are collected into a pii_pci_finding table and snapshot (piiPciScan list), if not already done in a prior step. This iteration ensures both the line-by-line sensitive findings and the flagged log entries are available via APIs: GET /project/{id}/logger-insights and GET /project/{id}/pii-pci.

The UI now gets two new sections:

Logger Insights: a page with a table of all logged messages. It shows columns like Class, Level, Message, Variables, “PII Risk?” and “PCI Risk?”, and the source line number. The UI provides richer filters so reviewers can combine class + log-level filters with free-text message searches and global “Expand All / Collapse All” controls. Combined with the row-level toggles (PII-only, PCI-only), this lets auditors focus on the few risky messages hidden inside very large projects. The user can expand snippets inline or use the global expand toggle to compare entire payloads. Download buttons still allow exporting the log table as CSV or PDF for reporting.

PCI/PII Scan: a page listing the raw sensitive findings across the repo. It’s a table with columns: File, Line, Snippet (a preview of the text around the match), Type (PII vs PCI), Severity, and an indication if the finding was ignored (suppressed). A toggle lets the user hide ignored findings, and there are CSV/PDF export options as well for this data.

These security-focused views provide immediate insight into whether the codebase might be logging sensitive data or containing secrets in files. By combining static rules with code analysis, CodeDocGen helps highlight potential compliance issues (like GDPR concerns or card data handling for PCI DSS).

6.6 Diagram Generation & Call Flows: (Iteration 6) With all underlying data in place, CodeDocGen generates diagrams to visualize the system architecture. A dedicated service takes the accumulated metadata (classes, relationships, call graph info, database schemas, etc.) and produces diagrams in text form (PlantUML and Mermaid). The diagram types include all those listed in Objective #7 above: Class, Component, Use Case, ERD, and Sequence (Call Flow) diagrams. Key considerations and steps in this process:

Class Diagram: Use the class metadata to identify relationships between classes. Every class becomes a node in the UML diagram. Draw inheritance lines for extends/implements relationships (solid arrow for extends, dashed for interface implementation). Also, parse member fields to find associations: e.g. if class A has a field of type B (and both are first-party), that can be represented as an association line. We will highlight first-party classes (perhaps with a different color or stereotype label) to distinguish them from external library classes. External classes (unless specifically included via the library scanning) might either be omitted or shown in a faded style.

Component Diagram: Identify the main layers/components of the app. Likely, group classes by stereotype: e.g. all Controllers in one component group, Services in another, Repositories/DAOs in another, plus any external systems. Then draw connections: e.g. Controller classes call Service classes (arrow from controller component to service component), Service to Repository, etc. We also include external entities: a database component that all Repositories connect to, any external SOAP services or REST APIs that the code calls (if we detected calls to external URLs or SOAP clients, represent those as external systems). The component diagram gives a high-level picture of the system’s structure and dependencies.

Use Case Diagram: Define actors and use cases. Likely we use a generic “User” actor (and possibly system actors for external systems calling APIs). Each API endpoint can be a “use case” (for example, a REST endpoint POST /orders could be a use case “Create Order”). We connect the User actor to each use case that a user can trigger (all REST endpoints that are user-facing). For SOAP, perhaps another actor (or the same if a user indirectly triggers it). This diagram shows the functionality provided by the system in terms of use cases and who/what triggers them.

ERD (Entity-Relationship Diagram): From the entity metadata, create a diagram of tables. Each entity becomes a box with its table name and key fields. Draw relationships (crow’s foot notation or similar) between entities based on foreign key relationships in the code (OneToMany, ManyToOne, etc.). Indicate primary keys and maybe important columns. Essentially, this is a class diagram restricted to the @Entity classes, focusing on their relationships.

Sequence Diagrams (Call Flows): Perhaps the most dynamic, these diagrams depict specific flows of method calls. We use the call graph constructed via static analysis (all method call relations within the code). Likely, we identify one or more entry point methods to generate sequences for – e.g. each API endpoint method could be an entry point for a sequence diagram of what it calls internally. For a given entry method, perform a depth-first traversal of method calls: each time method A calls method B, that’s a message in the sequence from A’s object lifeline to B’s lifeline. We continue down the call chain. We include all first-party classes as separate lifelines/participants. If a call goes out to an external class that is not in our first-party set, we have two options: if that class’s package matches one of the configured prefixes for libraries (meaning we did parse it), we can continue the sequence into that class. Otherwise, if it’s truly external (like a JDK call or third-party library not included), we can either omit it or show it as an external entity with no further expansion. For example, a call to java.util.Logger might simply be shown as an external call and not expanded. We make this behavior user-configurable via the “Show external calls” toggle in the UI’s Call Flow page. If toggled off, we might collapse any call that goes to an external class beyond the scope. If on, show it (to the extent we can, or at least as a black-box participant). The sequence diagram generator must be cycle-aware: it uses a stack to track the current call path. If it encounters a method that is already in the call stack, we insert a note or placeholder instead of looping. For instance, if A → B → C → A occurs, when generating the call from C back to A, we will output a message like “(cycle to A.method)” and not dive deeper. This prevents infinite recursion in the diagram output.

Each diagram (in PlantUML and Mermaid text form) is saved to the diagram table with its type and source text, and if an SVG was rendered (for PlantUML), the path to the image is stored. The diagram data is also attached to the JSON snapshot (diagrams list with type and source strings) for external use. New APIs GET /project/{id}/diagrams (returns list of all diagrams with their source text) and GET /project/{id}/diagram/{diagramId}/svg (to fetch a specific rendered SVG) are provided for the frontend or direct download.

On the frontend, a “Diagrams” section is added with a tabbed interface for each diagram type. For each diagram tab, the UI will either display the rendered SVG (if available, e.g. for class or component diagrams via PlantUML) or potentially render Mermaid diagrams on the fly (since Mermaid can be rendered client-side if needed). The user can click “View PlantUML Source” or “View Mermaid Source” to see the raw text definitions, and can download the SVG if needed. For sequence diagrams, the UI includes the toggle “Show external calls” to include/exclude external-library interactions for clarity. Additionally, because diagrams for very large codebases can be overwhelming, we plan to include simple filtering on diagrams (e.g. filter classes by package name, or limit to certain modules) to make them more navigable. Another page “Call Flow” in the UI navigation is dedicated to sequence diagrams and an outline of detected external service calls; it might simply show the main sequence diagram (perhaps the one starting from the primary entry point like a main REST controller) and list any external endpoints the code calls out to (for example, if the code uses RestTemplate to call an external API, list those target URLs).

6.7 Gherkin Feature Extraction: (Implemented by Iteration 7) CodeDocGen scans the repository for BDD feature files (*.feature). It looks in standard locations (src/test/resources or anywhere in the repo) for these files. For each feature file, it parses the content (which is plain text in Gherkin syntax) to extract the Feature name and the list of Scenario names (and possibly the steps within each scenario). These are collated into a gherkinFeatures section in the JSON snapshot. The purpose is to include any behavior-driven specifications in the documentation. The UI “Gherkin” page simply lists each feature with its scenarios, and allows expanding to read the steps in each scenario. This provides testers and auditors with insight into what high-level behaviors the tests are covering.

6.8 Export and Documentation: (Iteration 7) Finally, CodeDocGen can produce a consolidated technical documentation export. The primary export format is an HTML document tailored for Confluence (or general documentation sharing). A GET /project/{id}/export/confluence.html endpoint returns a single HTML page containing the entire analysis of the project. The content of this document is structured as follows (each section corresponds to a heading or section in the HTML):

Overview: Project name, repository URL, build info (Maven coordinates and Java version), and an analysis timestamp.

API Endpoints: Tables listing all discovered endpoints, grouped by category (REST, SOAP, and Legacy). Each entry shows method, path, class, etc., similar to the UI.

Diagrams: All diagram types are included. Instead of embedding images, the document includes the PlantUML/Mermaid source code for each diagram enclosed in <pre><code> blocks. (This is intentional for Confluence: by copy-pasting the PlantUML text into a Confluence PlantUML macro, the diagrams can render on Confluence. Mermaid source can be similarly used with a Mermaid plugin, or simply kept as text.) The diagrams covered are: Class, Component, Use Case, ERD, DB Schema (which might be same as ERD), and Sequence.

Database Analysis: Two subsections – “Entities and Interacting Classes” (listing each entity with the DAOs that use it) and “DAO Operations by Class” (each repository/DAO with its CRUD methods) – essentially textual versions of the database analysis results.

Logger Insights Summary: A summary of risky log statements. Rather than listing every log (which could be lengthy), this section could enumerate how many log statements were flagged for PII/PCI, perhaps listing a few examples or the classes with most issues. (Exact content can be adjusted; the goal is to summarize security-relevant logging findings.)

PCI/PII Scan Summary: Similar treatment – summarize how many findings of each type and severity, possibly tabulating a few, or simply stating that detailed CSV/PDF is available. The HTML could include the full table of findings if needed, but for Confluence brevity, a summary might suffice.

Gherkin Summary: List the names of all Feature files and their scenarios, to document tested behaviors.

Build & Tech Stack Info: Note the project’s Maven coordinates, Java version, and any notable frameworks (this could be gleaned from dependencies or known annotations). Essentially, a recap of what technologies are in use (e.g. “Spring Boot, Hibernate, JUnit 5”, etc., if easily determined).

Footer: A generation timestamp and a note like “Generated by CodeDocGen for [repo URL] on [date]”.

The HTML export is primarily plain text and pre-formatted blocks (no images), to ensure it can be cleanly transferred into Confluence or other documentation sites without needing binary attachments.

In addition to the HTML, the system provides direct export of the raw data: GET /project/{id}/export/snapshot returns the full JSON snapshot (ParsedDataResponse) which can be downloaded and fed to other tools. CSV and PDF exports for the Logger Insights and PII findings are also available (endpoints like /export/logs.csv, /export/pii.pdf, etc.), facilitating quick sharing of those specific reports with security teams. The Export page in the UI offers buttons for each of these (Download Technical Doc, Download JSON, Download CSVs) and even previews the HTML export so the user can see the formatted documentation before using it.

9. Definition of Done

CodeDocGen v1 is considered feature-complete and ready when it meets all the requirements above, and the end-to-end user workflow is satisfied:

A user can input a Git repository URL and trigger analysis, and all major insights are generated: an up-to-date Project Overview, a complete API endpoint catalog (covering REST, SOAP, etc.), database entity mappings, sequence/call-flow visualizations, class/component/use-case diagrams, lists of classes, BDD scenarios, logger analysis with PII/PCI flags, and sensitive data scan results. All these are available through the UI and via export.

The data is persisted so that the user can navigate the UI without re-running analysis, and if they do re-run on the same repo, the old data is cleanly replaced.

Cycle handling is verified across the board – no infinite loops or crashes occur due to cyclic code structures; instead, the output clearly indicates cycles where they occur (e.g. diagrams showing a “cycle” note).

The React frontend provides a clean dashboard with all sections accessible via sidebar, and interactive elements (filters, toggles) working as specified. The UI is polished for both light and dark mode.

All export features work: the CSV/PDF files download and open properly, the Confluence HTML contains all relevant info, and the JSON snapshot can be obtained for external use.

Security is in place: the API key protection is enforced on all appropriate endpoints, and no sensitive tokens or data are exposed in any outputs (aside from the intentional PII findings which are the point of the scan).

The tool supports the range of environments intended – it has been tested on Java 8, 11, 17, and 21 codebases (ensuring backward compatibility in parsing), with both simple and multi-module Maven projects, and has handled both Spring Boot modern projects and older Java EE style projects to cover the variety of API types. It can also accommodate user configuration for first-party prefixes, meaning internal libraries can be included in analysis if desired.

Once all the above is verified, CodeDocGen v1 can be delivered as a self-contained package for users (e.g. as a runnable JAR with an embedded frontend or as separate backend and frontend bundles, along with documentation on usage). This PRD encapsulates the full scope of CodeDocGen v1’s intended functionality and quality criteria.

## 7. UX & Interaction Requirements

### 7.1 Tab navigation and responsiveness

* The dashboard tabs (Overview, API Specs, Database, Logger Insights, PCI / PII Scan, Diagrams, Export) must remain accessible at every viewport width. Desktop/tablet layouts keep a horizontally scrollable pill bar that wraps as space allows. At sub‑tablet widths (<720 px) the pill bar hides and a native `<select>` mirrors every tab so routes like **Diagrams** never disappear off-screen.
* The tab rail stays sticky at the top edge of the overview card so navigation is always in reach while scrolling long tables or diagram lists. Use `role="tablist"`/`aria-current` to keep screen readers informed of the active panel.

### 7.2 Analyzer form behavior

* After a successful analysis (`status === ANALYZED_METADATA`), collapse the left analyzer form into a compact “Latest analysis” summary that surfaces project ID, repo URL, and analyzed timestamp.
* Provide explicit “Hide panel / Show form” affordances plus an **Edit inputs** button so users can quickly rerun analysis. On errors, auto-expand the form to keep the inputs immediately editable.
* The collapsed state should also offer a secondary action (e.g., “View dashboard”) to jump the focus back to the Overview tab.

### 7.3 Loading states and status messaging

* Replace ambiguous dots/spinners with a deterministic **analysis timeline** rendered inside the analyzer card. The timeline streams these steps: `Cloning repository and running analysis`, `Loading project overview`, `Mapping APIs and OpenAPI specs`, `Inspecting database entities`, `Gathering logger insights`, `Scanning for PCI / PII risks`, `Generating diagrams`.
* Each step transitions through `Queued → In progress… → Done` (or `Skipped` if the backend fails before reaching it). The component must announce updates via `aria-live="polite"` for assistive tech.

### 7.4 Empty states and hints

* Every empty panel must suggest a next action. Example: OpenAPI sections say “No OpenAPI specs found. Add Swagger annotations or include an `openapi.yaml` / `swagger.json` so CodeVision can render docs.” Similar guidance should exist for SOAP specs, diagrams, logger tables, etc.
* Use restrained styling (muted text, tight spacing) so guidance compliments, rather than overwhelms, the surrounding section.

### 7.5 Responsiveness expectations

* The grid (analyzer card + overview card) stacks vertically on narrow screens without horizontal scrolling. Diagram viewers keep SVGs scrollable within their own pane instead of stretching the page.
* Sticky navigation, dropdown fallback, collapsible analyzer, and the progress timeline are now part of the v1 acceptance criteria.
