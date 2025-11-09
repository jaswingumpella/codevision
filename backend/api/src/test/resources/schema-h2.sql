DROP TABLE IF EXISTS log_statement;
DROP TABLE IF EXISTS diagram;
DROP TABLE IF EXISTS dao_operation;
DROP TABLE IF EXISTS db_entity;
DROP TABLE IF EXISTS asset_image;
DROP TABLE IF EXISTS api_endpoint;
DROP TABLE IF EXISTS pii_pci_finding;
DROP TABLE IF EXISTS class_metadata;
DROP TABLE IF EXISTS project_snapshot;
DROP TABLE IF EXISTS analysis_job;
DROP TABLE IF EXISTS project CASCADE;

CREATE TABLE project (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    repo_url VARCHAR(255) NOT NULL,
    branch_name VARCHAR(255) NOT NULL,
    project_name VARCHAR(255) NOT NULL,
    last_analyzed_at TIMESTAMP NOT NULL,
    build_group_id VARCHAR(255),
    build_artifact_id VARCHAR(255),
    build_version VARCHAR(255),
    build_java_version VARCHAR(255),
    CONSTRAINT uq_project_repo_branch UNIQUE (repo_url, branch_name)
);

CREATE TABLE analysis_job (
    id UUID PRIMARY KEY,
    repo_url VARCHAR(2048) NOT NULL,
    branch_name VARCHAR(255) NOT NULL,
    status VARCHAR(32) NOT NULL,
    status_message VARCHAR(512),
    project_id BIGINT,
    error_message VARCHAR(1024),
    commit_hash VARCHAR(96),
    snapshot_id BIGINT,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    started_at TIMESTAMP,
    completed_at TIMESTAMP,
    CONSTRAINT fk_analysis_job_project FOREIGN KEY (project_id)
        REFERENCES project (id) ON DELETE SET NULL
);

CREATE TABLE project_snapshot (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    project_id BIGINT NOT NULL,
    project_name VARCHAR(255) NOT NULL,
    repo_url VARCHAR(255) NOT NULL,
    branch_name VARCHAR(255) NOT NULL,
    commit_hash VARCHAR(96),
    module_fingerprints_json CLOB,
    snapshot_json CLOB NOT NULL,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_project_snapshot_project FOREIGN KEY (project_id)
        REFERENCES project (id) ON DELETE CASCADE
);

CREATE TABLE class_metadata (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    project_id BIGINT NOT NULL,
    fully_qualified_name VARCHAR(255) NOT NULL,
    package_name VARCHAR(255),
    class_name VARCHAR(255) NOT NULL,
    stereotype VARCHAR(255) NOT NULL,
    source_set VARCHAR(64) NOT NULL,
    relative_path VARCHAR(255),
    user_code BOOLEAN NOT NULL,
    annotations_json CLOB,
    interfaces_json CLOB,
    CONSTRAINT fk_class_metadata_project FOREIGN KEY (project_id)
        REFERENCES project (id) ON DELETE CASCADE
);

CREATE TABLE api_endpoint (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    project_id BIGINT NOT NULL,
    protocol VARCHAR(64) NOT NULL,
    http_method VARCHAR(32),
    path_or_operation VARCHAR(512) NOT NULL,
    controller_class VARCHAR(512) NOT NULL,
    controller_method VARCHAR(512),
    spec_artifacts_json CLOB,
    CONSTRAINT fk_api_endpoint_project FOREIGN KEY (project_id)
        REFERENCES project (id) ON DELETE CASCADE
);

CREATE TABLE asset_image (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    project_id BIGINT NOT NULL,
    file_name VARCHAR(512) NOT NULL,
    relative_path VARCHAR(1024) NOT NULL,
    size_bytes BIGINT NOT NULL,
    sha256 VARCHAR(64),
    CONSTRAINT fk_asset_image_project FOREIGN KEY (project_id)
        REFERENCES project (id) ON DELETE CASCADE
);

CREATE TABLE db_entity (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    project_id BIGINT NOT NULL,
    entity_name VARCHAR(256) NOT NULL,
    fully_qualified_name VARCHAR(512),
    table_name VARCHAR(256),
    primary_keys_json CLOB,
    fields_json CLOB,
    relationships_json CLOB,
    CONSTRAINT fk_db_entity_project FOREIGN KEY (project_id)
        REFERENCES project (id) ON DELETE CASCADE
);

CREATE TABLE dao_operation (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    project_id BIGINT NOT NULL,
    repository_class VARCHAR(512) NOT NULL,
    method_name VARCHAR(255) NOT NULL,
    operation_type VARCHAR(64) NOT NULL,
    target_descriptor VARCHAR(512),
    query_snippet CLOB,
    CONSTRAINT fk_dao_operation_project FOREIGN KEY (project_id)
        REFERENCES project (id) ON DELETE CASCADE
);

CREATE TABLE diagram (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    project_id BIGINT NOT NULL,
    diagram_type VARCHAR(32) NOT NULL,
    title VARCHAR(256) NOT NULL,
    sequence_order INT,
    plantuml_source CLOB,
    mermaid_source CLOB,
    svg_path VARCHAR(1024),
    metadata_json CLOB,
    CONSTRAINT fk_diagram_project FOREIGN KEY (project_id)
        REFERENCES project (id) ON DELETE CASCADE
);

CREATE TABLE log_statement (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    project_id BIGINT NOT NULL,
    class_name VARCHAR(512) NOT NULL,
    file_path VARCHAR(1024),
    log_level VARCHAR(32) NOT NULL,
    line_number INT,
    message_template VARCHAR(2000),
    variables_json CLOB,
    pii_risk BOOLEAN NOT NULL,
    pci_risk BOOLEAN NOT NULL,
    CONSTRAINT fk_log_statement_project FOREIGN KEY (project_id)
        REFERENCES project (id) ON DELETE CASCADE
);

CREATE TABLE pii_pci_finding (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    project_id BIGINT NOT NULL,
    file_path VARCHAR(1024) NOT NULL,
    line_number INT,
    snippet CLOB,
    match_type VARCHAR(32) NOT NULL,
    severity VARCHAR(16) NOT NULL,
    ignored BOOLEAN NOT NULL,
    CONSTRAINT fk_pii_pci_project FOREIGN KEY (project_id)
        REFERENCES project (id) ON DELETE CASCADE
);
