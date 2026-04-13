---
name: new-phase
description: Start a new sprint with proper initialization
---

# Start New Sprint

Initialize a new sprint with proper state management.

## Steps

1. Parse sprint number from `$ARGUMENTS` (e.g., `1`, `2`, etc.)
2. Create git branch: `sprint-N/description`
3. Update `orchestration_state.json`:
```json
{
  "current_sprint": N,
  "status": "IN_PROGRESS",
  "iteration": 1,
  "started_at": "ISO-timestamp"
}
```
4. Read sprint details from plan file
5. List expected deliverables
6. Begin implementation by dispatching to `/orchestrate --sprint N`
