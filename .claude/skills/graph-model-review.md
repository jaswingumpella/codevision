---
name: graph-model-review
description: Review KnowledgeGraph changes for completeness and correctness
---

# Graph Model Review Skill

When reviewing changes to the graph/ package:

1. Verify all 35 node types are present in KgNodeType enum
2. Verify all 55+ edge types are present in KgEdgeType enum
3. Check KgNode has complete NodeMetadata (visibility, modifiers, annotations, generics, docs, complexity)
4. Check KgEdge has Provenance and ConfidenceLevel
5. Verify KnowledgeGraph has both outEdges and inEdges adjacency indexes
6. Verify KnowledgeGraphBuilder produces valid, non-null graphs
7. Verify GraphModelAdapter correctly converts in both directions
8. No mutable fields in records
9. Proper equals/hashCode for map keys
