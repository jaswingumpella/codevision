---
name: use-case-review
description: Review use case implementations for actionability
---

# Use Case Review Skill

When reviewing usecase/ changes:

1. Each use case service answers a specific user question (see product-owner.md)
2. Return types are structured, not raw graph data (user sees actionable items)
3. Each response includes:
   - Summary (one-line answer to the question)
   - Details (list of specific items)
   - Suggested actions (what to do next)
4. REST endpoints follow consistent naming: GET /api/usecases/{projectId}/{use-case-name}
5. Services only depend on graph/ package (not analysis/ directly)
6. Each service has comprehensive Given/When/Then tests
7. Edge cases handled: empty graph, single node, disconnected graph
