---
name: check-phase
description: Verify current sprint completion against quality gates
---

# Check Phase Completion

Verify the current sprint meets all quality gates.

## Steps

1. Read `.claude/checkpoints/orchestration_state.json` for current sprint
2. Run compliance checks:
```bash
python .claude/scripts/verify_compliance.py --all
```
3. Run tests:
```bash
mvn -f backend/pom.xml -pl api test
npm --prefix frontend test
```
4. Check for expected files from the plan
5. Report status:
   - PASS: All gates met, ready for review
   - FAIL: List specific failures with remediation suggestions
