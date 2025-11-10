# Iteration 14 Completion – JPA ERD & Sequences + CSVs

## Summary

Iteration 14 taught the bytecode pipeline how to understand persistence metadata. We now map JPA annotations, relationships, and sequence generators so ERD exports have accurate structure/properties derived from the compiled artefacts.

## Key Deliverables

- Extended `BytecodeEntityScanner` to recognize `@Entity`, `@Table`, `@Id`, `@Join*`, `@SequenceGenerator`, `@GeneratedValue`, `@TableGenerator`, and Hibernate `@GenericGenerator`.
- Captured relationships/injections inside `GraphModel` fields and sequence usage via `SequenceUsage`.
- Emitted `entities.csv` and `sequences.csv` from the compiled graph with deterministic ordering.

## Verification

- Verified CSV exports on fixture projects containing `@SequenceGenerator` (class- and field-level) plus bi-directional relationships.
- Confirmed ER data merges cleanly with source metadata and feeds upcoming diagram generators.
