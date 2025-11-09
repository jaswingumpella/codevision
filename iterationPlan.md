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

## Iteration 3 – API & asset surfacing *(Status: ✅ Completed – see [`docs/iteration-3-completion.md`](docs/iteration-3-completion.md))*

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

**Status:** ✅ Completed – see [`docs/iteration-4-completion.md`](docs/iteration-4-completion.md) for delivery notes.

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
* Unit tests cover entity extraction and DAO classification heuristics.

---

## Iteration 5 – Logger Insights + PCI/PII scanning

**Status:** ✅ Completed – see [`docs/iteration-5-completion.md`](docs/iteration-5-completion.md) for delivery notes.

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

**Status:** ✅ Completed – see [`docs/iteration-6-completion.md`](docs/iteration-6-completion.md) for delivery notes.

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

## Iteration 7 – Full project export (Confluence-style HTML) + Metadata dump *(Status: ✅ Completed – see [`docs/iteration-7-completion.md`](docs/iteration-7-completion.md))*

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
Updated Iteration Plan

Below is the updated iteration plan for implementing CodeDocGen, reflecting the completion of Iterations 1 and 2 and outlining the work for remaining iterations (3 through 7) to achieve the full feature set. Each iteration delivers a vertical slice of functionality, ensuring that by the end of the iteration there is a usable increment of the product.

Iteration 1 – Bootstrap & Repo Ingestion (✅ Completed)

Goal: Establish the project structure and allow a basic analysis run that clones a repo and stores project info.

Backend Deliverables: Set up a Spring Boot application (Java 21). Implement GitCloneService to clone Git repositories (supporting auth via git.auth.username/token in config for private repos). Implement a ProjectService and project DB table to save projects (fields: repo URL, project name, last analyzed timestamp). Enforce API key security on endpoints via a filter (if security.apiKey is set). Provide the POST /analyze endpoint to accept {"repoUrl": "..."}; on call, clone the repo and insert/update the project record, then respond with {"projectId": X, "status": "ANALYZED_BASE"}. No detailed parsing yet (this iteration doesn’t go beyond confirming the clone and DB write).

Frontend Deliverables: Initialize a React app (via Vite). Create a simple page with a form to input a Git repo URL (and an optional API key, if the backend requires one). Clicking “Analyze” calls the backend /analyze endpoint. Upon success, display the returned project ID (and maybe the derived project name) as a confirmation. Set up basic project structure for the UI with Axios (including API key header insertion) and a success/failure notification component.

Result: By the end of Iteration 1, a user can enter a repository URL in the UI, the system will clone the repo and store minimal info in H2, and the UI will show a confirmation with the project identifier. Re-submitting the same URL will update the existing record (overwrite behavior confirmed). This proves the end-to-end pipeline (UI -> backend -> DB) for a simple case and sets up the groundwork for adding analysis features.

Iteration 2 – Source Discovery & Metadata (✅ Completed)

Goal: Extend the analysis to actually parse the repository’s source code and collect basic metadata, providing an initial project “overview”. Ensure cycle-safe handling in this process.

Backend Deliverables: Introduce JavaSourceScanner to walk through the repository’s src/main/java and src/test/java directories (for each Maven module, as detected from the root POM). Integrate JavaParser to parse each Java file. For every class, create a ClassMetadataRecord capturing its fully-qualified name, package, implemented interfaces, annotations, source set (Main or Test), and an inferred stereotype (e.g. CONTROLLER, SERVICE, etc.). Implement the heuristic to mark classes under certain packages as first-party (userCode=true) – initially using default prefixes (com.barclays, com.codeviz2) with the intent to make this configurable later. All parsed classes are saved to a new class_metadata table in H2.

Add a BuildMetadataExtractor to parse pom.xml: retrieve the project’s Maven groupId, artifactId, version, and the Java target version (from plugin or properties). If the project is multi-module (root POM has modules), iterate through modules to ensure their sources are scanned too. Store build info in the project record (new columns for group, artifact, version, java version) and also prepare it for the overview JSON.

Implement YamlScanner to find important YAML files: gather any OpenAPI spec files (openapi*.yml) and application config files (application.yml, application-*.yml). Read their content (or a summary) and include them in a MetadataDump structure. Similarly, if any WSDL or XSD files are present, note their paths (we’ll fully process them in Iteration 3).

Create the ParsedDataResponse data model class to start aggregating these results. Populate it with: basic project info (name, URL, timestamp), the build info, a summary count of classes, the list of class metadata (possibly truncated for the overview), and the collected OpenAPI spec metadata (file names, etc.). Persist the full JSON to a new project_snapshot table (one row per project). Provide GET /project/{id}/overview which returns the current ParsedDataResponse (this will be used by the UI to display the overview).

Implement cycle detection in the scanning logic: although at this stage we mostly just list classes, we prepare for future deeper graphs by ensuring if any traversal occurs (e.g. if we were mapping class relationships or parsing annotation dependencies) we maintain a visited set to break cycles. In practice, class parsing via JavaParser won’t infinitely recurse, but this principle will be applied strongly in later iterations (call graphs, etc.).

