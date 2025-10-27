package com.codevision.codevisionbackend.analyze.scanner;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class DaoAnalysisServiceImplTest {

    private final DaoAnalysisServiceImpl service = new DaoAnalysisServiceImpl();

    @Test
    void analyzesSpringDataRepositoryInterfaces(@TempDir Path tempDir) throws Exception {
        Path sourceRoot = tempDir.resolve("src/main/java/com/example");
        Files.createDirectories(sourceRoot);

        Files.writeString(
                sourceRoot.resolve("CustomerRepository.java"),
                """
                        package com.example;

                        import java.util.List;
                        import org.springframework.data.jpa.repository.JpaRepository;
                        import org.springframework.data.jpa.repository.Query;

                        public interface CustomerRepository extends JpaRepository<Customer, Long> {

                            List<Customer> findByStatus(String status);

                            @Query("select c from Customer c where c.region = :region")
                            List<Customer> findByRegion(String region);
                        }
                        """);

        DbEntityRecord entityRecord = new DbEntityRecord(
                "Customer",
                "com.example.Customer",
                "customers",
                List.of("id"),
                List.of(),
                List.of());

        DbAnalysisResult result =
                service.analyze(tempDir, List.of(tempDir), List.of(entityRecord));

        assertThat(result.entities()).containsExactly(entityRecord);
        assertThat(result.classesByEntity()).containsEntry("Customer", List.of("com.example.CustomerRepository"));

        List<DaoOperationRecord> operations =
                result.operationsByClass().get("com.example.CustomerRepository");
        assertThat(operations).hasSize(2);

        DaoOperationRecord derived = operations.stream()
                .filter(op -> op.methodName().equals("findByStatus"))
                .findFirst()
                .orElseThrow();
        assertThat(derived.operationType()).isEqualTo("SELECT");
        assertThat(derived.target()).isEqualTo("Customer [customers]");
        assertThat(derived.querySnippet()).isNull();

        DaoOperationRecord annotated = operations.stream()
                .filter(op -> op.methodName().equals("findByRegion"))
                .findFirst()
                .orElseThrow();
        assertThat(annotated.operationType()).isEqualTo("SELECT");
        assertThat(annotated.querySnippet()).isEqualTo("select c from Customer c where c.region = :region");
    }
}
