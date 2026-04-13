package com.codevision.codevisionbackend.graph;

/**
 * Comprehensive enumeration of every directed edge type in the CodeVision
 * knowledge graph. Edge types are grouped by semantic category to aid
 * readability and maintenance.
 */
public enum KgEdgeType {

    // ── Containment / declaration ────────────────────────────────────────
    CONTAINS,
    DECLARES,
    MEMBER_OF,

    // ── Inheritance / type hierarchy ─────────────────────────────────────
    EXTENDS,
    IMPLEMENTS,
    MIXES_IN,
    REALIZES,

    // ── Module system ────────────────────────────────────────────────────
    IMPORTS,
    EXPORTS,

    // ── Usage / reference ────────────────────────────────────────────────
    USES,
    REFERENCES,

    // ── Invocation ───────────────────────────────────────────────────────
    CALLS,
    CALLED_BY,
    OVERRIDES,
    OVERLOADS,
    CONSTRUCTS,
    INSTANTIATES,

    // ── Field access ─────────────────────────────────────────────────────
    READS_FIELD,
    WRITES_FIELD,

    // ── Signature ────────────────────────────────────────────────────────
    RETURNS_TYPE,
    ACCEPTS_PARAMETER,
    THROWS,
    CATCHES,

    // ── Dependency injection / CDI ───────────────────────────────────────
    INJECTS,
    PROVIDES,
    PRODUCES,
    QUALIFIES,

    // ── Annotation / decoration ──────────────────────────────────────────
    ANNOTATES,
    DECORATES,

    // ── OOP relationships ────────────────────────────────────────────────
    INHERITS_FROM,
    DELEGATES_TO,

    // ── Messaging / eventing ─────────────────────────────────────────────
    PUBLISHES,
    SUBSCRIBES,
    LISTENS_TO,

    // ── Persistence / data ───────────────────────────────────────────────
    MAPS_TO_TABLE,
    MAPS_TO_COLUMN,
    QUERIES,
    PERSISTS,
    JOINS,
    RELATES_TO,

    // ── Testing ──────────────────────────────────────────────────────────
    TESTS,
    MOCKS,
    STUBS,
    ASSERTS_ON,

    // ── Configuration / build ────────────────────────────────────────────
    CONFIGURES,
    PROFILES,
    ENABLES,
    DEPENDS_ON,
    COMPILES_AGAINST,
    LINKS_TO,

    // ── Generics / type system ───────────────────────────────────────────
    PARAMETERIZES,
    BOUNDS,
    WIDENS_TO,
    NARROWS_TO,

    // ── Lifecycle ────────────────────────────────────────────────────────
    INITIALIZES,
    DESTROYS,
    SCHEDULES,

    // ── Access control ───────────────────────────────────────────────────
    EXPOSES,
    RESTRICTS
}
