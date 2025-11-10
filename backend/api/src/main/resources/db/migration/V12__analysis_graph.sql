CREATE TABLE IF NOT EXISTS compiled_analysis_run (
    id UUID PRIMARY KEY,
    repo_path TEXT NOT NULL,
    project_id BIGINT,
    started_at TIMESTAMPTZ,
    completed_at TIMESTAMPTZ,
    status VARCHAR(32) NOT NULL,
    status_message TEXT,
    output_dir TEXT,
    entity_count BIGINT,
    endpoint_count BIGINT,
    dependency_count BIGINT,
    sequence_count BIGINT,
    duration_ms BIGINT,
    classpath TEXT,
    accept_packages TEXT,
    CONSTRAINT fk_compiled_run_project FOREIGN KEY (project_id) REFERENCES project(id) ON DELETE SET NULL
);

CREATE TABLE IF NOT EXISTS entity (
    id BIGSERIAL PRIMARY KEY,
    class_name TEXT UNIQUE NOT NULL,
    package_name TEXT NOT NULL,
    jar_or_dir TEXT,
    table_name TEXT,
    origin TEXT NOT NULL,
    scc_id BIGINT,
    in_cycle BOOLEAN DEFAULT FALSE
);

CREATE TABLE IF NOT EXISTS entity_field (
    entity_id BIGINT REFERENCES entity(id) ON DELETE CASCADE,
    name TEXT NOT NULL,
    type TEXT,
    is_join BOOLEAN NOT NULL DEFAULT FALSE,
    PRIMARY KEY (entity_id, name)
);

CREATE TABLE IF NOT EXISTS sequence (
    id BIGSERIAL PRIMARY KEY,
    generator_name TEXT UNIQUE NOT NULL,
    sequence_name  TEXT,
    allocation_size INT DEFAULT 50,
    initial_value  INT DEFAULT 1
);

CREATE TABLE IF NOT EXISTS entity_uses_sequence (
    entity_id BIGINT REFERENCES entity(id) ON DELETE CASCADE,
    field_name TEXT NOT NULL,
    sequence_id BIGINT REFERENCES sequence(id) ON DELETE CASCADE,
    PRIMARY KEY (entity_id, field_name)
);

CREATE TABLE IF NOT EXISTS class_dep (
    caller TEXT NOT NULL,
    callee TEXT NOT NULL,
    package_filter TEXT NOT NULL,
    PRIMARY KEY (caller, callee)
);

CREATE TABLE IF NOT EXISTS compiled_endpoint (
    id BIGSERIAL PRIMARY KEY,
    endpoint_type TEXT NOT NULL,
    http_method TEXT,
    path TEXT,
    controller_class TEXT,
    controller_method TEXT,
    framework TEXT
);

CREATE INDEX IF NOT EXISTS idx_entity_pkg ON entity (package_name);
CREATE INDEX IF NOT EXISTS idx_entity_table ON entity (table_name);
