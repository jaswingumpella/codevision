---
name: orchestrate
description: Main entry point for autonomous sprint execution with multi-role review
---

# Orchestrate Sprint Execution

You are the orchestration engine for CodeVision's autonomous development workflow.

## Process

### 1. Determine Current State
Read `.claude/checkpoints/orchestration_state.json` to find current sprint and status.

### 2. Execute Sprint
For the current sprint, follow this sequence:

**Step A: Implementation**
- Read the plan at `.claude/plans/abundant-prancing-raven.md` for sprint details
- Spawn implementation agents based on sprint assignment table:
  - Sprints 1-5: developer + tester agents (backend focus)
  - Sprints 6-7: + ui-engineer + ux-specialist (frontend focus)
  - Sprints 8-9: + architect (complex design)
  - Sprint 10: + strategist (final polish)
- Each agent implements their portion in parallel where possible

**Step B: Build Validation**
```bash
mvn -f backend/pom.xml -pl api compile
npm --prefix frontend run build  # if frontend changes
```

**Step C: Test Execution**
```bash
python .claude/scripts/test_runner.py
```
All tests must pass. Coverage must meet 90% gate.

**Step D: Multi-Role Review**
Spawn ALL 7 review agents in parallel:
- architect.md, product-owner.md, developer.md
- ux-specialist.md, ui-engineer.md, tester.md, strategist.md
Each produces structured findings JSON.

**Step E: Fix Critical Items**
- CRITICAL findings: fix immediately
- MAJOR findings: fix if iteration budget allows (max 3 iterations)
- MINOR findings: add to backlog

**Step F: QA Signoff**
```bash
python .claude/scripts/verify_compliance.py --all
```
Must return exit code 0. If not, loop back to Step A (max 3 iterations).

**Step G: Advance**
Update `orchestration_state.json`:
- Mark current sprint as completed
- Advance to next sprint
- Write checkpoint summary to `checkpoints/summaries/sprint-N-summary.md`

### 3. Arguments
- `$ARGUMENTS` may contain: `--sprint N` (start at sprint N), `--resume` (continue from last state), `--dry-run` (plan only)

### 4. Escalation
If max iterations (3) exceeded without QA pass:
- Write escalation to `checkpoints/BLOCKED/sprint-N-escalation.md`
- Report to user with specific failing checks
