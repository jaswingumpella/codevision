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
4. Persist results in H2:

   * Structured tables for the UI and exports
   * A full project snapshot JSON blob (`ParsedDataResponse`) for external tooling
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
* H2 for persistence
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
* Local runtime only (no internet dependency at analysis time beyond cloning the repo)

---

## 5. Core Concepts

### 5.1 Project

A "project" = one Git repo URL.
The application treats `repoUrl` as a unique identifier.

Behavior:

* On each analysis run for a given repo URL:

  * Clone repo
  * Wipe and replace previously stored data for that project in H2
  * Recompute analysis
* We do not retain analysis history or per-commit versioning.
* We do not store commit hash.

### 5.2 Snapshot

After analysis, the system builds `ParsedDataResponse` — a full JSON view of:

* APIs
* Classes
* Database analysis
* Diagrams
* Gherkin features
* Logger Insights
* PCI/PII findings
* Tech stack info
* Metadata dumps (OpenAPI YAML, WSDL, XSD, etc.)

This snapshot is persisted in H2 and is downloadable via API for pasting into external assistants like GitLab Duo.

### 5.3 Cyclic safety

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

## 6. Detailed Feature Areas

### 6.1 Repo ingestion

**Inputs**

* Git repo URL (public or private)

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

* Repo is cloned into a temporary working directory
* Project name derived from repo URL
* Multi-module Maven supported:

  * Parse root `pom.xml`
  * Collect modules and analyze all of them
  * If the root is an aggregator (no code), walk nested directories for additional `pom.xml` files and treat those as modules automatically

**Outcome**

* Project entry created/updated in H2:

  * `project_name`
  * `repo_url` (unique)
  * `last_analyzed_at`
  * Maven group/artifact/version
  * Java version (from pom / maven-compiler-plugin)

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

* `class_metadata` table in H2
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

* `api_endpoint` table in H2
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

   * `classesByEntity`:

     * Map: entityOrTable → list of DAO/repository classes that interact with it
   * `operationsByClass`:

     * Map: DAO/repo class → list of { methodName, operationType, entityOrTable, rawQuerySnippet }

4. Cycles:

   * Entities that reference each other bi-directionally must not break analysis.
   * We compute mappings from already-collected metadata, not by deep recursion, to avoid loops.

**Storage**

* `db_entity` table
* `dao_operation` table
* `DbAnalysisResult` embedded in snapshot as `ParsedDataResponse.dbAnalysis`

**UI**

* “Database” page:

  * Section 1: “Entities and Interacting Classes” (uses `classesByEntity`)
  * Section 2: “Detailed Operations by DAO/Repository Class” (uses `operationsByClass`)
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

    * Class name text filter
    * Log level dropdown
    * Toggles for “show only PII risk” / “show only PCI risk”
  * “Expand All” / “Collapse All” to show variable details
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

* Load configurable rules from `application.yml` via `@ConfigurationProperties("security.scan")`:

  * Each rule:

    * `pattern` (keyword and/or regex)
    * `type`: `PII` or `PCI`
    * `severity`: `LOW`, `MEDIUM`, `HIGH`
* Also load `ignorePatterns` to suppress known false positives
* Scan ALL text-based files in the repo:

  * `.java`, `.yml`, `.yaml`, `.properties`, `.xml`, `.sql`,
  * `.log`, `.wsdl`, `.xsd`, `.feature`, etc.

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
  * Toggle: Hide ignored
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

   * Call chains traced across methods
   * Internal calls between first-party classes (highlighted) and external classes (dimmed)
   * Outbound service calls (e.g. via `RestTemplate`, `WebClient`, Feign)
   * Cycle-safe:

     * If a method call repeats within the current traversal path, insert a `"(cyclic reference...)"` node rather than recurse

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
* For large codebases:

  * Support basic filtering / grouping (e.g. filter by package prefix)
  * Goal is to reduce noise without crashing the browser

---

### 6.8 Call Flow / Sequence Data Model

A call flow is derived from static method-to-method call analysis:

* Build a graph of caller → callee
* Track outbound service calls for inter-service nodes
* During diagramgen DFS:

  * Maintain `currentPath` stack
  * On re-entry to a method already in `currentPath`, emit a “cyclic reference” note instead of continuing
  * This prevents infinite recursion in diagrams

This call flow view is included in `ParsedDataResponse.callFlows`, and also used to generate the Sequence Diagram entry in `diagram`.

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
* `GET /project/{projectId}/diagrams`
* `GET /project/{projectId}/gherkin`
* `GET /project/{projectId}/metadata`

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
2. API Specs
3. Call Flow
4. Diagrams
5. Database
6. Classes
7. Gherkin
8. Logger Insights
9. PCI / PII Scan
10. Metadata
11. Export

### Global UI elements

* API key injection in Axios
* Dark/light theme toggle
* Progress feedback while `/analyze` is running

  * Even though backend is synchronous, frontend should still show “Analyzing…” until it gets the response

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
* Button: “Download ParsedDataResponse.json” (again, for convenience)

---

## 9. Persistence / H2 Schema (Logical)

**project**

* `id` (PK)
* `repo_url` (UNIQUE)
* `project_name`
* `last_analyzed_at` (timestamp)
* `java_version_detected`
* `maven_group_id`
* `maven_artifact_id`
* `maven_version`

**project_snapshot**

* `project_id` (FK → project.id)
* `snapshot_json` (CLOB)

  * full `ParsedDataResponse`

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
   * H2 is embedded/local
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
2. All derived content is persisted to H2 for that repo, overwriting previous runs for that same repo.
3. Cycles in code do not crash or loop forever; instead, diagrams and call flows show a readable cyclic reference marker.
4. The React UI shows all sections in a left-nav dashboard, with light/dark mode, filters, toggles, downloads.
5. The backend exposes:

   * CSV/PDF exports for security review
   * HTML export for pasting into Confluence
   * A full JSON snapshot for feeding to external tooling (GitLab Duo, Jenkins, ServiceNow, audit bots, etc.).
6. All APIs except healthcheck require `X-API-KEY`.

This document is the authoritative product definition for CodeDocGen v1.
