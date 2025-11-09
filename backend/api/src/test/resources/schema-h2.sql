DROP TABLE IF EXISTS class_metadata;
DROP TABLE IF EXISTS project_snapshot;
DROP TABLE IF EXISTS analysis_job;
DROP TABLE IF EXISTS project CASCADE;

CREATE TABLE project (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    repo_url VARCHAR(255) NOT NULL,
    project_name VARCHAR(255) NOT NULL,
    last_analyzed_at TIMESTAMP NOT NULL,
    build_group_id VARCHAR(255),
    build_artifact_id VARCHAR(255),
    build_version VARCHAR(255),
    build_java_version VARCHAR(255)
);

CREATE TABLE analysis_job (
    id UUID PRIMARY KEY,
    repo_url VARCHAR(2048) NOT NULL,
    status VARCHAR(32) NOT NULL,
    status_message VARCHAR(512),
    project_id BIGINT,
    error_message VARCHAR(1024),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    started_at TIMESTAMP,
    completed_at TIMESTAMP,
    CONSTRAINT fk_analysis_job_project FOREIGN KEY (project_id)
        REFERENCES project (id) ON DELETE SET NULL
);

CREATE TABLE project_snapshot (
    project_id BIGINT PRIMARY KEY,
    project_name VARCHAR(255) NOT NULL,
    repo_url VARCHAR(255) NOT NULL,
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