Frontend Deliverables: Enhance the React app with a basic project Overview panel/page. After an Analyze is done (or when the user selects a project), call /project/{id}/overview to fetch the snapshot JSON. Display key information: the build info (GroupId:ArtifactId:Version, Java version), and class counts (e.g. “X classes total (Y main, Z test)”). If any OpenAPI specs were found, list their filenames and perhaps a download link for each. If multiple modules were present, show a list of modules or include module info as part of the build info. The UI should be styled clearly (using Material UI cards or similar).

Also, set up the frontend’s routing or state management to handle multiple pages (preparing for upcoming pages like API specs, diagrams, etc., even if they are empty placeholders for now). Perhaps introduce a sidebar navigation with at least “Overview” active, and placeholders for upcoming sections to demonstrate the intended layout. Ensure that Axios is configured to send the API key header if needed, and that the dev server proxy is routing API calls to localhost:8080 correctly.

Verification & Outcome: By end of Iteration 2, running an analysis on a sample project (e.g. Spring Petclinic) will result in the UI overview showing the project’s Maven info and counts of classes, confirming that source code was parsed and data stored. The snapshot JSON can be retrieved (e.g. via curl) to verify it contains the expected fields (projectId, projectName, buildInfo, classes[], metadataDump, etc.). Any cycles in class references would not crash the system (though we haven’t visualized them yet). At this point, we have a functional skeleton that knows about the project’s structure and can expose that data.

Iteration 3 – API Inventory & Asset Surfacing (🚧 Planned)

Goal: Identify all externally exposed APIs in the codebase (REST, SOAP, etc.) and gather documentation artifacts. Also, inventory static documentation assets (images) present in the repo. Deliver a UI page to display the API catalog and any available API docs (OpenAPI, WSDL).

Backend Tasks: Implement an ApiScanner component that uses the class metadata and annotations to find controllers/endpoints:

REST: Find classes annotated with @RestController or @Controller. For each, find methods with mapping annotations (@GetMapping, @PostMapping, etc., or the older @RequestMapping). If a controller method has no direct annotation but the class implements an interface, check that interface’s methods for mapping annotations – this covers the case of code generated from OpenAPI interfaces where the implementation class might be empty. Resolve the full path for each method (including class-level @RequestMapping prefixes).

SOAP: Find Spring-WS endpoint classes (@Endpoint). For each, gather its methods annotated with message-mapping annotations like @PayloadRoot (which indicates the SOAP action or request name). Also, if WSDL files were found in Iteration 2, parse them using a simple XML parser to extract high-level info: e.g. service name, port name, and operation names. This parsed info can be added to the MetadataDump (e.g. metadataDump.wsdlServiceSummary).

Legacy Servlets: Find any class that extends HttpServlet. For each such class, note that it provides HTTP endpoints via doGet, doPost, etc. If possible, parse the web deployment descriptor (web.xml) in the repo (if any) to find URL mappings for these servlets; otherwise, document them by class name.

JAX-RS: Search for classes or methods with JAX-RS annotations (@Path, etc.). Each class with @Path is like a base resource; methods with @Path or HTTP method annotations define endpoints. Combine class-level and method-level paths to a full path.

Normalize all the above into instances of ApiEndpointInfo (fields: protocol, httpMethod, pathOrOperation, controllerClass, controllerMethod, plus perhaps a reference to related spec docs). Save these into the api_endpoint table (ensuring to clear old entries on re-analyze). Update the snapshot JSON to include a list of apiEndpoints.

Next, implement an AssetScanner to find non-code assets. Specifically, scan the repository for image files (PNG, JPG, SVG, GIF) in docs or resources folders. For each image found, collect metadata: file path (relative), maybe file size or a simple hash for integrity. Store these in a new asset_image table and include in snapshot (assets.images[]). We do not extract image content (that would bloat the JSON), just references. These could later be used to bundle documentation or for the user to review what diagrams or images exist in the repo.

Provide a new endpoint GET /project/{id}/api-endpoints returning the list of API endpoints (with all details, and possibly enriched with links to documentation artifacts). Also, perhaps extend GET /project/{id}/metadata to include the raw OpenAPI spec text and a summary of WSDL/XSD (so the frontend can display those). (In PRD, /metadata was intended to return dumps of OpenAPI, WSDL, etc., for AI usage.)

Frontend Tasks: Create an “API Specs” page in the React app. This page will present the API endpoint catalog in an organized way: potentially use tabs for REST, SOAP, and Legacy (Servlet/JAX-RS). Each tab contains a table of endpoints relevant to that category. For example, the REST tab would list each REST endpoint with columns: HTTP Method, Path, Controller Class, Method. SOAP tab might list each SOAP operation with columns: Operation Name, Endpoint Class, Method (and maybe the request/response names). The Legacy tab can list servlets with HTTP method and servlet name/path.

Below or alongside the tables, incorporate viewers for the API specification documents:

