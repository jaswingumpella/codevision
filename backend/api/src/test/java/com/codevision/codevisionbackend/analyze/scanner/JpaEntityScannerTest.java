package com.codevision.codevisionbackend.analyze.scanner;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class JpaEntityScannerTest {

    @Test
    void detectsEntityMetadataAndRelationships(@TempDir Path tempDir) throws Exception {
        Path sourceRoot = tempDir.resolve("src/main/java/com/example");
        Files.createDirectories(sourceRoot);

        Files.writeString(
                sourceRoot.resolve("Customer.java"),
                """
                        package com.example;

                        import jakarta.persistence.Column;
                        import jakarta.persistence.Entity;
                        import jakarta.persistence.Id;
                        import jakarta.persistence.OneToMany;
                        import jakarta.persistence.Table;
                        import java.util.List;

                        @Entity
                        @Table(name = "customers")
                        public class Customer {

                            @Id
                            private Long id;

                            @Column(name = "customer_name")
                            private String name;

                            @OneToMany
                            private List<Order> orders;
                        }
                        """);

        Files.writeString(
                sourceRoot.resolve("Order.java"),
                """
                        package com.example;

                        import jakarta.persistence.Entity;
                        import jakarta.persistence.Id;
                        import jakarta.persistence.ManyToOne;

                        @Entity
                        public class Order {

                            @Id
                            private Long id;

                            @ManyToOne
                            private Customer customer;
                        }
                        """);

        JpaEntityScanner scanner = new JpaEntityScanner();
        List<DbEntityRecord> records = scanner.scan(tempDir, List.of(tempDir));

        assertThat(records).hasSize(2);

        DbEntityRecord customer = records.stream()
                .filter(record -> record.className().equals("Customer"))
                .findFirst()
                .orElseThrow();

        assertThat(customer.tableName()).isEqualTo("customers");
        assertThat(customer.primaryKeys()).containsExactly("id");
        assertThat(customer.fields()).anySatisfy(field -> {
            assertThat(field.name()).isEqualTo("name");
            assertThat(field.columnName()).isEqualTo("customer_name");
        });
        assertThat(customer.relationships()).anySatisfy(relationship -> {
            assertThat(relationship.fieldName()).isEqualTo("orders");
            assertThat(relationship.targetType()).isEqualTo("Order");
            assertThat(relationship.relationshipType()).isEqualTo("ONE_TO_MANY");
        });

        DbEntityRecord order = records.stream()
                .filter(record -> record.className().equals("Order"))
                .findFirst()
                .orElseThrow();
        assertThat(order.tableName()).isEqualTo("Order");
        assertThat(order.primaryKeys()).containsExactly("id");
        assertThat(order.relationships()).anySatisfy(relationship -> {
            assertThat(relationship.fieldName()).isEqualTo("customer");
            assertThat(relationship.targetType()).isEqualTo("Customer");
            assertThat(relationship.relationshipType()).isEqualTo("MANY_TO_ONE");
        });
    }
}
