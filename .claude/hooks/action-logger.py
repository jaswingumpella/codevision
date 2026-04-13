#!/usr/bin/env python3
"""Action Logger Hook - PostToolUse for all tools.

Appends every tool action to ACTIVE.md checkpoint for audit trail.
"""

import sys
import os
import json
from datetime import datetime


def main():
    tool_name = os.environ.get("TOOL_NAME", "unknown")
    tool_input = os.environ.get("TOOL_INPUT", "{}")

    try:
        data = json.loads(tool_input)
    except (json.JSONDecodeError, AttributeError):
        data = {}

    timestamp = datetime.now().strftime("%H:%M:%S")
    checkpoint_path = ".claude/checkpoints/ACTIVE.md"

    # Extract relevant info based on tool type
    detail = ""
    if tool_name == "Write":
        detail = f"Write: {data.get('file_path', 'unknown')}"
    elif tool_name == "Edit":
        detail = f"Edit: {data.get('file_path', 'unknown')}"
    elif tool_name == "Bash":
        cmd = data.get("command", "")
        detail = f"Bash: {cmd[:80]}{'...' if len(cmd) > 80 else ''}"
    elif tool_name == "Read":
        detail = f"Read: {data.get('file_path', 'unknown')}"
    else:
        detail = f"{tool_name}"

    entry = f"- [{timestamp}] {detail}\n"

    try:
        with open(checkpoint_path, "a") as f:
            f.write(entry)
    except FileNotFoundError:
        os.makedirs(os.path.dirname(checkpoint_path), exist_ok=True)
        with open(checkpoint_path, "w") as f:
            f.write(f"# Active Checkpoint\n\n## Action Log\n{entry}")

    sys.exit(0)


if __name__ == "__main__":
    main()
