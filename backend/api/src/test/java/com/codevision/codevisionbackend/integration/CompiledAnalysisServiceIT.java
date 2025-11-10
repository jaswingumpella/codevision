package com.codevision.codevisionbackend.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.codevision.codevisionbackend.analysis.CompiledAnalysisResult;
import com.codevision.codevisionbackend.analysis.CompiledAnalysisService;
import com.codevision.codevisionbackend.analysis.CompiledAnalysisService.CompiledAnalysisParameters;
import com.codevision.codevisionbackend.analysis.ExportedFile;
import com.codevision.codevisionbackend.analysis.persistence.AnalysisEntityRepository;
import com.codevision.codevisionbackend.analysis.persistence.CompiledAnalysisRunRepository;
import com.codevision.codevisionbackend.analysis.persistence.CompiledAnalysisRunStatus;
import com.codevision.codevisionbackend.analysis.persistence.CompiledEndpointRecord;
import com.codevision.codevisionbackend.analysis.persistence.CompiledEndpointRepository;
import com.codevision.codevisionbackend.analysis.persistence.SequenceRecord;
import com.codevision.codevisionbackend.analysis.persistence.SequenceRecordRepository;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class CompiledAnalysisServiceIT extends AbstractPostgresIntegrationTest {

    @Autowired
    private CompiledAnalysisService compiledAnalysisService;

    @Autowired
    private CompiledAnalysisRunRepository compiledAnalysisRunRepository;

    @Autowired
    private AnalysisEntityRepository analysisEntityRepository;

    @Autowired
    private SequenceRecordRepository sequenceRecordRepository;

    @Autowired
    private CompiledEndpointRepository compiledEndpointRepository;

    @Test
    void bytecodeOnlyRunPersistsGraphAndExports(@TempDir Path workspace) throws Exception {
        Path fixtureRoot = copyCompiledFixture(workspace);

        CompiledAnalysisResult result = compiledAnalysisService.analyze(
                new CompiledAnalysisParameters(fixtureRoot, List.of("com.codevision.fixtures"), false, null));

        assertThat(result.run().getStatus()).isEqualTo(CompiledAnalysisRunStatus.SUCCEEDED);
        assertThat(compiledAnalysisRunRepository.findById(result.run().getId())).isPresent();

        assertThat(analysisEntityRepository.findByClassName("com.codevision.fixtures.domain.FixtureEntity"))
                .isNotNull();
        assertThat(sequenceRecordRepository.findAll())
                .extracting(SequenceRecord::getGeneratorName)
                .contains("fixture_seq");
        assertThat(compiledEndpointRepository.findAll())
                .extracting(CompiledEndpointRecord::getType)
                .extracting(Enum::name)
                .contains("HTTP", "KAFKA", "SCHEDULED");

        List<ExportedFile> exports = compiledAnalysisService.listExports(result.run().getId());
        assertThat(exports)
                .extracting(ExportedFile::name)
                .contains("analysis.json", "entities.csv", "sequences.csv");
    }
}
