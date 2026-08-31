INSERT INTO er_risk_model_factor(model_type,factor_code,factor_name,weight,enabled,version_no,updated_at)
SELECT t.model_type, t.factor_code, t.factor_name, 0, TRUE, 1, CURRENT_TIMESTAMP
FROM (VALUES
  ('GOVERNMENT','LIMIT_CONSUMPTION','限制消费令'),('GOVERNMENT','CASE_FILING','立案信息'),
  ('ENTERPRISE','LIMIT_CONSUMPTION','限制消费令'),('ENTERPRISE','CASE_FILING','立案信息'),
  ('OTHER','LIMIT_CONSUMPTION','限制消费令'),('OTHER','CASE_FILING','立案信息')
) t(model_type,factor_code,factor_name)
WHERE NOT EXISTS (SELECT 1 FROM er_risk_model_factor f WHERE f.model_type=t.model_type AND f.factor_code=t.factor_code);
