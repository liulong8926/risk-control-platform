ALTER TABLE er_enterprise_profile ADD COLUMN highest_notified_risk_level VARCHAR(32) NULL;

CREATE TABLE er_batch_risk_change (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  run_id BIGINT NOT NULL,
  profile_id BIGINT NOT NULL,
  previous_risk_level VARCHAR(32) NULL,
  current_risk_level VARCHAR(32) NULL,
  risk_status VARCHAR(24) NOT NULL,
  notification_eligible BOOLEAN NOT NULL DEFAULT FALSE,
  notified_at TIMESTAMP NULL,
  created_at TIMESTAMP NOT NULL,
  updated_at TIMESTAMP NOT NULL,
  CONSTRAINT uq_er_batch_risk_change UNIQUE(run_id, profile_id),
  CONSTRAINT fk_er_batch_risk_change_run FOREIGN KEY(run_id) REFERENCES er_collection_run(id),
  CONSTRAINT fk_er_batch_risk_change_profile FOREIGN KEY(profile_id) REFERENCES er_enterprise_profile(id)
);

CREATE INDEX idx_er_batch_risk_change_run_status ON er_batch_risk_change(run_id, risk_status);
CREATE INDEX idx_er_batch_risk_change_profile ON er_batch_risk_change(profile_id);
