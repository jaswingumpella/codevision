#!/usr/bin/env python3
"""Phase Orchestrator - Autonomous multi-agent sprint orchestration engine.

Manages the lifecycle of sprints: planning -> implementation -> test -> review -> QA signoff -> advance.

Usage:
    python phase_orchestrator.py --sprint N         # Start from sprint N
    python phase_orchestrator.py --resume            # Resume from last state
    python phase_orchestrator.py --dry-run            # Plan only, no execution
    python phase_orchestrator.py --status             # Show current state
"""

import argparse
import json
import os
import sys
from datetime import datetime
from pathlib import Path

# Sprint configuration
SPRINT_CONFIG = {
    0: {
        "name": "Ecosystem Setup",
        "slug": "ecosystem-setup",
        "primary_agents": ["orchestrator"],
        "review_agents": [],
        "expected_files": [
            "CLAUDE.md",
            ".claude/settings.json",
            ".claude/agents/architect.md",
            ".claude/agents/tester.md",
            ".claude/scripts/phase_orchestrator.py",
            ".claude/scripts/verify_compliance.py",
        ],
    },
    1: {
        "name": "Knowledge Graph Foundation",
        "slug": "knowledge-graph-foundation",
        "primary_agents": ["developer", "tester"],
        "review_agents": ["architect", "product-owner", "developer", "tester", "strategist"],
        "expected_files": [
            "backend/api/src/main/java/com/codevision/codevisionbackend/graph/KnowledgeGraph.java",
            "backend/api/src/main/java/com/codevision/codevisionbackend/graph/KgNode.java",
            "backend/api/src/main/java/com/codevision/codevisionbackend/graph/KgEdge.java",
            "backend/api/src/main/java/com/codevision/codevisionbackend/graph/KgNodeType.java",
            "backend/api/src/main/java/com/codevision/codevisionbackend/graph/KgEdgeType.java",
            "backend/api/src/main/java/com/codevision/codevisionbackend/graph/NodeMetadata.java",
            "backend/api/src/main/java/com/codevision/codevisionbackend/graph/AnnotationValue.java",
            "backend/api/src/main/java/com/codevision/codevisionbackend/graph/Provenance.java",
            "backend/api/src/main/java/com/codevision/codevisionbackend/graph/ConfidenceLevel.java",
            "backend/api/src/main/java/com/codevision/codevisionbackend/graph/KnowledgeGraphBuilder.java",
            "backend/api/src/main/java/com/codevision/codevisionbackend/graph/GraphModelAdapter.java",
        ],
    },
    2: {
        "name": "Enhanced Java Analysis",
        "slug": "enhanced-java-analysis",
        "primary_agents": ["developer", "tester"],
        "review_agents": ["architect", "product-owner", "developer", "tester", "strategist"],
        "expected_files": [
            "backend/api/src/main/java/com/codevision/codevisionbackend/analysis/MetricsCalculator.java",
            "backend/api/src/main/java/com/codevision/codevisionbackend/analysis/DocumentationExtractor.java",
        ],
    },
    3: {
        "name": "Dependency Resolution Engine",
        "slug": "dependency-resolution",
        "primary_agents": ["developer", "tester"],
        "review_agents": ["architect", "product-owner", "developer", "tester", "strategist"],
        "expected_files": [
            "backend/api/src/main/java/com/codevision/codevisionbackend/analysis/dependency/DependencyResolver.java",
            "backend/api/src/main/java/com/codevision/codevisionbackend/analysis/dependency/DependencyResolverRegistry.java",
            "backend/api/src/main/java/com/codevision/codevisionbackend/analysis/dependency/MavenDependencyResolver.java",
            "backend/api/src/main/java/com/codevision/codevisionbackend/analysis/dependency/ExclusionConfig.java",
        ],
    },
    4: {
        "name": "Graph Algorithms",
        "slug": "graph-algorithms",
        "primary_agents": ["developer", "tester"],
        "review_agents": ["architect", "product-owner", "developer", "tester", "strategist"],
        "expected_files": [
            "backend/api/src/main/java/com/codevision/codevisionbackend/graph/algorithm/GraphAlgorithm.java",
            "backend/api/src/main/java/com/codevision/codevisionbackend/graph/algorithm/PageRankAlgorithm.java",
            "backend/api/src/main/java/com/codevision/codevisionbackend/graph/algorithm/LeidenCommunityDetection.java",
            "backend/api/src/main/java/com/codevision/codevisionbackend/graph/algorithm/DeadCodeDetector.java",
        ],
    },
    5: {
        "name": "Actionable Use Cases",
        "slug": "actionable-use-cases",
        "primary_agents": ["developer", "tester"],
        "review_agents": ["architect", "product-owner", "developer", "tester", "strategist"],
        "expected_files": [
            "backend/api/src/main/java/com/codevision/codevisionbackend/usecase/UseCaseController.java",
        ],
    },
    6: {
        "name": "Interactive Dashboard",
        "slug": "interactive-dashboard",
        "primary_agents": ["developer", "ui-engineer", "ux-specialist"],
        "review_agents": ["architect", "product-owner", "developer", "ux-specialist", "ui-engineer", "tester", "strategist"],
        "expected_files": [
            "frontend/src/components/graph/GraphExplorer.jsx",
            "frontend/src/components/graph/GraphCanvas.jsx",
        ],
    },
    7: {
        "name": "Use Case Views + Tours",
        "slug": "use-case-views",
        "primary_agents": ["developer", "ui-engineer", "ux-specialist"],
        "review_agents": ["architect", "product-owner", "developer", "ux-specialist", "ui-engineer", "tester", "strategist"],
        "expected_files": [
            "frontend/src/components/usecases/OnboardingGuide.jsx",
        ],
    },
    8: {
        "name": "Multi-Language Support",
        "slug": "multi-language",
        "primary_agents": ["developer", "architect"],
        "review_agents": ["architect", "product-owner", "developer", "tester", "strategist"],
        "expected_files": [
            "backend/api/src/main/java/com/codevision/codevisionbackend/analysis/multilang/TreeSitterBridge.java",
        ],
    },
    9: {
        "name": "Unlimited Call Graph + Persistence",
        "slug": "call-graph-persistence",
        "primary_agents": ["developer", "architect", "tester"],
        "review_agents": ["architect", "product-owner", "developer", "tester", "strategist"],
        "expected_files": [
            "backend/api/src/main/java/com/codevision/codevisionbackend/callgraph/UnlimitedCallGraphBuilder.java",
            "backend/api/src/main/java/com/codevision/codevisionbackend/graph/KnowledgeGraphPersistService.java",
        ],
    },
    10: {
        "name": "Export + Polish",
        "slug": "export-polish",
        "primary_agents": ["developer", "tester", "strategist"],
        "review_agents": ["architect", "product-owner", "developer", "tester", "strategist"],
        "expected_files": [
            "backend/api/src/main/java/com/codevision/codevisionbackend/graph/export/JsonExporter.java",
        ],
    },
}

