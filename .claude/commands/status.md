---
name: status
description: Quick project status overview
---

# Project Status

Show current project state.

## Steps

1. Read `.claude/checkpoints/orchestration_state.json` for current sprint and status
2. Run `git log --oneline -10` for recent commits
3. Run `git status` for working tree state
4. Read `.claude/checkpoints/ACTIVE.md` for current session state
5. Check for any BLOCKED items in `.claude/checkpoints/BLOCKED/`
6. Report:
   - Current sprint and status
   - Last 5 commits
   - Active blockers (if any)
   - Next action to take
