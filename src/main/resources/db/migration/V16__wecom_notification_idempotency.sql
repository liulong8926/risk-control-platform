ALTER TABLE er_wecom_notification_log ADD COLUMN idempotency_key VARCHAR(100) NULL;

CREATE UNIQUE INDEX uq_er_wecom_notification_idempotency
  ON er_wecom_notification_log(idempotency_key);
