CREATE TABLE er_role (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  role_code VARCHAR(64) NOT NULL UNIQUE,
  role_name VARCHAR(100) NOT NULL,
  enabled BOOLEAN NOT NULL DEFAULT TRUE,
  created_at TIMESTAMP NOT NULL,
  updated_at TIMESTAMP NOT NULL
);
CREATE TABLE er_role_permission (
  role_id BIGINT NOT NULL,
  permission_code VARCHAR(100) NOT NULL,
  PRIMARY KEY(role_id, permission_code),
  CONSTRAINT fk_role_permission_role FOREIGN KEY(role_id) REFERENCES er_role(id) ON DELETE CASCADE
);
INSERT INTO er_role(role_code,role_name,enabled,created_at,updated_at)
VALUES ('ADMIN','系统管理员',TRUE,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP),
       ('OPERATIONS','运营人员',TRUE,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP),
       ('VIEWER','只读账号',TRUE,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP);
INSERT INTO er_role_permission(role_id,permission_code)
SELECT id,'*' FROM er_role WHERE role_code='ADMIN';
INSERT INTO er_role_permission(role_id,permission_code)
SELECT r.id,p.permission_code FROM er_role r CROSS JOIN (SELECT 'RISK_VIEW' permission_code UNION ALL SELECT 'RISK_WRITE' UNION ALL SELECT 'MODEL_VIEW' UNION ALL SELECT 'ROBOT_VIEW') p WHERE r.role_code='OPERATIONS';
INSERT INTO er_role_permission(role_id,permission_code)
SELECT id,'RISK_VIEW' FROM er_role WHERE role_code='VIEWER';
