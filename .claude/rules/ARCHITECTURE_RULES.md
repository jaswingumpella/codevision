# Architecture Rules

## Package Boundaries

| Package | Responsibility | Can Import From |
|---------|---------------|-----------------|
| `graph/` | KnowledgeGraph model, algorithms, export | Only `graph/` internal |
| `analysis/` | Bytecode/source scanners, GraphModel | `graph/` |
| `dependency/` | Dependency resolution per ecosystem | `graph/` |
| `multilang/` | tree-sitter, language analyzers | `graph/`, `analysis/` |
| `callgraph/` | Call graph builder, CHA, dispatch | `graph/`, `analysis/` |
| `usecase/` | 12 use case services | `graph/`, `graph/algorithm/` |
| `analyze/` | Orchestration, pipeline | All packages |
| `project/` | REST controllers | `analyze/`, `usecase/`, `graph/` |

## Design Patterns (Mandatory)

- **Strategy**: `DependencyResolver` interface with per-ecosystem implementations
- **Visitor**: AST walkers for tree-sitter and JavaParser
- **Builder**: `KnowledgeGraphBuilder` for incremental graph construction
- **Adapter**: `GraphModelAdapter` for legacy GraphModel compatibility
- **Command**: `GraphAlgorithm<R>` interface for pluggable algorithms
- **Template Method**: `LanguageAnalyzer` with language-specific hooks

## Absolute Prohibitions

1. **NO hardcoded depth/size limits**: No `MAX_`, no `.limit(N)` with literal integers, no depth counters
2. **NO depth-based termination**: Use cycle-detection (visited-set) + time-based deadline (Instant)
3. **NO mutable value types**: Use Java records for all data carriers
4. **NO circular package dependencies**: graph/ never imports from usecase/
5. **NO string-typed enums**: Node types and edge types must be enum values, not strings

## Safety Mechanisms (Replace Depth Limits)

- **Cycle detection**: `Set<String> visited` in every DFS/BFS traversal
- **Time-based deadline**: `Instant deadline = Instant.now().plusSeconds(config.maxRuntimeSeconds)`
- **Memory monitoring**: Check `Runtime.getRuntime().freeMemory()` during large traversals

## Configuration

Every tunable value must live in `application.yml` and be injectable via `@ConfigurationProperties`.
Default values must be "unlimited" (-1 or Integer.MAX_VALUE) unless safety requires a limit.
