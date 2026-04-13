#!/usr/bin/env python3
"""Post-Test Hook - Logs test results after test execution.

Parses test output for pass/fail counts and logs to ACTIVE.md checkpoint.
"""

import sys
import os
import json
import re
from datetime import datetime


def parse_maven_output(output: str) -> dict:
    """Parse Maven Surefire test output."""
    result = {"passed": 0, "failed": 0, "errors": 0, "skipped": 0}

    # Look for "Tests run: X, Failures: Y, Errors: Z, Skipped: W"
    match = re.search(
        r'Tests run:\s*(\d+),\s*Failures:\s*(\d+),\s*Errors:\s*(\d+),\s*Skipped:\s*(\d+)',
        output
    )
    if match:
        total = int(match.group(1))
        result["failed"] = int(match.group(2))
        result["errors"] = int(match.group(3))
        result["skipped"] = int(match.group(4))
        result["passed"] = total - result["failed"] - result["errors"] - result["skipped"]

    return result


def parse_npm_output(output: str) -> dict:
    """Parse npm/vitest test output."""
    result = {"passed": 0, "failed": 0, "errors": 0, "skipped": 0}

    # Vitest format: "Tests  X passed (Y)"
    match = re.search(r'(\d+)\s+passed', output)
    if match:
        result["passed"] = int(match.group(1))

    match = re.search(r'(\d+)\s+failed', output)
    if match:
        result["failed"] = int(match.group(1))

    return result


def log_to_checkpoint(result: dict, test_type: str):
    """Append test results to ACTIVE.md."""
    checkpoint_path = ".claude/checkpoints/ACTIVE.md"
    timestamp = datetime.now().isoformat()

    total = result["passed"] + result["failed"] + result["errors"]
    status = "PASS" if result["failed"] == 0 and result["errors"] == 0 else "FAIL"

    entry = f"\n### Test Run: {test_type} [{timestamp}]\n"
    entry += f"- Status: **{status}**\n"
    entry += f"- Passed: {result['passed']}/{total}\n"
    if result["failed"] > 0:
        entry += f"- Failed: {result['failed']}\n"
    if result["errors"] > 0:
        entry += f"- Errors: {result['errors']}\n"
    if result["skipped"] > 0:
        entry += f"- Skipped: {result['skipped']}\n"

    try:
        with open(checkpoint_path, "a") as f:
            f.write(entry)
    except FileNotFoundError:
        os.makedirs(os.path.dirname(checkpoint_path), exist_ok=True)
        with open(checkpoint_path, "w") as f:
            f.write(f"# Active Checkpoint\n{entry}")


def main():
    tool_output = os.environ.get("TOOL_OUTPUT", "")

    if not tool_output:
        sys.exit(0)

    # Determine test type from command
    tool_input = os.environ.get("TOOL_INPUT", "{}")
    try:
        data = json.loads(tool_input)
        command = data.get("command", "")
    except (json.JSONDecodeError, AttributeError):
        command = ""

    if "mvn" in command:
        result = parse_maven_output(tool_output)
        log_to_checkpoint(result, "Maven/JUnit")
    elif "npm" in command:
        result = parse_npm_output(tool_output)
        log_to_checkpoint(result, "npm/Vitest")

    sys.exit(0)


if __name__ == "__main__":
    main()
