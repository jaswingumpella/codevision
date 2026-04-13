#!/usr/bin/env python3
"""Pre-Write Check Hook - Warns on code quality issues before writing.

Checks:
- Java: no hardcoded MAX_ limits, no bare catch, no System.out
- JSX: no console.log in production code

Does NOT block — only warns.
"""

import sys
import os
import json
import re


def check_java(content: str, file_path: str) -> list[str]:
    """Check Java file for issues."""
    warnings = []

    # Check for hardcoded limits (the #1 rule violation)
    limit_patterns = [
        (r'private\s+static\s+final\s+int\s+MAX_\w+\s*=\s*\d+', "Hardcoded MAX_ limit found"),
        (r'\.limit\(\s*\d+\s*\)', "Hardcoded .limit(N) found — should be configurable"),
        (r'if\s*\(\s*depth\s*[<>=]+\s*\d+', "Depth-based termination found — use cycle detection + time-based deadline"),
    ]

    for pattern, message in limit_patterns:
        matches = re.findall(pattern, content)
        if matches:
            warnings.append(f"  LIMIT VIOLATION: {message}")
            for m in matches[:3]:
                warnings.append(f"    Found: {m.strip()}")

    # Check for bare catch
    if re.search(r'catch\s*\(\s*Exception\s+\w+\s*\)\s*\{[\s\n]*\}', content):
        warnings.append("  Empty catch block found — handle or rethrow")

    # Check for System.out
    if "System.out.print" in content and "src/test" not in file_path:
        warnings.append("  System.out found — use SLF4J Logger instead")

    # Check for magic numbers in comparisons
    magic_pattern = r'(?:if|while|for).*[<>=]+\s*(?:2[5-9]|[3-9]\d|\d{3,})[^.]'
    if re.search(magic_pattern, content):
        warnings.append("  Possible magic number in condition — use named constant or config")

    return warnings


def check_jsx(content: str, file_path: str) -> list[str]:
    """Check JSX file for issues."""
    warnings = []

    if "console.log" in content:
        warnings.append("  console.log found in production code — remove or use proper logging")

    return warnings


def main():
    tool_input = os.environ.get("TOOL_INPUT", "{}")
    try:
        data = json.loads(tool_input)
        file_path = data.get("file_path", "")
        content = data.get("content", "")
    except (json.JSONDecodeError, AttributeError):
        sys.exit(0)

    if not file_path or not content:
        sys.exit(0)

    warnings = []

    if file_path.endswith(".java"):
        warnings = check_java(content, file_path)
    elif file_path.endswith(".jsx") or file_path.endswith(".tsx"):
        warnings = check_jsx(content, file_path)

    if warnings:
        filename = os.path.basename(file_path)
        print(f"[Pre-Write Check] Warnings for {filename}:")
        for w in warnings:
            print(w)

    # Always exit 0 — warn only
    sys.exit(0)


if __name__ == "__main__":
    main()
