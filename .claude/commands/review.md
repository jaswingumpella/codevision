---
name: review
description: Trigger multi-role review for current sprint
---

# Multi-Role Review

Spawn all 7 review agents in parallel to evaluate the current sprint.

## Steps

1. Determine current sprint from `orchestration_state.json`
2. Get list of changed files: `git diff --name-only main...HEAD`
3. Spawn review agents in parallel:
   - **Architect**: structural integrity, no limits, SOLID
   - **Product Owner**: user value, actionability
   - **Developer**: code quality, Java 21 idioms
   - **UX Specialist**: dashboard usability (sprints 6-7 only)
   - **UI Engineer**: component architecture (sprints 6-7 only)
   - **Tester**: coverage, Given/When/Then, edge cases
   - **Strategist**: competitive positioning
4. Collect all findings
5. Consolidate into single report:
   - CRITICAL items (must fix before advancing)
   - MAJOR items (should fix, can defer with justification)
   - MINOR items (add to backlog)
6. Write review report to `checkpoints/agent_outputs/sprint-N-review.json`
