-- Adds storage for serialized spec artifact references captured during analysis.
ALTER TABLE IF EXISTS api_endpoint
    ADD COLUMN IF NOT EXISTS spec_artifacts_json TEXT;
