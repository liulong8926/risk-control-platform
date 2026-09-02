CREATE INDEX idx_attempt_profile ON er_collection_attempt(profile_id);
ALTER TABLE er_collection_attempt DROP INDEX uq_active_attempt;
