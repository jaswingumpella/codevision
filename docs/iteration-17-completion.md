# Iteration 17 Completion – Postgres Persistence & API

## Summary

Iteration 17 closed the loop by persisting compiled-analysis metadata, exposing REST APIs, and wiring the React experience so analysts can run bytecode analyses, browse summaries, and download exports directly from the UI.

## Key Deliverables

- New tables: `compiled_analysis_run`, `entity`, `entity_field`, `sequence`, `entity_uses_sequence`, `class_dep`, `compiled_endpoint`.
- `PersistService` batch-upserts entities, fields, sequences, sequence usage, dependencies, and endpoints after each run.
- `AnalysisController` endpoints: `POST /api/analyze`, `GET /api/analyze/{id}/exports`, `GET /api/entities`, `/api/sequences`, `/api/endpoints`, plus export download streams.
- React “Compiled Analysis” tab with run controls, export list, sample tables, and inline Mermaid ERD source.

## Verification

- Manual runs confirmed database tables populate correctly and exports download through the new endpoints.
- UI smoke tests verified analysts can trigger analyses, inspect metadata, and retrieve artifacts without leaving the browser.
