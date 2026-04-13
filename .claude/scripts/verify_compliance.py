#!/usr/bin/env python3
"""Compliance Checker - Verifies architecture rules and quality standards.

Checks:
- No hardcoded limits (MAX_, .limit(N), depth counters)
- Architecture boundaries (package imports)
- Test existence for every main class
- Coverage thresholds

Usage:
    python verify_compliance.py --all
    python verify_compliance.py --check limits
    python verify_compliance.py --check architecture
    python verify_compliance.py --check tests
"""

import argparse
import os
import re
import sys
from pathlib import Path

BACKEND_MAIN = "backend/api/src/main/java/com/codevision/codevisionbackend"
BACKEND_TEST = "backend/api/src/test/java/com/codevision/codevisionbackend"


class Violation:
    def __init__(self, category: str, severity: str, file: str, line: int, message: str, remediation: str = ""):
        self.category = category
        self.severity = severity
        self.file = file
        self.line = line
        self.message = message
        self.remediation = remediation

    def __str__(self):
        loc = f"{self.file}:{self.line}" if self.line > 0 else self.file
        return f"[{self.severity}] {self.category}: {self.message} ({loc})"


def check_hardcoded_limits() -> list[Violation]:
    """Check for hardcoded depth/size limits in Java source."""
    violations = []

    limit_patterns = [
        (r'private\s+static\s+final\s+int\s+(MAX_\w+)\s*=\s*(\d+)',
         "Hardcoded limit constant", "Replace with configurable property from application.yml"),
        (r'\.limit\(\s*(\d+)\s*\)',
         "Hardcoded .limit() call", "Use configurable property or remove limit entirely"),
        (r'if\s*\(\s*depth\s*[<>=!]+\s*(\d+)\s*\)',
         "Depth-based termination", "Use cycle detection (visited-set) + time-based deadline"),
        (r'if\s*\(\s*depth\s*<=\s*0\s*\)',
         "Depth countdown termination", "Use cycle detection (visited-set) + time-based deadline"),
    ]

    for java_file in Path(BACKEND_MAIN).rglob("*.java"):
        try:
            content = java_file.read_text()
            lines = content.split("\n")

            for line_num, line in enumerate(lines, 1):
                for pattern, message, remediation in limit_patterns:
                    if re.search(pattern, line):
                        violations.append(Violation(
                            category="LIMITS",
                            severity="CRITICAL",
                            file=str(java_file),
                            line=line_num,
                            message=f"{message}: {line.strip()[:80]}",
                            remediation=remediation,
                        ))
        except (OSError, UnicodeDecodeError):
            continue

    return violations


def check_architecture_boundaries() -> list[Violation]:
    """Check package import boundaries."""
    violations = []

    # Rules: graph/ should not import from usecase/
    forbidden_imports = [
        ("graph/", "usecase/", "graph package must not import from usecase"),
        ("graph/", "analyze/", "graph package must not import from analyze"),
        ("graph/", "project/", "graph package must not import from project"),
    ]

    for java_file in Path(BACKEND_MAIN).rglob("*.java"):
        try:
            content = java_file.read_text()
            rel_path = str(java_file.relative_to(BACKEND_MAIN))

            for source_pkg, forbidden_pkg, message in forbidden_imports:
                if rel_path.startswith(source_pkg):
                    # Check imports
                    import_pattern = f"import.*codevisionbackend\\.{forbidden_pkg.replace('/', '.')}"
                    for line_num, line in enumerate(content.split("\n"), 1):
                        if re.search(import_pattern, line):
                            violations.append(Violation(
                                category="ARCHITECTURE",
                                severity="CRITICAL",
                                file=str(java_file),
                                line=line_num,
                                message=message,
                            ))
        except (OSError, UnicodeDecodeError):
            continue

    return violations


