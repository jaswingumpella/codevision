# Reference GitHub Repositories for CodeVision Validation

Use the repositories below to exercise CodeVision across different architectural profiles. Each project is open source and large enough to validate the new Logger Insights + PCI/PII scan workflows as well as the previously delivered features (repo ingestion, API inventory, DB analysis, diagram scaffolding).

| Repository | URL | Why it is useful | Primary coverage |
|------------|-----|------------------|------------------|
| Spring Petclinic | https://github.com/spring-projects/spring-petclinic | Canonical Spring MVC + Spring Data JPA demo with moderate size, clean logging, application YAML, REST controllers, and repositories. | End-to-end sanity checks across repo ingestion, class metadata, API catalog, DB analysis, logger extraction, and security scans with manageable runtime. |
| ServiceMix | https://github.com/apache/servicemix | Large, multi-module Apache Camel/OSGi/SOAP platform containing controllers, legacy servlets, XML configs, and extensive logging. | Stress testing for multi-module detection, SOAP/WSDL parsing, legacy endpoint discovery, logger volume, PCI/PII scanner performance, and snapshot size. |
| WebGoat | https://github.com/OWASP/WebGoat | Intentionally vulnerable Spring Boot application full of sensitive strings, security exercises, and verbose logging. | Validating PCI/PII findings, ensuring risky log statements are flagged, and confirming the UI/export surfaces highlight security hotspots. |

## How to Run the Tests

1. **Clone the target repo** (example with Spring Petclinic):
   ```bash
   git clone https://github.com/spring-projects/spring-petclinic.git
   ```
2. **Point CodeVision at the clone**:
   ```bash
   curl -X POST http://localhost:8080/analyze \
     -H 'Content-Type: application/json' \
     -H 'X-API-KEY: <your-key>' \
     -d '{"repoUrl": "file:///absolute/path/to/spring-petclinic"}'
   ```
3. **Review the dashboard** after analysis completes:
   - Verify Overview, API Specs, Database, Logger Insights, and PCI/PII tabs populate.
   - Download CSV/PDF exports for logs and PCI/PII findings.
   - Fetch `/project/{id}/overview`, `/logger-insights`, `/pii-pci`, and `/export/*` endpoints to confirm payloads.
4. **Repeat for ServiceMix and WebGoat** to exercise larger and security-heavy cases. Capture timing, snapshot size, and any scanner findings for regression tracking.

## Validation Checklist

- [ ] Snapshot JSON contains populated `loggerInsights` and `piiPciScan` arrays.
- [ ] `log_statement` and `pii_pci_finding` tables are replaced cleanly on re-analysis.
- [ ] Logger Insights tab filters (class, level, PII/PCI toggles) behave correctly even with thousands of rows (ServiceMix).
- [ ] PCI/PII tab hides ignored findings when the toggle is enabled.
- [ ] CSV/PDF exports match the table filters applied in the UI.
- [ ] Export endpoints respond with HTTP 200 and properly set `Content-Disposition`.
- [ ] Diagrams tab lists Class/Component/Use Case/ERD/DB Schema/Sequence diagrams for the analyzed project. Class diagrams should include arrows (either from the call graph or via the fallback Controller→Service→Repository heuristic) and Component cards should show representative class names.
- [ ] Sequence tab shows one entry per endpoint; toggling “Show codeviz2 externals” swaps between the internal-only and full flows, the HTTP method + path label updates accordingly, and every arrow label reflects the invoked method name (self-calls should render as loops and DAO arrows should list the repository methods).
- [ ] Responsive layout: shrink the browser to ~1024px and confirm the analyzer form stacks above the diagrams tab without horizontal scrolling. SVG panes should scroll inside their cards instead of pushing the page.
- [ ] Sticky tab rail stays visible while scrolling and the mobile `<select>` still exposes every tab when the viewport is <720 px (verify the Diagrams tab is never orphaned off-screen).
- [ ] Analyzer card collapses into the “Latest analysis” summary after a successful run, exposes project metadata, and the Hide/Show + Edit inputs actions work; failed runs automatically reopen the form.
- [ ] The new analysis timeline reports each step (`Analyze → Overview → API → Database → Logger → PCI/PII → Diagrams`) with sensible statuses; abort a run mid-way to ensure remaining steps flip to `Skipped`.
- [ ] OpenAPI sections in Overview and API Specs display the new actionable guidance (“Add Swagger annotations or include openapi.yaml/swagger.json…”) whenever no specs are found.
- [ ] Metadata tab exposes captured OpenAPI/WSDL/XSD content and displays the snapshot download link for AI workflows.
- [ ] Export tab downloads the HTML + JSON bundles and renders the inline HTML preview without console errors (Petclinic is a quick sanity check).

Record findings (especially false positives or performance anomalies) so the rule set in `security.scan` can be tuned per environment.
