package com.codevision.codevisionbackend.usecase;

import com.codevision.codevisionbackend.graph.KnowledgeGraph;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller exposing all 12 actionable use cases.
 * Accepts a KnowledgeGraph (to be resolved from project ID via persistence in Sprint 9).
 */
@RestController
@RequestMapping("/api/v1/usecases")
public class UseCaseController {

    private final DeadCodeReportService deadCodeService;
    private final ImpactAnalysisService impactService;
    private final ArchitectureHealthService healthService;
    private final ApiSurfaceMapService apiSurfaceService;
    private final CycleBreakerService cycleBreakerService;
    private final TestGapAnalysisService testGapService;
    private final OnboardingGuideService onboardingService;
    private final DependencyAuditService dependencyAuditService;
    private final DbSchemaIntelligenceService dbSchemaService;
    private final DuplicationFinderService duplicationService;
    private final MigrationPlannerService migrationService;
    private final SecurityScanService securityService;

    public UseCaseController(
            DeadCodeReportService deadCodeService,
            ImpactAnalysisService impactService,
            ArchitectureHealthService healthService,
            ApiSurfaceMapService apiSurfaceService,
            CycleBreakerService cycleBreakerService,
            TestGapAnalysisService testGapService,
            OnboardingGuideService onboardingService,
            DependencyAuditService dependencyAuditService,
            DbSchemaIntelligenceService dbSchemaService,
            DuplicationFinderService duplicationService,
            MigrationPlannerService migrationService,
            SecurityScanService securityService) {
        this.deadCodeService = deadCodeService;
        this.impactService = impactService;
        this.healthService = healthService;
        this.apiSurfaceService = apiSurfaceService;
        this.cycleBreakerService = cycleBreakerService;
        this.testGapService = testGapService;
        this.onboardingService = onboardingService;
        this.dependencyAuditService = dependencyAuditService;
        this.dbSchemaService = dbSchemaService;
        this.duplicationService = duplicationService;
        this.migrationService = migrationService;
        this.securityService = securityService;
    }

    @PostMapping("/dead-code")
    public ResponseEntity<DeadCodeReportService.DeadCodeReport> deadCode(@RequestBody KnowledgeGraph graph) {
        return ResponseEntity.ok(deadCodeService.generate(graph));
    }

    @PostMapping("/impact/{nodeId}")
    public ResponseEntity<ImpactAnalysisService.ImpactResult> impact(
            @RequestBody KnowledgeGraph graph, @PathVariable String nodeId) {
        return ResponseEntity.ok(impactService.analyzeImpact(graph, nodeId));
    }

    @PostMapping("/health")
    public ResponseEntity<ArchitectureHealthService.HealthReport> health(@RequestBody KnowledgeGraph graph) {
        return ResponseEntity.ok(healthService.score(graph));
    }

    @PostMapping("/api-surface")
    public ResponseEntity<ApiSurfaceMapService.ApiSurfaceResult> apiSurface(@RequestBody KnowledgeGraph graph) {
        return ResponseEntity.ok(apiSurfaceService.map(graph));
    }

    @PostMapping("/cycles")
    public ResponseEntity<CycleBreakerService.CycleReport> cycles(@RequestBody KnowledgeGraph graph) {
        return ResponseEntity.ok(cycleBreakerService.analyze(graph));
    }

    @PostMapping("/test-gaps")
    public ResponseEntity<TestGapAnalysisService.TestGapReport> testGaps(@RequestBody KnowledgeGraph graph) {
        return ResponseEntity.ok(testGapService.analyze(graph));
    }

    @PostMapping("/onboarding")
    public ResponseEntity<OnboardingGuideService.OnboardingGuide> onboarding(@RequestBody KnowledgeGraph graph) {
        return ResponseEntity.ok(onboardingService.generate(graph));
    }

    @PostMapping("/dependency-audit")
    public ResponseEntity<DependencyAuditService.AuditReport> dependencyAudit(@RequestBody KnowledgeGraph graph) {
        return ResponseEntity.ok(dependencyAuditService.audit(graph));
    }

    @PostMapping("/db-schema")
    public ResponseEntity<DbSchemaIntelligenceService.SchemaReport> dbSchema(@RequestBody KnowledgeGraph graph) {
        return ResponseEntity.ok(dbSchemaService.analyze(graph));
    }

    @PostMapping("/duplications")
    public ResponseEntity<DuplicationFinderService.DuplicationReport> duplications(@RequestBody KnowledgeGraph graph) {
        return ResponseEntity.ok(duplicationService.find(graph));
    }

    @PostMapping("/migration/{artifactNodeId}")
    public ResponseEntity<MigrationPlannerService.MigrationPlan> migration(
            @RequestBody KnowledgeGraph graph, @PathVariable String artifactNodeId) {
        return ResponseEntity.ok(migrationService.plan(graph, artifactNodeId));
    }

    @PostMapping("/security")
    public ResponseEntity<SecurityScanService.SecurityReport> security(@RequestBody KnowledgeGraph graph) {
        return ResponseEntity.ok(securityService.scan(graph));
    }
}
