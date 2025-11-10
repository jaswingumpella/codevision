# Iteration 13 Completion – Call Graph & SCC

## Summary

Iteration 13 layered call-graph awareness and cycle detection on top of the bytecode scan. We now capture method-level invocation edges with ASM, aggregate them to class dependencies, and tag strongly connected components so downstream traversals/diagrams remain cycle-safe.

## Key Deliverables

- `BytecodeCallGraphScanner` parses every compiled class/jar to record `INVOKE*` instructions and produce `GraphModel.MethodCallEdge` plus class-level `DependencyEdge` entries.
- `TarjanScc` annotates each class with `sccId`/`inCycle`, enabling deterministic traversal limits.
- `dependencies.csv` export lists every class-to-class relationship discovered in the compiled graph.

## Verification

- Unit-tested `TarjanScc` on cyclic/acyclic graphs.
- Exercised ASM scanner on fixture jars to confirm method edges and aggregated class dependencies match expectations.
