# CodeVision - AST Knowledge Graph Engine

## Overview
CodeVision analyzes codebases via AST parsing (JavaParser + tree-sitter), bytecode analysis (ASM + ClassGraph), and dependency resolution to build comprehensive knowledge graphs. The graph powers 12 actionable use cases through an interactive Sigma.js WebGL dashboard.

## Tech Stack
- Backend: Java 21, Spring Boot 3.2.5, Maven, PostgreSQL, Flyway, H2 (test)
- Frontend: React 18, Vite, Sigma.js v3, Graphology
- Scripts: Python 3.10+
- AST: JavaParser 3.25.10, tree-sitter (JNI), OW2 ASM 9.6, ClassGraph 4.8.172

## Package Structure
- `graph/` - KnowledgeGraph, KgNode, KgEdge, algorithms, export, persistence
- `analysis/` - Bytecode/source scanners, GraphModel (legacy), merging
- `dependency/` - DependencyResolver implementations per ecosystem
- `multilang/` - tree-sitter integration, language analyzers
- `callgraph/` - Unlimited call graph builder, CHA, dispatch resolution
- `usecase/` - 12 actionable use case services
- `analyze/` - Existing analysis orchestration (being extended)
- `project/` - Existing project API controllers

## Design Patterns
- Strategy: DependencyResolver (Maven, Gradle, npm, pip, Go, Cargo, NuGet)
- Visitor: AST walkers for JavaParser and tree-sitter trees
- Builder: KnowledgeGraphBuilder for incremental graph construction
- Adapter: GraphModelAdapter for backward compatibility
- Command: GraphAlgorithm<R> interface for pluggable algorithms
- Template Method: LanguageAnalyzer base class

## Non-Negotiable Rules
1. NO hardcoded depth/size limits - use cycle-detection + time-based deadlines
2. ALL tests use Given/When/Then with @Nested classes
3. 90%+ coverage on new code
4. Java records for all value types
5. Configuration-driven: every tunable in application.yml
6. TDD: write failing test first, then implement

## Build & Test
- Backend unit tests: `mvn -f backend/pom.xml -pl api test`
- Backend full verify: `mvn -f backend/pom.xml -pl api verify`
- Frontend tests: `npm --prefix frontend test`
- QA compliance: `python .claude/scripts/verify_compliance.py --all`

## Competitors (What We Beat)
- Graphify: 22 languages, tree-sitter, Leiden clustering, 7 export formats
- Understand-Anything: 47 languages, 20 node types, 35 edge types, interactive dashboard
- CodeVision: 50+ languages, 35 node types, 55+ edge types, 12 actionable use cases, 12 export formats, full transitive dependency resolution, interactive WebGL dashboard
