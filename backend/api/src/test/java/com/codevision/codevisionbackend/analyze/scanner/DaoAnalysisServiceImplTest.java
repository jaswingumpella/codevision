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

        Files.writeString(
                sourceRoot.resolve("CustomerService.java"),
                """
                        package com.example;

                        public class CustomerService {
                            private final CustomerRepository repository;

                            public CustomerService(CustomerRepository repository) {
                                this.repository = repository;
                            }

                            public void refreshCustomers() {
                                repository.findByStatus("ACTIVE");
                                repository.findByRegion("EU");
                                repository.save(new Customer());
                            }
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
        assertThat(operations).isNotEmpty();

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

        DaoOperationRecord save = operations.stream()
                .filter(op -> op.methodName().equals("save"))
                .findFirst()
                .orElseThrow();
        assertThat(save.operationType()).isEqualTo("INSERT_OR_UPDATE");
    }

    @Test
    void analyzesLegacyDaoMethods(@TempDir Path tempDir) throws Exception {
        Path sourceRoot = tempDir.resolve("src/main/java/com/example");
        Files.createDirectories(sourceRoot);

        Files.writeString(
                sourceRoot.resolve("CustomerDao.java"),
                """
                        package com.example;

                        import org.hibernate.SessionFactory;
                        import java.util.List;

                        public class CustomerDao {
                            private final SessionFactory sessionFactory;

                            public CustomerDao(SessionFactory sessionFactory) {
                                this.sessionFactory = sessionFactory;
                            }

                            public void saveCustomer(Customer customer) {
                                sessionFactory.getCurrentSession().save(customer);
                            }

                            public List<Customer> listCustomers() {
                                return sessionFactory.getCurrentSession()
                                        .createCriteria(Customer.class)
                                        .list();
                            }
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

        List<DaoOperationRecord> operations =
                result.operationsByClass().get("com.example.CustomerDao");
        assertThat(operations).isNotEmpty();

        assertThat(operations.stream()
                .anyMatch(op -> op.methodName().equals("saveCustomer")
                        && op.operationType().equals("INSERT_OR_UPDATE")))
                .isTrue();
        assertThat(operations.stream()
                .anyMatch(op -> op.methodName().equals("listCustomers")
                        && op.operationType().equals("SELECT")))
                .isTrue();
    }
}
