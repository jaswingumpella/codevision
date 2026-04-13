---
name: algorithm-review
description: Review graph algorithm implementations for correctness
---

# Algorithm Review Skill

When reviewing graph/algorithm/ changes:

1. Every algorithm implements GraphAlgorithm<R> interface
2. No hardcoded limits in any algorithm
3. All traversals use visited-set for cycle detection
4. PageRank: values sum to ~1.0, convergence check present
5. Leiden/Louvain: all nodes assigned to exactly one community
6. Tarjan SCC: iterative implementation (not recursive — avoids stack overflow on large graphs)
7. Dead code detector: walks from ALL entry points (endpoints, scheduled, main, tests)
8. Impact analyzer: correct reverse transitive closure
9. Property-based tests exist for mathematical invariants
10. Known-answer tests with small fixture graphs
