package com.codevision.codevisionbackend.analysis.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.codevision.codevisionbackend.analysis.CompiledAnalysisResult;
import com.codevision.codevisionbackend.analysis.CompiledAnalysisService;
import com.codevision.codevisionbackend.analysis.ExportedFile;
import com.codevision.codevisionbackend.analysis.GraphModel;
import com.codevision.codevisionbackend.analysis.persistence.AnalysisEntityRecord;
import com.codevision.codevisionbackend.analysis.persistence.AnalysisEntityRepository;
import com.codevision.codevisionbackend.analysis.persistence.CompiledAnalysisRun;
import com.codevision.codevisionbackend.analysis.persistence.CompiledAnalysisRunStatus;
import com.codevision.codevisionbackend.analysis.persistence.CompiledEndpointRecord;
import com.codevision.codevisionbackend.analysis.persistence.CompiledEndpointRepository;
import com.codevision.codevisionbackend.analysis.persistence.SequenceRecord;
import com.codevision.codevisionbackend.analysis.persistence.SequenceRecordRepository;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class AnalysisControllerTest {

    @Mock
    private CompiledAnalysisService compiledAnalysisService;

    @Mock
    private AnalysisEntityRepository analysisEntityRepository;

    @Mock
    private SequenceRecordRepository sequenceRecordRepository;

    @Mock
    private CompiledEndpointRepository compiledEndpointRepository;

    private MockMvc mockMvc;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        AnalysisController controller = new AnalysisController(
                compiledAnalysisService,
                analysisEntityRepository,
                sequenceRecordRepository,
                compiledEndpointRepository);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void runCompiledAnalysisReturnsResponse() throws Exception {
        CompiledAnalysisRun run = completedRun();
        CompiledAnalysisResult result = new CompiledAnalysisResult(run, null, GraphModel.empty());
        when(compiledAnalysisService.analyze(any())).thenReturn(result);
        Path export = Files.writeString(tempDir.resolve("analysis.json"), "{}");
        when(compiledAnalysisService.listExports(run.getId()))
                .thenReturn(List.of(new ExportedFile("analysis.json", 2, export)));

        mockMvc.perform(post("/api/analyze")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"repoPath": "%s"}
                                """.formatted(tempDir)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.analysisId").value(run.getId().toString()))
                .andExpect(jsonPath("$.exports[0].name").value("analysis.json"));
    }

    @Test
    void getCompiledAnalysisReturnsNotFoundWhenMissing() throws Exception {
        when(compiledAnalysisService.findLatestByProject(1L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/project/1/compiled-analysis"))
                .andExpect(status().isNotFound());
    }

    @Test
    void listEntitiesSupportsPackageFilter() throws Exception {
        AnalysisEntityRecord record = new AnalysisEntityRecord();
        record.setClassName("com.example.Entity");
        record.setPackageName("com.example");
        record.setOrigin(GraphModel.Origin.BYTECODE);
        when(analysisEntityRepository.findByPackageNameStartingWithIgnoreCase(eq("com"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(record)));

        mockMvc.perform(get("/api/entities").param("packageFilter", "com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].className").value("com.example.Entity"));
    }

    @Test
    void downloadExportStreamsFile() throws Exception {
        CompiledAnalysisRun run = completedRun();
        Path export = Files.writeString(tempDir.resolve("export.csv"), "id,name");
        when(compiledAnalysisService.listExports(run.getId()))
                .thenReturn(List.of(new ExportedFile("export.csv", 7, export)));
        when(compiledAnalysisService.resolveExportFile(run.getId(), "export.csv")).thenReturn(export);

        mockMvc.perform(get("/api/analyze/{id}/exports", run.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("export.csv"));

        mockMvc.perform(get("/api/analyze/{id}/exports/{file}", run.getId(), "export.csv"))
                .andExpect(status().isOk());
    }

    @Test
    void listSequencesReturnsPagedData() throws Exception {
        SequenceRecord sequence = new SequenceRecord();
        sequence.setGeneratorName("seq");
        when(sequenceRecordRepository.findAll(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(sequence)));

        mockMvc.perform(get("/api/sequences"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].generatorName").value("seq"));
    }

    @Test
    void listEndpointsReturnsRecords() throws Exception {
        CompiledEndpointRecord record = new CompiledEndpointRecord();
        record.setPath("/api");
        record.setControllerClass("com.example.Controller");
        record.setType(GraphModel.EndpointType.HTTP);
        when(compiledEndpointRepository.findAll(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(record)));

        mockMvc.perform(get("/api/endpoints"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].path").value("/api"));
    }

    private CompiledAnalysisRun completedRun() {
        CompiledAnalysisRun run = new CompiledAnalysisRun();
        run.setId(UUID.randomUUID());
        run.setRepoPath(tempDir.toString());
        run.setStatus(CompiledAnalysisRunStatus.SUCCEEDED);
        run.setStatusMessage("done");
        run.setStartedAt(Instant.now().minusSeconds(5));
        run.setCompletedAt(Instant.now());
        run.setOutputDirectory(tempDir.toString());
        run.setEntityCount(1L);
        run.setEndpointCount(1L);
        run.setDependencyCount(1L);
        run.setSequenceCount(0L);
        return run;
    }
}