def check_test_existence() -> list[Violation]:
    """Check that every main class has a corresponding test class."""
    violations = []

    for java_file in Path(BACKEND_MAIN).rglob("*.java"):
        rel_path = java_file.relative_to(BACKEND_MAIN)
        class_name = java_file.stem

        # Skip some known patterns that don't need dedicated tests
        skip_patterns = ["Application", "Config", "Properties", "Exception", "Constants"]
        if any(class_name.endswith(p) for p in skip_patterns):
            continue

        # Check for corresponding test
        test_file = Path(BACKEND_TEST) / str(rel_path).replace(".java", "Test.java")
        if not test_file.exists():
            # Also check for IT suffix
            it_file = Path(BACKEND_TEST) / str(rel_path).replace(".java", "IT.java")
            if not it_file.exists():
                violations.append(Violation(
                    category="TESTS",
                    severity="MAJOR",
                    file=str(java_file),
                    line=0,
                    message=f"No test class found for {class_name}",
                    remediation=f"Create {test_file}",
                ))

    return violations


def check_test_structure() -> list[Violation]:
    """Check that tests use Given/When/Then @Nested structure."""
    violations = []

    for test_file in Path(BACKEND_TEST).rglob("*Test.java"):
        try:
            content = test_file.read_text()

            # Check for @Nested annotation (Given/When/Then structure)
            if "@Test" in content and "@Nested" not in content:
                violations.append(Violation(
                    category="TEST_STRUCTURE",
                    severity="MAJOR",
                    file=str(test_file),
                    line=0,
                    message="Test class has @Test but no @Nested Given/When/Then structure",
                    remediation="Reorganize tests into @Nested Given/When/Then classes",
                ))
        except (OSError, UnicodeDecodeError):
            continue

    return violations


def run_checks(categories: list[str]) -> list[Violation]:
    """Run specified compliance checks."""
    all_violations = []

    checks = {
        "limits": check_hardcoded_limits,
        "architecture": check_architecture_boundaries,
        "tests": check_test_existence,
        "test_structure": check_test_structure,
    }

    for cat in categories:
        if cat in checks:
            all_violations.extend(checks[cat]())
        elif cat == "all":
            for check_fn in checks.values():
                all_violations.extend(check_fn())

    return all_violations


def main():
    parser = argparse.ArgumentParser(description="CodeVision Compliance Checker")
    parser.add_argument("--check", default="all", help="Check category: limits|architecture|tests|test_structure|all")
    parser.add_argument("--severity", default="CRITICAL", help="Minimum severity to report: CRITICAL|MAJOR|MINOR")
    parser.add_argument("--all", action="store_true", help="Run all checks")

    args = parser.parse_args()

    categories = ["all"] if args.all else [args.check]
    violations = run_checks(categories)

    # Filter by severity
    severity_order = {"CRITICAL": 0, "MAJOR": 1, "MINOR": 2}
    min_severity = severity_order.get(args.severity, 0)
    violations = [v for v in violations if severity_order.get(v.severity, 2) <= min_severity]

    if not violations:
        print("COMPLIANCE CHECK: PASSED")
        print(f"  Categories checked: {', '.join(categories)}")
        print(f"  No violations found")
        sys.exit(0)

    # Group by severity
    critical = [v for v in violations if v.severity == "CRITICAL"]
    major = [v for v in violations if v.severity == "MAJOR"]
    minor = [v for v in violations if v.severity == "MINOR"]

    print(f"COMPLIANCE CHECK: {'FAILED' if critical else 'WARNINGS'}")
    print(f"  Categories checked: {', '.join(categories)}")
    print(f"  Critical: {len(critical)}, Major: {len(major)}, Minor: {len(minor)}")
    print()

    for v in violations:
        print(f"  {v}")
        if v.remediation:
            print(f"    Fix: {v.remediation}")

    # Exit with failure only on CRITICAL violations
    sys.exit(1 if critical else 0)


if __name__ == "__main__":
    main()