If an OpenAPI spec was found, show a panel with the OpenAPI YAML displayed (syntax-highlighted). If multiple specs, list them (perhaps allow download).

If WSDL/XSDs were found, provide a section to display the parsed summary (list of services/ports/operations). Allow the user to toggle viewing the raw WSDL or XSD content (which could be lengthy XML). Possibly use an expandable text area for these.

Additionally, add a section on this page for Media Assets if images were found: list the image files with maybe an icon or file type and a download link. This shows any diagrams or pictures included in the repo’s documentation.

The UI should be easy to navigate: perhaps an accordion or sub-tabs for “OpenAPI Spec” and “WSDLs” to not overwhelm the main table. Ensure that long text (like YAML or XML) is scrollable within its container.

Expected Outcome: By end of Iteration 3, the system can enumerate all externally exposed endpoints in the project, and the UI shows them categorized. A user can see, for example, all REST endpoints with their URLs and controllers, any SOAP web services with their operations, etc. They can also view the actual OpenAPI spec content if available (e.g. to copy it or inspect it) and see WSDL details for SOAP services. If the project had images (say architecture diagrams in the repo), those are now listed. Essentially, the tool now provides a complete API surface overview of the project. This is valuable for understanding what the service exposes and documenting it. (At this stage, diagrams are not yet generated, and deeper analysis like DB or security is coming next.)

Iteration 4 – Database Analysis (Entities & CRUD) (🚧 Planned)

Goal: Make the tool outline how the application interacts with its database, by analyzing entity models and data access code. Provide a UI view for this.

Backend Tasks: Implement JpaEntityScanner and DAO analysis as described in the PRD:

Process all classes annotated @Entity. Collect their schema details: table name, primary key(s), fields (name and type), relationships (for each @OneToMany, @ManyToOne, etc., note the related entity and cardinality). Save each entity to the db_entity table (with JSON fields for the list of fields and relationships).

Implement DaoAnalyzer: find Spring Data repositories (interfaces that extend JpaRepository or similar). For each, determine the domain entity via generic parameter. List out all methods. For standard CRUD methods (save, delete, findAll, etc.), assign operation types easily. For query methods (e.g. findByEmailAndStatus), use naming conventions and/or if an @Query is present, parse that to figure out if it’s a SELECT, etc. Also capture any custom implementations (if a repository has an Impl class with custom methods). Find any classic DAO classes: these might be classes in the code that have methods doing JDBC calls or using EntityManager. This can be trickier: we might search for certain patterns, like methods that execute SQL queries or call repository methods. We might use naming (class ends with Dao or Repository, method names like getAllUsers, insertOrder, etc.) to guess. Even if heuristic, including them is beneficial. For each such method, attempt to determine the target table or entity (perhaps by looking at any SQL string literals or the method name). Mark the operation type appropriately.

Build the two maps for DbAnalysisResult: classesByEntity and operationsByClass as described earlier. This essentially reorganizes the collected info for easy consumption. Embed this object into the snapshot JSON (dbAnalysis).

Create the new endpoints: GET /project/{id}/db-analysis which returns the structured DB analysis (or simply the relevant part of the snapshot). No separate DB-specific tables beyond db_entity and dao_operation are strictly required for the functionality, since we can generate the maps on the fly from those tables or even directly from class metadata. However, storing them (as done above) helps if we want to query or join in SQL. Ensure cycle safety: in case entities reference each other (A has B, B has A), our relationships info might show both directions but we will not recursively traverse beyond one hop when building maps – we rely on the static lists, so we won’t infinite loop.

Frontend Tasks: Develop the “Database” page in the UI. This page will likely have two main sections (which can be shown one below the other or as two tabs within the page):

Entities and Interacting Classes: Essentially a table where each row is an Entity (or table) and one column lists all the repository/DAO classes that interact with it. We can format it as: Entity Name (perhaps with table name in parentheses) – and then a comma-separated or bulleted list of class names. If an entity is not used by any repository (which would be unusual), it might still appear with an empty usage list.

Operations by Class: A table where each row is a DAO/Repository method. Columns: Class (e.g. OrderRepository), Method (deleteById), Type (DELETE), Entity/Table (Order), Query (if we have a snippet or JPQL/SQL, include a short excerpt). We might sort this table by class then by operation, or group by class (e.g. show class name as a subheader with its methods listed). The goal is to let a user pick a specific data access class and see everything it does, and also see for each entity which classes touch it.

Optionally, if the ERD diagram has been generated by now, the top of the Database page could show the ERD (SVG image or Mermaid diagram) for a visual schema reference. This depends on integration with iteration 6, but we can plan the placeholder: e.g. an <img> or a Mermaid viewer showing the ERD if available.

Ensure the UI is user-friendly: perhaps use expandable panels – e.g. a list of entities, click one to expand and see classes; and a list of DAOs, click to see methods. Or simply tables as described. Provide sorting or filtering if the lists are long (maybe a filter by entity name or class name). The information here is crucial for understanding data flow and for tasks like ensuring compliance (e.g. check that all accesses to certain tables go through specific approved paths).

