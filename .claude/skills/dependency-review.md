---
name: dependency-review
description: Review dependency resolver implementations
---

# Dependency Review Skill

When reviewing dependency/ changes:

1. DependencyResolver interface is clean Strategy pattern
2. Registry auto-detects build system from project root files
3. Maven resolver uses `dependency:tree` for FULL transitive resolution (no depth limit)
4. Exclusion patterns correctly match configurable OSS libraries
5. Source download pipeline handles missing sources gracefully
6. Dependency nodes tagged with artifact coordinates (group:name:version)
7. Edges from project code to dependency code correctly established
8. No network calls in unit tests (mock resolver output)
9. Integration test verifies against real Maven project fixture
