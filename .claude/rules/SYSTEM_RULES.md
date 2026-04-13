# System Rules

These rules are non-negotiable and apply to ALL agents and ALL sprints.

## 1. Read Before Write
MUST read a file before modifying it. No blind edits.

## 2. Tests Before Implementation (TDD)
Write the failing test FIRST (RED), then implement to make it pass (GREEN), then refactor.
The test file should exist BEFORE the implementation file.

## 3. No Depth Limits
NEVER add hardcoded depth or size limits. Use:
- Cycle detection via visited-set for graph traversals
- Time-based deadline (Instant) for long-running operations
- Configuration-driven limits with unlimited defaults

## 4. Backward Compatibility
Use the Adapter pattern (GraphModelAdapter) to maintain compatibility with existing code.
Never break existing REST API contracts without versioning.

## 5. Configuration-Driven
Every tunable value MUST be in application.yml.
Use @ConfigurationProperties for type-safe config.
Default to unlimited (-1) for all limits.

## 6. Scope Discipline
Do ONLY what was asked. No gold-plating, no "while I'm here" refactors.
One sprint, one set of deliverables.

## 7. Checkpoint Discipline
Write checkpoints to ACTIVE.md every 10 significant tool calls.
Include: files modified, decisions made, blockers encountered.

## 8. Source of Truth Hierarchy
1. CLAUDE.md (project context)
2. SYSTEM_RULES.md (these rules)
3. ARCHITECTURE_RULES.md (structural constraints)
4. TESTING_RULES.md (test conventions)
5. Plan file (sprint details)
6. Agent .md files (role-specific guidance)

## 9. Anti-Hallucination
ALWAYS verify files/functions exist before referencing them.
NEVER invent file paths or class names without checking.
Use Glob/Grep to confirm before modifying.

## 10. Minimal Changes
Make the smallest change that accomplishes the goal.
Don't refactor surrounding code unless it's part of the sprint deliverable.

## 11. Java 21 Idioms
- Records for value types
- Sealed interfaces for closed hierarchies
- Pattern matching where applicable
- var for local variables with clear types

## 12. No Secrets
Never commit credentials, API keys, or tokens.
Use environment variables for all sensitive configuration.