Expected Outcome: After Iteration 4, the user can go to the “Database” section and immediately see a comprehensive picture of how the code handles persistence. For example, they could see that CustomerEntity is interacted with by CustomerRepository and LegacyCustomerDAO, and that in those classes, there are methods like findByEmail (SELECT), save (INSERT), etc., targeting the customer table. This bridges the gap between code and database, which is very useful for engineers and auditors. Combined with the earlier API info, we now cover both the input (API) and output (DB) sides of the application’s functionality.

Iteration 5 – Logger Insights & PII/PCI Scan (🚧 Planned)

Goal: Augment the static analysis with security-focused insights: centralized logging review and a comprehensive scan for sensitive data in the codebase. Provide UI pages for both.

Backend Tasks: Implement the PiiPciInspector and integrate it with scanning:

Define a configuration structure for security scan rules (security.scan.rules list in application.yml). Each rule has a regex or keyword pattern, a category (PII or PCI), and a severity level. Define common patterns (e.g. for PII: “password”, “SSN”, “passport”, for PCI: credit card regex patterns, CVV, etc.). Also define ignorePatterns for known false positives (e.g. “use password in variable names might be false positive, etc.). Load these into the PiiPciInspector service.

Text file scanning: Go through all files in the repo (we can reuse the clone on disk). For each file that is of a text type (we consider extensions: .java, .xml, .properties, .yml, .yaml, .sql, .log, .txt, .feature, etc.), read line by line (or entire content) and apply each rule’s pattern. If a match is found, create a Finding record: record the file path, line number (if we do line-by-line scanning), snippet of text around the match, the rule type (PII vs PCI), and severity. Also mark if the match should be ignored (if it matches an ignorePattern). This results in a list of findings, which we persist in pii_pci_finding table.

Logging scan: Implement LoggerScanner to traverse the AST of each Java class (or potentially simpler, use regex on source for log. calls). Identify all log statements. For each, capture class and line number, log level, message template string, and variables. This likely can be done via JavaParser by checking method call expressions where the callee’s name is something like info, debug etc. and the caller is a logger instance. Once captured, run the message + variables through the PiiPciInspector’s rules. If any pattern matches in the concatenated message (or variables content, though we mostly just have variable names, so probably check the message for obvious things like "SSN" in the log text), then mark that log entry’s flags accordingly. Save all log entries in log_statement table.

Results integration: Include these findings in the ParsedDataResponse: add loggerInsights (list of log statements with fields: class, level, message, variables, piiRisk, pciRisk) and piiPciScan (list of findings: file, line, snippet, type, severity, ignored). Provide new API endpoints GET /project/{id}/logger-insights and GET /project/{id}/pii-pci to retrieve each set in isolation (the UI might use these to load each page). Also implement export endpoints for these: GET /project/{id}/export/logs.csv (and .pdf) and similarly /export/pii.csv (.pdf). These will generate a CSV or PDF of the respective data (likely using a simple CSV library and PDF report generator). The CSV can be straightforward with headers; the PDF might be a nicely formatted table.

Frontend Tasks: Develop two new pages in the UI for Logger Insights and PCI/PII Scan.

Logger Insights page: Display the table of logged statements. Include UI controls for filtering as described: a text box to filter by class name (probably do case-insensitive contains match on the class column), a dropdown to filter by log level (INFO/WARN/ERROR/etc.), and two toggle switches like “Show PII only” and “Show PCI only” which, when activated, filter the table to only rows where the respective flag is true. The table columns: Class, Level, Message, Variables, PII?, PCI?, Line #. The Message might be truncated if very long, but allow the user to expand (maybe by clicking the row or an expand icon) to see the full message and variables in context. Implement an “Expand All / Collapse All” button to toggle showing full details for every log entry if desired (this could simply show the variables column fully or show multiline messages). Provide buttons “Download CSV” and “Download PDF” which trigger downloads from the export endpoints (maybe just open the URL or use an Axios call and then prompt save).

PCI/PII Scan page: Display the table of sensitive data findings. Provide a toggle “Hide Ignored” to filter out any findings that were marked as ignored. Columns: File, Line, Snippet, Type (PII or PCI), Severity (Low/Med/High). This table can potentially be large, so maybe paginate or allow scrolling. Provide CSV/PDF download buttons as well. Possibly add a filter by filename (text search) if many files are listed.

Both pages should reuse a consistent table styling, and the filtering toggles should be intuitive (you might use checkboxes or chips to indicate PII/PIC filters). Test with a sample project to ensure that if no findings or no risky logs, the pages handle that gracefully (e.g. show “No issues found, great job!” or simply an empty state).

Expected Outcome: By the end of Iteration 5, CodeDocGen becomes a useful security auditing tool. A user can inspect the Logger Insights page to quickly see if any sensitive data might be getting logged in plain text (which is a common security concern). For example, if they see logs like logger.info("User password is " + password), it would appear flagged in red (PCI/PII risk). The PII/PCI Scan page will highlight other potential issues, like hard-coded secrets or sample data in files. These features make the product valuable not just for documentation but also for compliance and security review. The CSV/PDF exports mean the user can easily share the findings with others (e.g. attach to a ticket or email).

