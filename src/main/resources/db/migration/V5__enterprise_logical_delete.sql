ALTER TABLE er_enterprise_profile ADD COLUMN deleted BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE er_enterprise_profile ADD COLUMN deleted_at TIMESTAMP NULL;
ALTER TABLE er_enterprise_profile ADD COLUMN deleted_by VARCHAR(100) NULL;
CREATE INDEX idx_er_profile_deleted ON er_enterprise_profile(deleted);