STATE_FILE = ".claude/checkpoints/orchestration_state.json"
ACTIVE_FILE = ".claude/checkpoints/ACTIVE.md"


def load_state() -> dict:
    """Load orchestration state from JSON file."""
    if os.path.exists(STATE_FILE):
        with open(STATE_FILE) as f:
            return json.load(f)
    return {
        "current_sprint": 0,
        "status": "NOT_STARTED",
        "iteration": 0,
        "max_iterations": 3,
        "started_at": None,
        "completed_sprints": [],
        "backlog": [],
    }


def save_state(state: dict):
    """Save orchestration state to JSON file."""
    os.makedirs(os.path.dirname(STATE_FILE), exist_ok=True)
    with open(STATE_FILE, "w") as f:
        json.dump(state, f, indent=2, default=str)


def check_expected_files(sprint_num: int) -> dict:
    """Check which expected files exist for a sprint."""
    config = SPRINT_CONFIG.get(sprint_num, {})
    expected = config.get("expected_files", [])

    found = []
    missing = []
    for f in expected:
        if os.path.exists(f):
            found.append(f)
        else:
            missing.append(f)

    return {"found": found, "missing": missing, "total": len(expected)}


def show_status(state: dict):
    """Display current orchestration status."""
    sprint = state["current_sprint"]
    config = SPRINT_CONFIG.get(sprint, {})

    print(f"{'=' * 60}")
    print(f"  CodeVision Orchestration Status")
    print(f"{'=' * 60}")
    print(f"  Current Sprint: {sprint} - {config.get('name', 'Unknown')}")
    print(f"  Status: {state['status']}")
    print(f"  Iteration: {state['iteration']}/{state['max_iterations']}")
    print(f"  Started: {state.get('started_at', 'N/A')}")
    print(f"  Completed: {state.get('completed_sprints', [])}")
    print()

    # Check files
    files = check_expected_files(sprint)
    print(f"  Expected Files: {files['total']}")
    print(f"  Found: {len(files['found'])}")
    if files["missing"]:
        print(f"  Missing: {len(files['missing'])}")
        for m in files["missing"][:5]:
            print(f"    - {m}")

    # Backlog
    backlog = state.get("backlog", [])
    if backlog:
        print(f"\n  Backlog Items: {len(backlog)}")
        for item in backlog[:5]:
            print(f"    [{item.get('priority', '?')}] {item.get('title', '?')}")

    print(f"{'=' * 60}")


