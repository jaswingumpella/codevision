#!/usr/bin/env python3
"""TDD Guard Hook - PreWrite for backend/api/src/main/**/*.java

Warns if a corresponding test file doesn't exist yet.
Does NOT block — only warns to encourage TDD discipline.
"""

import sys
import os
import json


def main():
    # Hook receives context via environment or stdin
    tool_input = os.environ.get("TOOL_INPUT", "{}")
    try:
        data = json.loads(tool_input)
        file_path = data.get("file_path", "")
    except (json.JSONDecodeError, AttributeError):
        file_path = ""

    if not file_path:
        sys.exit(0)

    # Only check main source files (not test files)
    if "src/main/java" not in file_path:
        sys.exit(0)

    # Derive test file path
    test_path = file_path.replace("src/main/java", "src/test/java")
    # Remove .java and add Test.java
    if test_path.endswith(".java"):
        test_path = test_path[:-5] + "Test.java"

    if not os.path.exists(test_path):
        class_name = os.path.basename(file_path).replace(".java", "")
        print(f"[TDD Guard] WARNING: No test file found for {class_name}")
        print(f"  Expected: {test_path}")
        print(f"  Consider writing the test FIRST (TDD: RED -> GREEN -> REFACTOR)")
        # Exit 0 = warn only, don't block
        sys.exit(0)

    sys.exit(0)


if __name__ == "__main__":
    main()
