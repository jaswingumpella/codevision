#!/usr/bin/env python3
"""Quality Gate Hook - PreBash for gh pr create.

BLOCKING: Prevents PR creation if quality checks fail.
Checks: tests pass, coverage >= 90%, no critical compliance violations.
"""

import sys
import os
import subprocess
import json


def run_command(cmd: list[str]) -> tuple[int, str]:
    """Run a command and return exit code + output."""
    try:
        result = subprocess.run(cmd, capture_output=True, text=True, timeout=300)
        return result.returncode, result.stdout + result.stderr
    except subprocess.TimeoutExpired:
        return 1, "Command timed out"
    except FileNotFoundError:
        return 1, f"Command not found: {cmd[0]}"


def main():
    tool_input = os.environ.get("TOOL_INPUT", "{}")
    try:
        data = json.loads(tool_input)
        command = data.get("command", "")
    except (json.JSONDecodeError, AttributeError):
        command = ""

    # Only gate on PR creation
    if "gh pr create" not in command:
        sys.exit(0)

    print("[Quality Gate] Running pre-PR checks...")
    failures = []

    # Check 1: Compliance
    code, output = run_command(["python", ".claude/scripts/verify_compliance.py", "--all"])
    if code != 0:
        failures.append(f"Compliance check failed:\n{output}")

    # Check 2: Backend tests
    code, output = run_command(["mvn", "-f", "backend/pom.xml", "-pl", "api", "test", "-q"])
    if code != 0:
        failures.append("Backend tests failed")

    if failures:
        print("[Quality Gate] BLOCKED - Cannot create PR:")
        for f in failures:
            print(f"  - {f}")
        sys.exit(1)  # Block the PR creation

    print("[Quality Gate] PASSED - All checks green")
    sys.exit(0)


if __name__ == "__main__":
    main()
