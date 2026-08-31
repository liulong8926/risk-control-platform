INSERT INTO er_role_permission(role_id,permission_code)
SELECT r.id,p.permission_code FROM er_role r CROSS JOIN (VALUES
 ('MENU_RISK'),('MENU_MODEL'),('MENU_KEY'),('MENU_ROBOT'),('MENU_ACCOUNTS'),('MENU_ROLES'),
 ('RISK_VIEW'),('RISK_WRITE'),('MODEL_VIEW'),('MODEL_MANAGE'),('KEY_VIEW'),('KEY_MANAGE'),
 ('ROBOT_VIEW'),('ROBOT_MANAGE'),('ACCOUNT_MANAGE'),('ROLE_MANAGE')) p(permission_code)
WHERE r.role_code='ADMIN' AND NOT EXISTS (SELECT 1 FROM er_role_permission x WHERE x.role_id=r.id AND x.permission_code=p.permission_code);
INSERT INTO er_role_permission(role_id,permission_code)
SELECT r.id,p.permission_code FROM er_role r CROSS JOIN (VALUES ('MENU_RISK'),('MENU_MODEL'),('MENU_ROBOT'),('RISK_VIEW'),('RISK_WRITE'),('MODEL_VIEW'),('ROBOT_VIEW')) p(permission_code)
WHERE r.role_code='OPERATIONS' AND NOT EXISTS (SELECT 1 FROM er_role_permission x WHERE x.role_id=r.id AND x.permission_code=p.permission_code);
INSERT INTO er_role_permission(role_id,permission_code)
SELECT r.id,'MENU_RISK' FROM er_role r WHERE r.role_code='VIEWER' AND NOT EXISTS (SELECT 1 FROM er_role_permission x WHERE x.role_id=r.id AND x.permission_code='MENU_RISK');
