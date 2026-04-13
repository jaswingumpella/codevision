# Testing Rules

## Test Structure: Given/When/Then with @Nested

ALL test classes MUST use this structure:

```java
class MyServiceTest {

    @Nested
    class Given_EmptyInput {

        @Nested
        class When_ProcessingCalled {

            @Test
            void Then_ReturnsEmptyResult() {
                // arrange (given)
                var input = List.of();

                // act (when)
                var result = service.process(input);

                // assert (then)
                assertThat(result).isEmpty();
            }
        }
    }

    @Nested
    class Given_ValidInput {

        @Nested
        class When_ProcessingCalled {

            @Test
            void Then_ReturnsExpectedOutput() { ... }

            @Test
            void Then_SideEffectsApplied() { ... }
        }
    }
}
```

## Coverage Requirements

- **New code**: >= 90% line and branch coverage (JaCoCo enforced)
- **Graph algorithms**: 100% branch coverage (critical path)
- **REST controllers**: Integration tests for every endpoint

## Test Categories

| Category | Naming | Location | Runner |
|----------|--------|----------|--------|
| Unit | `*Test.java` | `src/test/java/` | maven-surefire |
| Integration | `*IT.java` | `src/test/java/` | maven-failsafe |
| Property-based | `*PropertyTest.java` | `src/test/java/` | maven-surefire + jqwik |

## Fixture Rules

- Fixtures in `src/test/resources/fixtures/`
- Small, purpose-built (not copied from production)
- Checked into git (not generated at test time)
- Named descriptively: `simple-spring-app/`, `cyclic-deps/`, `multi-module/`

## Prohibitions

- NO @Disabled tests (fix or delete)
- NO mocking internal classes (only external boundaries: HTTP, DB, filesystem)
- NO Thread.sleep in tests (use CountDownLatch, CompletableFuture, Awaitility)
- NO test interdependence (each test must pass in isolation)
- NO random data without seeds (deterministic tests only)
