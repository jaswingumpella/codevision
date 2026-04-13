package com.codevision.codevisionbackend.graph;

/**
 * Comprehensive enumeration of every node type representable in the CodeVision
 * knowledge graph. Each constant maps to a distinct semantic category of code
 * or infrastructure artefact discovered during analysis.
 */
public enum KgNodeType {

    // ── Structural / organisational ──────────────────────────────────────
    REPOSITORY,
    MODULE,
    PACKAGE,
    FILE,
    NAMESPACE,

    // ── Type declarations ────────────────────────────────────────────────
    CLASS,
    INTERFACE,
    ENUM,
    RECORD,
    STRUCT,
    TRAIT,
    PROTOCOL,
    ANNOTATION_TYPE,
    TYPE_ALIAS,
    UNION,

    // ── Members ──────────────────────────────────────────────────────────
    METHOD,
    CONSTRUCTOR,
    FIELD,
    PROPERTY,
    CONSTANT,
    PARAMETER,
    TYPE_PARAMETER,
    LAMBDA,
    CLOSURE,
    FUNCTION,
    EXTENSION_FUNCTION,

    // ── Infrastructure & runtime ─────────────────────────────────────────
    ENDPOINT,
    DATABASE_ENTITY,
    DATABASE_COLUMN,
    CONFIG_KEY,
    BUILD_TARGET,
    DEPENDENCY_ARTIFACT,

    // ── Testing ──────────────────────────────────────────────────────────
    TEST_CASE,
    TEST_SUITE,

    // ── Messaging ────────────────────────────────────────────────────────
    EVENT,
    QUEUE
}
