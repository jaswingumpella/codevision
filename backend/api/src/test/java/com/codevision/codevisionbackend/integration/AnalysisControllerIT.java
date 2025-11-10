package com.codevision.codevisionbackend.integration;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.codevision.codevisionbackend.analysis.CompiledAnalysisResult;
import com.codevision.codevisionbackend.analysis.CompiledAnalysisService;
import com.codevision.codevisionbackend.analysis.CompiledAnalysisService.CompiledAnalysisParameters;
import com.codevision.codevisionbackend.project.Project;
import com.codevision.codevisionbackend.project.ProjectRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class AnalysisControllerIT extends AbstractPostgresIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CompiledAnalysisService compiledAnalysisService;

    @Autowired
    private ProjectRepository projectRepository;

    @Test
    void compiledAnalysisEndpointsExposeRunsAndPagination(@TempDir Path workspace) throws Exception {
        Path fixtureRoot = copyCompiledFixture(workspace);
        Project project = projectRepository.saveAndFlush(
                new Project("https://git.local/fixtures.git", "fixture-app", "main", OffsetDateTime.now()));

        CompiledAnalysisResult seededRun = compiledAnalysisService.analyze(
                new CompiledAnalysisParameters(fixtureRoot, List.of("com.codevision.fixtures"), false, project.getId()));

        ObjectNode request = objectMapper.createObjectNode();
        request.put("repoPath", fixtureRoot.toString());
        ArrayNode packages = request.putArray("acceptPackages");
        packages.add("com.codevision.fixtures");
        request.put("includeDependencies", false);
        String requestJson = objectMapper.writeValueAsString(request);

        mockMvc.perform(post("/api/analyze")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isUnauthorized());

        MvcResult postResult = mockMvc.perform(post("/api/analyze")
                        .header("X-API-KEY", apiKey())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.analysisId").isNotEmpty())
                .andExpect(jsonPath("$.exports.length()", greaterThan(0)))
                .andReturn();

        JsonNode postJson = objectMapper.readTree(postResult.getResponse().getContentAsByteArray());
        UUID analysisId = UUID.fromString(postJson.get("analysisId").asText());

        MvcResult exportsResult = mockMvc.perform(get("/api/analyze/{id}/exports", analysisId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").isNotEmpty())
                .andReturn();

        JsonNode exports = objectMapper.readTree(exportsResult.getResponse().getContentAsByteArray());
        String firstFile = exports.get(0).get("name").asText();

        mockMvc.perform(get("/api/analyze/{id}/exports/{file}", analysisId, firstFile))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", containsString(firstFile)))
                .andExpect(content().string(containsString("com.codevision.fixtures")));

        mockMvc.perform(get("/api/project/{projectId}/compiled-analysis", project.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.analysisId").value(seededRun.run().getId().toString()))
                .andExpect(jsonPath("$.entityCount", greaterThan(0)));

        mockMvc.perform(get("/api/entities").param("page", "0").param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements", greaterThan(0)))
                .andExpect(jsonPath("$.items.length()", greaterThanOrEqualTo(5)))
                .andExpect(jsonPath("$.items[*].className", hasItem("com.codevision.fixtures.domain.FixtureEntity")));

        mockMvc.perform(get("/api/sequences"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].generatorName").value("fixture_seq"));

        mockMvc.perform(get("/api/endpoints").param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[*].type", hasItems("HTTP", "KAFKA", "SCHEDULED")))
                .andExpect(jsonPath("$.items[*].path", hasItem("/fixtures")));
    }
}
