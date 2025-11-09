# Iteration 11 – Snapshot History & Security Enhancements

## Scope
- Preserve every analysis run as a versioned snapshot tied to branch + commit.
- Add APIs + UI for listing snapshots and diffing any two versions.
- Speed up subsequent analyses by fingerprinting Maven modules and re-parsing only the directories that changed between commits.
- Polish the Logger Insights and PCI/PII experiences: richer filters, global expand/collapse, ignore/restore UX, and more flexible rule configuration.

## Backend Deliverables
- **Branch-aware analysis:** `/analyze` accepts an optional `branchName` and persists it on projects, jobs, and snapshots so teams can keep parallel histories per branch.
- **Snapshot history:** Replaced the single-row `project_snapshot` with an append-only table (`snapshot_id`, branch, commit hash, module fingerprints). Added `GET /project/{id}/snapshots` and `GET /project/{id}/snapshots/{snapshotId}/diff/{compareSnapshotId}` to surface the history and per-field diffs (classes, endpoints, DB entities).
- **Incremental analysis:** After cloning a repo we fingerprint each Maven module (via the JGit tree IDs). If the commit matches the latest snapshot we reuse it immediately. Otherwise we only re-run expensive scanners (Java, logger, PCI/PII) for the modules whose fingerprints changed.
- **Security workflows:** The PCI/PII inspector now merges inline rules and optional rule files and exposes `PATCH /project/{id}/pii-pci/{findingId}` so false positives can be marked ignored/restored without redeploying.

## Frontend Deliverables
- **Snapshot panel:** New tab with a timeline table, baseline/compare selectors, refresh button, and a diff summary that mirrors the new backend API. The analyze form gained a Branch input to keep branch context visible.
- **Logger Insights UX:** Added message-level search, “Expand All / Collapse All” controls, and fixed the default class filter so it no longer inherits the example OrderService text. Export buttons continue to work for large datasets.
- **PCI/PII UX:** Hide ignored findings by default, provide Ignore/Restore actions per row, and call the new patch endpoint to keep state in sync. CSV/PDF exports still have one-click buttons.
- **Export preview:** The iframe now uses lazy loading and height constraints so very large HTML exports no longer cause scroll jank.
- **Tests & warnings:** `App.test.jsx` and `App.panels.test.jsx` now await every polling tick, silencing the prior `act(...)` console warning while keeping coverage over the async job workflow.

## Verification
- Backend: `SPRING_PROFILES_ACTIVE=test mvn -f backend/pom.xml -pl api -am test -Dskip.frontend=true`
- Frontend: `npm --prefix frontend run test`
- Vitest output is clean (no `act(...)` warnings), and both suites pass with the new branch-aware snapshot fixtures.