Iteration 6 – Diagram Generation & Call Flow Visualization (🚧 Planned)

Goal: Automatically generate architecture diagrams (class, component, use case, ERD) and sequence diagrams for call flows, using the data gathered. Implement backend logic for diagram generation and integrate into UI with viewing and filtering options. Also incorporate analysis of selected library classes to extend call flows where needed.

Backend Tasks: Develop a DiagramBuilderService to create diagrams. Key sub-tasks:

Call Graph Construction: First, build a global call graph of the codebase if not already done. One way is to use the JavaParser AST to find all method call expressions and map them. Alternatively, analyze bytecode or use a library for call graph. For simplicity, we might restrict to calls within first-party code: e.g. go through each method in each class (we have the AST) and find references to other classes’ methods. Build a map: caller (FQN#method) -> list of callee (FQN#method). Include calls to external classes as well if they appear. This call graph will be used for sequence diagrams. Store it in memory or, if needed, in a structure that can be put in the snapshot (callFlows), and make sure we persist the invoked method names so downstream diagrams can label every arrow precisely (including self-loops when a class calls another method on itself).

Generate Class Diagram: Use the class metadata (plus perhaps call graph or field analysis for associations) to generate PlantUML and Mermaid definitions. E.g. in PlantUML: start with @startuml and list all classes (maybe grouping by package). For each class, if we have stereotypes, we can adorn them (PlantUML allows adding stereotypes or icons for classes, but at least we can use annotations like <<Controller>>). Then add relationships: for each class, if it extends another, do ClassA <|-- ClassB. If it implements an interface, do ClassB <|.. InterfaceX. For associations, if class A has a field of type B, do ClassA --> ClassB : has (composition or dependency arrow). We might want to limit associations to important ones to avoid clutter. Possibly only show associations between first-party classes. Use colors or labels to highlight userCode vs external. Repeat the same information in Mermaid format (classDiagram syntax).

Generate Component Diagram: Determine components: e.g. one component representing all Controllers, one for Services, one for Repositories, one for External Systems, one for Database. Or perhaps individual classes as components if we want more granular. We can leverage stereotypes: e.g. group all controllers into a “Controller layer” box. In PlantUML, we can use the rectangle or component notation. Show arrows: controllers -> services (if a controller class calls a service class according to our call graph), services -> repositories, repositories -> database. Also services -> external REST/SOAP if call graph shows calls to external endpoints. The data for this largely comes from the call graph and the known stereotypes. Mermaid has a flowchart or graph LR that could be used, or Mermaid’s classDiagram might not be suited for high-level components, so maybe use flowchart LR for Mermaid component diagram.

Generate Use Case Diagram: Identify actors (most likely a generic “User” actor for all user-triggered endpoints, and maybe “External System” actor for incoming calls if any). Each API endpoint from our API inventory becomes a “use case”. In PlantUML, syntax: actor User, and then User -> (UseCaseName) lines to link. For Mermaid, use sequenceDiagram or Mermaid’s use case support (Mermaid doesn’t have a built-in use case diagram syntax; it might need to be approximated with flowchart or class diagrams). Alternatively, we might only fully support PlantUML for use case, and provide a textual summary for Mermaid. But ideally, do both: could represent use cases as classes with <<UseCase>> stereotype in Mermaid class diagram, or skip Mermaid for this type if not easily supported. (We’ll aim to provide both with as close semantics as possible.) Use each endpoint’s name or path as the label for the use case. Connect “User” to each. If there are distinct actors (maybe an admin vs a normal user), distinguishing would require more info than we have, so likely we use one actor.

Generate ERD Diagram: For PlantUML, there is a specific ERD syntax or we can use the class diagram notation but with tables. PlantUML has a database notation or we can just do classes with PK/FK fields listed. Perhaps simplest: treat each Entity as a class and list its fields (especially PKs and maybe noteworthy columns) in the class box. Then for relationships: if A has many Bs, draw a line A ||--o{ B (Crow’s foot notation: || for PK side, o{ for multiple side). PlantUML supports that notation on associations. For Mermaid, there is erDiagram syntax which is explicitly for ER diagrams. We can use that: e.g. EntityA ||--o{ EntityB : "relationshipName" style. Use the data from db_entity and relationships to output these.

Generate Sequence Diagrams: Choose representative entry points for sequences. The most straightforward is to generate a sequence diagram for each API endpoint (each public controller method). That might be a lot, but we could focus on top-level ones or allow user to pick. In any case, for each such entry, perform a DFS on the call graph: e.g. start at ControllerX.someEndpoint(), see it calls ServiceY.doSomething(), then that calls RepositoryZ.find() etc. Build the sequence of calls. Ensure to track and avoid cycles. If the call goes to an external class (not in first-party prefixes), decide how to render: if the external class is one we chose to include (prefix-matched library), then it might be in class metadata and we show it like a normal lifeline (maybe with a different color). If it’s truly out of scope, we show a lifeline named “ExternalSystem” or the class name but without details, and we do not continue exploring calls inside it. (We might have a rule like: only traverse into callee if userCode==true for that class. If userCode is false and not in our special library include list, we treat it as leaf.) As we generate, if any method appears that is already in the call stack, insert a note “[cyclic call to X]” and stop that branch. Represent asynchronous or constructor calls appropriately if any (probably not needed; we can treat all calls as synchronous arrows). Use PlantUML’s -> arrows with activation boxes by default. For Mermaid, use sequenceDiagram syntax similarly (participant ClassA, etc., then ClassA->>ClassB: call). Mark external calls maybe with a different arrow style or note. DAO invocations should emit a trailing arrow to the shared “Database” participant that lists the repository methods executed (e.g., “findByStatus(), save()”). If the analyzer cannot prove a method edge (reflection, proxies, etc.), the branch simply terminates instead of inventing placeholder “call” labels; accuracy beats coverage here.

Include Library Code in Diagrams: As part of this iteration, implement the logic to use the parsed dependency classes (those with configured prefixes from earlier) in diagrams. For example, if ServiceA calls com.mycompany.lib.UtilClass.helper(), and UtilClass was identified as a library class to include, then show UtilClass in the class and sequence diagrams with its name, and include its subsequent calls as well. This means our call graph should include edges into those classes. We might have built those edges if we scanned bytecode. If not done yet, implement a lightweight bytecode analysis for those libs specifically for call relations (or require that source for those libs be available – maybe an optional step to fetch sources JAR for those dependencies to parse). In any case, ensure sequence generation doesn’t treat those calls as terminal. Essentially, this extends our userCode boundary to include certain libraries.

Save all generated diagram sources (PlantUML text and Mermaid text) into the diagram table (with a reference to project and diagram type). If possible, also invoke PlantUML to render SVGs for at least the static diagrams (class, component, ERD, use case, maybe not sequence if it’s very large). Save the SVG to disk or in DB (we have svg_path). Link these in the snapshot or provide on request via the /diagram/{id}/svg endpoint.

Frontend Tasks: Enable the Diagrams UI that was set up in earlier iterations:

The Diagrams page will have a tab for each diagram type: Class, Component, Use Case, ERD, Sequence. When the user clicks a tab, the app should fetch /project/{id}/diagrams (or it could have been included in overview snapshot) and filter to that type, then display the diagram. If an SVG is available, display it directly (e.g. embed in an <img> tag or an object tag with pan/zoom capability). If not (e.g. for Mermaid), consider using a Mermaid renderer client-side, or simply show the source with an option to copy it. Provide buttons as specified: “View PlantUML Source” opens a modal or expandable panel showing the PlantUML text for the current diagram; “View Mermaid Source” similarly; “Download SVG” triggers downloading the rendered image. If filtering is needed (for very complex diagrams), provide a simple text filter input – for instance, typing a package name filters the class diagram to only show classes in that package (this could be implemented by regenerating a trimmed diagram on the fly or, more simply, by hiding elements via JavaScript if possible). Full dynamic filtering might be complex; a simpler approach is to just allow hiding external classes in sequence diagrams via the toggle.

The Call Flow page (if separate from Diagrams) might essentially duplicate the Sequence diagram view but with a specific focus. We can have the Call Flow page show the primary sequence diagram (maybe for the main use case of the system if one can be identified) and also list any external calls. However, if we already have Sequence diagram in Diagrams tab, the Call Flow page could be somewhat redundant. Another angle is the Call Flow page lists all the entry-point methods (like all controller operations) and lets the user select one to view its sequence diagram. That might be more user-friendly than dumping dozens of sequence diagrams at once. So, the Call Flow page could present a list (or tree) of API endpoints and when you click one, it loads the corresponding sequence diagram SVG. This might be easier for large systems. Provide the toggle for showing external calls here as well (which could re-render or fetch an alternate version of the diagram where external lifelines are omitted). If our backend doesn’t pre-generate both versions, the toggle might simply hide those lifelines via some clever SVG manipulation or we might pre-generate two sequence diagrams (one with, one without externals). For now, we assume we can filter via code.

Expected Outcome: By the end of Iteration 6, CodeDocGen will automatically produce visual diagrams that greatly aid in understanding the system. The user can navigate to Diagrams, see a class diagram of the whole project (or filtered to certain parts), see a component diagram that shows how the pieces of the system interact, use case diagrams summarizing functionalities, an ERD showing the data model, and sequence diagrams for the flows through the code. For example, for a given REST endpoint, a sequence diagram might show: Controller -> Service -> Repository -> Database, plus maybe calls to external services, with loops or alternative paths if present. The diagrams are stored and can be downloaded or copy-pasted elsewhere. Crucially, even if the code had circular call paths, the sequence diagrams will not get stuck; they will show a “[cycle]” note and stop going deeper, as verified by tests with artificial recursive calls. The inclusion of certain library classes in diagrams (per config) means if a project heavily uses an internal framework, those classes can appear in diagrams rather than as opaque “External” nodes. This iteration delivers one of the most requested features: automated architecture diagrams, saving engineers countless hours of manual diagramming.

Iteration 7 – Final Touches & Export (🚧 Planned)

Goal: Deliver the final polish: the full Confluence-ready HTML export, any remaining metadata integration (like finalizing Gherkin extraction), and ensure all UI pages and download features are wired up. Basically, make CodeDocGen “production-ready” for v1.

Backend Tasks: Implement the ExportService to generate the consolidated HTML document. This involves pulling together data from all the previous steps:

Gather the latest snapshot (ParsedDataResponse). The service can either build the HTML directly from the database data (joining multiple tables) or from the JSON snapshot in project_snapshot. Likely easier to use the JSON for simplicity. Use an HTML template or string builder to create the document with all required sections (as outlined in PRD and summarized above). For each diagram, embed its PlantUML or Mermaid source as a code block (with appropriate labels). Format tables for APIs, database analysis, etc., using simple HTML table tags. Include styling if needed (maybe minimal, since when pasting to Confluence, Confluence will apply its own style). Ensure special characters are escaped properly in HTML.

Provide the GET /project/{id}/export/confluence.html endpoint to retrieve this HTML. Also finalize the GET /project/{id}/export/snapshot if not already (just return the JSON blob). Ensure the /metadata endpoint returns all captured metadata text (OpenAPI YAML, WSDL summaries, etc.) – if not done, implement that: likely by reading from MetadataDump portion of snapshot and returning it as JSON or a structured format. Possibly, /metadata could return a JSON with keys like openApiSpecs (array of spec content or file references), wsdls (some summary), configs (the application.yml content), etc.

Integrate any remaining features: e.g., if Gherkin scanning was deferred, implement it now (though we planned it in iteration 2 and just not fully utilized until export). Make sure all data classes include everything needed for export (like if class annotations or interfaces weren’t included in snapshot but we want them in documentation, consider adding them now, or at least ensure we can retrieve from DB if needed). Minor enhancements: e.g., if the codebase’s Maven packaging is known (maybe note if it’s a war vs jar, though not critical), or if any dependency analysis was wanted (maybe list top 5 libraries used?). These are stretch, only if time permits.

Do a thorough run-through to catch any performance issues (e.g., very large codebases might produce extremely large JSON or diagrams – consider adding config to skip certain analyses if needed, or warn the user). Also, double-check that the cycle detection works in all parts (especially sequence generation, as large graphs can be heavy for PlantUML; perhaps we limit depth or nodes if things get too big).

Frontend Tasks:

Implement the Export page in the UI. This page should have a button “Download Technical Doc (HTML)” that calls the /export/confluence.html endpoint (and probably triggers a browser download of the HTML file). It might also show a preview of that HTML content directly in the app (for example, render the returned HTML in an iframe or a div) so the user can scroll and see what it looks like (this was suggested in PRD). However, since the HTML contains code blocks with PlantUML/Mermaid text, the preview might not render diagrams visually, but at least the content can be reviewed. If rendering within React is complex, we could skip live preview and just let the user download and open it.

Also on Export page, provide a “Download ParsedDataResponse.json” button that simply fetches the /export/snapshot JSON and triggers a download (or opens a modal to copy-paste).

If not already accessible, ensure the Metadata page is implemented: this page will show raw content captured, such as the OpenAPI spec text (maybe as a text area or code block for the user to read/copy), WSDL summary text (if any), and perhaps the application config. It can also list the project’s Maven GAV and maybe a list of key dependencies (though we haven’t explicitly parsed all dependencies, maybe skip detailed dependencies list). The idea is to present any supporting metadata that didn’t fit neatly into previous categories. For example, “OpenAPI spec content for external reference” could be shown here with a copy button. Also mention that the snapshot JSON can be fed to tools like GitLab Duo for further analysis (maybe a note at top).

Final UI polish: Go through each page and ensure consistency – e.g., all pages should have titles or clear headings, make sure dark mode looks okay (Material UI should handle mostly). Add any helpful hints or tooltips (for instance, on the Logger page, tooltip explaining what PII/PCI means). Ensure error handling is in place: if an endpoint returns an error (like analysis fails or data missing), show a user-friendly message. Possibly implement a simple landing page where user can input a new repo URL (the form from iteration 1) and then on success automatically navigate to the project dashboard. (Currently, after analysis, we show overview – but what if multiple projects? Maybe the UI can have a project selection list if multiple projects in DB. That could be an enhancement: a front page listing all analyzed projects (from GET /project). If time allows, implement that: a list of projects with their names and last analyzed time, and ability to switch between them in the UI.)

Expected Outcome: Iteration 7 wraps up the project. The HTML export can be obtained and pasted into Confluence, showing all information in one document (with diagrams as source code blocks). The JSON export can be fed to an AI assistant; for example, a user could ask “Which endpoints in this project log credit card numbers?” and the assistant could answer based on the JSON (this is outside CodeDocGen, but we enable it by providing structured data). All UI sections are implemented, meaning the product now has a complete interface covering everything from overview to metadata. The user experience is smooth: one can analyze a repo with one click and then browse all this rich information.

### Validation Repositories & Test Playbook

Starting with Iteration 5 we run every milestone against the reference repositories documented in `docs/test-repositories.md`. Each repo (Petclinic, ServiceMix, WebGoat) stresses a different slice of the feature set:

* **Petclinic** – fast sanity checks for repo ingestion, API catalog, DB analysis, and the new logger/PCI tabs.
* **ServiceMix** – multi-module + SOAP + servlet discovery, ensures scanners/tabs scale.
* **WebGoat** – intentionally noisy security findings to validate PCI/PII rule tuning and exports.

The doc also includes the validation checklist (snapshot rows, CSV/PDF downloads, UI filter sanity). Capture timing/results for each run so regressions can be spotted before promoting a build.

## Iteration 8 – UX polish & progressive feedback *(Status: ✅ Completed)*

### Goal

Close the UX gaps discovered during internal dogfooding: hidden Diagrams tab on smaller screens, analyzer sidebar clutter after a run completes, lack of visible loading states, and empty-state messages that simply say “Not found” without suggesting fixes.

### Backend

* No new API surface. The frontend orchestrates the existing `/analyze` + follow-up fetches but now maps each call to a deterministic step list (Analyze → Overview → API → Database → Logger → PCI/PII → Diagrams). When an upstream request fails, remaining steps are marked `Skipped` so users understand why the pipeline stopped.

### UI

* Replace the old horizontal tab strip with a sticky, wrapping pill row plus a mobile `<select>` fallback so every panel (especially **Diagrams**) stays discoverable on narrow viewports.
* Add a collapsible analyzer card: after `status === ANALYZED_METADATA`, the form tucks away into a “Latest analysis” summary (project name, repo URL, project ID, analyzed timestamp, quick actions). Users can hide/show it manually, and the card auto-expands whenever an error occurs to keep inputs accessible.
* Introduce an analysis timeline widget (aria-live friendly) that mirrors each backend step with `Queued / In progress… / Done / Skipped` states instead of a generic spinner/dot.
* Refresh empty states—OpenAPI panes now explain how to add Swagger files, API Specs hint at controller annotations, etc.—so each panel offers actionable next steps when data is missing.

### Persistence

* None. All work happens in the React layer.

### Done Criteria

* Tabs remain reachable at <720 px via the dropdown, and the sticky rail keeps navigation visible while scrolling long tables.
* Analyzer card collapses post-analysis, can be redisplayed with a button, and reopens automatically on failure.
* Timeline reflects real-time status of every fetch in the `/analyze` workflow and shows `Skipped` states when runs abort midstream.
* OpenAPI empty states in Overview and API Specs encourage users to add Swagger/OpenAPI files instead of stopping at “No definitions detected.”

At this point, CodeDocGen v1 meets the PRD’s vision functionally, but running on Render highlighted a reliability gap: the on-disk H2 database lives on ephemeral storage, so every restart wipes the user’s analysis history. To keep the product usable in hosted scenarios we are extending the plan with a focused persistence-hardening iteration.

## Iteration 9 – PostgreSQL persistence hardening *(Status: 🚧 Planned)*

### Goal

Replace the brittle, file-based H2 datastore with a managed PostgreSQL instance so analysis history survives restarts in Render and other hosts, while keeping local setup one-command simple.

### Backend

* Swap the runtime dependency from H2 to the PostgreSQL JDBC driver and point Spring Data JPA at the external database via `SPRING_DATASOURCE_*` env vars.
* Ensure Hibernate dialect/timezone defaults are Postgres-friendly and that connection pool sizing is overridable per environment.
* Add a readiness check (e.g., Spring Boot actuator health or startup validation) that fails fast when the DB is unreachable so Render restarts don’t go into partial states.
* Update application defaults so diagram storage paths, snapshot writes, etc., continue to work with the new datasource.

### Persistence / DevOps

* Introduce a `docker-compose.postgres.yml` (or similar) that boots a local Postgres 15 instance with persistent volumes, matching production schemas and credentials via `.env`.
* Document the exact env vars Render must supply (`SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`, `SPRING_JPA_HIBERNATE_DDL_AUTO` override if needed).
* Provide a lightweight migration/story for existing H2 data (explicitly call out that historic local runs will be dropped, but offer an export/import script if feasible).

### UI / DX

* Update README + onboarding docs so engineers know how to spin up the dockerized DB locally before launching the backend.
* Add a troubleshooting section that covers common Postgres startup issues (port busy, auth failure) and how to reset the docker volume.

### Done Criteria

* Backend boots locally against the dockerized Postgres container (schema auto-creates, `/analyze` runs, restart retains projects).
* Render deployment variables point at the managed Postgres service and retain data across restarts.
* README / PRD / iteration plan all reflect Postgres as the canonical datastore, and there is a documented local workflow that new contributors can follow without manual DB installs.
