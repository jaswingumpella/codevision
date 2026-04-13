CREATE TABLE kg_node (
    id VARCHAR(512) NOT NULL,
    project_id BIGINT NOT NULL,
    type VARCHAR(64) NOT NULL,
    name VARCHAR(512) NOT NULL,
    qualified_name VARCHAR(1024),
    metadata JSONB,
    provenance JSONB,
    confidence VARCHAR(32),
    artifact_id VARCHAR(256),
    origin VARCHAR(32),
    PRIMARY KEY (id, project_id)
);

CREATE TABLE kg_edge (
    id VARCHAR(512) NOT NULL,
    project_id BIGINT NOT NULL,
    source_node_id VARCHAR(512) NOT NULL,
    target_node_id VARCHAR(512) NOT NULL,
    type VARCHAR(64) NOT NULL,
    label VARCHAR(512),
    confidence VARCHAR(32),
    provenance JSONB,
    properties JSONB,
    PRIMARY KEY (id, project_id)
);

CREATE INDEX idx_kg_node_project ON kg_node(project_id);
CREATE INDEX idx_kg_node_type ON kg_node(project_id, type);
CREATE INDEX idx_kg_edge_project ON kg_edge(project_id);
CREATE INDEX idx_kg_edge_source ON kg_edge(project_id, source_node_id);
CREATE INDEX idx_kg_edge_target ON kg_edge(project_id, target_node_id);
CREATE INDEX idx_kg_edge_type ON kg_edge(project_id, type);
