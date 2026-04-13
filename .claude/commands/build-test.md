---
name: build-test
description: Run full build and test suite
---

# Build and Test

Run the complete build and test pipeline.

## Steps

1. **Backend Compile**:
```bash
mvn -f backend/pom.xml -pl api compile -q
```

2. **Backend Tests**:
```bash
mvn -f backend/pom.xml -pl api test
```

3. **Frontend Build**:
```bash
npm --prefix frontend run build
```

4. **Frontend Tests**:
```bash
npm --prefix frontend test
```

5. **Report**: Pass/fail counts, coverage percentage, build time
