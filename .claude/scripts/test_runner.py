#!/usr/bin/env python3
"""Test Runner - Executes backend and frontend tests and reports results.

Usage:
    python test_runner.py                # Run all tests
    python test_runner.py --backend      # Backend only
    python test_runner.py --frontend     # Frontend only
"""

import argparse
import json
import os
import re
import subprocess
import sys
from datetime import datetime


def run_command(cmd: list[str], cwd: str = ".") -> tuple[int, str, str]:
    """Run a command and return exit code, stdout, stderr."""
    try:
        result = subprocess.run(
            cmd,
            capture_output=True,
            text=True,
            timeout=600,
            cwd=cwd,
        )
        return result.returncode, result.stdout, result.stderr
    except subprocess.TimeoutExpired:
        return 1, "", "Command timed out after 600s"
    except FileNotFoundError:
        return 1, "", f"Command not found: {cmd[0]}"


def run_backend_tests() -> dict:
    """Run Maven backend tests."""
    print("Running backend tests...")
    code, stdout, stderr = run_command(
        ["mvn", "-f", "backend/pom.xml", "-pl", "api", "test", "-q"]
    )

    output = stdout + stderr
    result = {
        "type": "backend",
        "exit_code": code,
        "passed": 0,
        "failed": 0,
        "errors": 0,
        "skipped": 0,
        "coverage": None,
    }

    # Parse test results
    match = re.search(
        r'Tests run:\s*(\d+),\s*Failures:\s*(\d+),\s*Errors:\s*(\d+),\s*Skipped:\s*(\d+)',
        output,
    )
    if match:
        total = int(match.group(1))
        result["failed"] = int(match.group(2))
        result["errors"] = int(match.group(3))
        result["skipped"] = int(match.group(4))
        result["passed"] = total - result["failed"] - result["errors"] - result["skipped"]

    # Try to parse JaCoCo coverage from XML
    jacoco_xml = "backend/api/target/site/jacoco/jacoco.xml"
    if os.path.exists(jacoco_xml):
        try:
            import xml.etree.ElementTree as ET
            tree = ET.parse(jacoco_xml)
            root = tree.getroot()
            for counter in root.findall(".//counter[@type='LINE']"):
                missed = int(counter.get("missed", 0))
                covered = int(counter.get("covered", 0))
                total_lines = missed + covered
                if total_lines > 0:
                    result["coverage"] = round(covered / total_lines * 100, 1)
                break
        except Exception:
            pass

    return result


def run_frontend_tests() -> dict:
    """Run npm frontend tests."""
    print("Running frontend tests...")
    code, stdout, stderr = run_command(
        ["npm", "test"],
        cwd="frontend",
    )

    output = stdout + stderr
    result = {
        "type": "frontend",
        "exit_code": code,
        "passed": 0,
        "failed": 0,
        "errors": 0,
        "skipped": 0,
        "coverage": None,
    }

    # Parse vitest output
    match = re.search(r'(\d+)\s+passed', output)
    if match:
        result["passed"] = int(match.group(1))

    match = re.search(r'(\d+)\s+failed', output)
    if match:
        result["failed"] = int(match.group(1))

    return result


def print_report(results: list[dict]):
    """Print consolidated test report."""
    timestamp = datetime.now().strftime("%Y-%m-%d %H:%M:%S")

    print(f"\n{'=' * 60}")
    print(f"  Test Report - {timestamp}")
    print(f"{'=' * 60}")

    total_passed = 0
    total_failed = 0
    all_green = True

    for r in results:
        status = "PASS" if r["exit_code"] == 0 else "FAIL"
        if r["exit_code"] != 0:
            all_green = False

        total_passed += r["passed"]
        total_failed += r["failed"] + r["errors"]

        coverage_str = f"{r['coverage']}%" if r["coverage"] else "N/A"

        print(f"\n  {r['type'].upper()}: {status}")
        print(f"    Passed: {r['passed']}")
        if r["failed"] > 0:
            print(f"    Failed: {r['failed']}")
        if r["errors"] > 0:
            print(f"    Errors: {r['errors']}")
        if r["skipped"] > 0:
            print(f"    Skipped: {r['skipped']}")
        print(f"    Coverage: {coverage_str}")

    print(f"\n  TOTAL: {total_passed} passed, {total_failed} failed")
    print(f"  VERDICT: {'ALL GREEN' if all_green else 'FAILURES DETECTED'}")
    print(f"{'=' * 60}")

    # Write results to state file
    report_path = ".claude/checkpoints/last_test_report.json"
    os.makedirs(os.path.dirname(report_path), exist_ok=True)
    with open(report_path, "w") as f:
        json.dump({"timestamp": timestamp, "results": results, "all_green": all_green}, f, indent=2)

    return all_green


def main():
    parser = argparse.ArgumentParser(description="CodeVision Test Runner")
    parser.add_argument("--backend", action="store_true", help="Run backend tests only")
    parser.add_argument("--frontend", action="store_true", help="Run frontend tests only")

    args = parser.parse_args()

    results = []

    if args.backend or (not args.backend and not args.frontend):
        results.append(run_backend_tests())

    if args.frontend or (not args.backend and not args.frontend):
        results.append(run_frontend_tests())

    all_green = print_report(results)
    sys.exit(0 if all_green else 1)


if __name__ == "__main__":
    main()
