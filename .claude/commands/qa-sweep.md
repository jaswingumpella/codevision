---
name: qa-sweep
description: Comprehensive QA sweep across backend and frontend
---

# QA Sweep

Run comprehensive quality assurance checks.

## Steps

1. **Backend Tests**:
```bash
mvn -f backend/pom.xml -pl api test
```

2. **Frontend Tests**:
```bash
npm --prefix frontend test
```

3. **Compliance Check**:
```bash
python .claude/scripts/verify_compliance.py --all
```

4. **Architecture Check**:
   - Grep for hardcoded limits: `MAX_`, `.limit(`, `maxDepth`
   - Verify no depth-based termination without cycle detection
   - Check package boundary violations

5. **Test Coverage**:
   - Parse JaCoCo XML report
   - Verify >= 90% on new code

6. **Report**: Summarize findings by category (CRITICAL/MAJOR/MINOR)
