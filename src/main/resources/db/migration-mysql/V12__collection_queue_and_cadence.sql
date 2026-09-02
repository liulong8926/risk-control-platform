ALTER TABLE er_enterprise_profile ADD COLUMN next_collection_at TIMESTAMP NULL;
ALTER TABLE er_enterprise_profile ADD COLUMN collection_interval_days INT NOT NULL DEFAULT 15;
CREATE INDEX idx_er_profile_next_collection ON er_enterprise_profile(next_collection_at);