def start_sprint(state: dict, sprint_num: int):
    """Initialize a new sprint."""
    config = SPRINT_CONFIG.get(sprint_num)
    if not config:
        print(f"ERROR: Sprint {sprint_num} not configured")
        sys.exit(1)

    state["current_sprint"] = sprint_num
    state["status"] = "IN_PROGRESS"
    state["iteration"] = 1
    state["started_at"] = datetime.now().isoformat()

    save_state(state)

    print(f"Sprint {sprint_num}: {config['name']} - STARTED")
    print(f"  Primary agents: {', '.join(config['primary_agents'])}")
    print(f"  Expected files: {len(config['expected_files'])}")
    print(f"  Review agents: {', '.join(config.get('review_agents', []))}")


def complete_sprint(state: dict, sprint_num: int):
    """Mark sprint as completed and write summary."""
    config = SPRINT_CONFIG.get(sprint_num, {})

    if sprint_num not in state.get("completed_sprints", []):
        state.setdefault("completed_sprints", []).append(sprint_num)

    state["status"] = "COMPLETED"

    # Write summary
    summary_path = f".claude/checkpoints/summaries/sprint-{sprint_num}-summary.md"
    os.makedirs(os.path.dirname(summary_path), exist_ok=True)

    files = check_expected_files(sprint_num)
    with open(summary_path, "w") as f:
        f.write(f"# Sprint {sprint_num}: {config.get('name', 'Unknown')} - Summary\n\n")
        f.write(f"- Completed: {datetime.now().isoformat()}\n")
        f.write(f"- Iterations: {state['iteration']}\n")
        f.write(f"- Files created: {len(files['found'])}/{files['total']}\n")
        if files["missing"]:
            f.write(f"- Missing files: {', '.join(files['missing'])}\n")

    save_state(state)
    print(f"Sprint {sprint_num}: {config.get('name', 'Unknown')} - COMPLETED")


def main():
    parser = argparse.ArgumentParser(description="CodeVision Phase Orchestrator")
    parser.add_argument("--sprint", type=int, help="Start from sprint N")
    parser.add_argument("--resume", action="store_true", help="Resume from last state")
    parser.add_argument("--dry-run", action="store_true", help="Plan only, no execution")
    parser.add_argument("--status", action="store_true", help="Show current state")
    parser.add_argument("--complete", type=int, help="Mark sprint N as completed")

    args = parser.parse_args()
    state = load_state()

    if args.status:
        show_status(state)
        return

    if args.complete is not None:
        complete_sprint(state, args.complete)
        return

    if args.sprint is not None:
        start_sprint(state, args.sprint)
        if args.dry_run:
            print("  [DRY RUN] Would dispatch agents and run pipeline")
            return
        return

    if args.resume:
        show_status(state)
        if state["status"] == "IN_PROGRESS":
            print(f"\nResuming sprint {state['current_sprint']}...")
        return

    # Default: show status
    show_status(state)


if __name__ == "__main__":
    main()
