INSERT INTO er_risk_model_factor(model_type,factor_code,factor_name,weight,enabled,version_no,updated_at)
SELECT t.model_type, t.factor_code, t.factor_name, 0, TRUE, 1, CURRENT_TIMESTAMP
FROM (
  SELECT 'GOVERNMENT' model_type,'LIMIT_CONSUMPTION' factor_code,'限制消费令' factor_name
  UNION ALL SELECT 'GOVERNMENT','CASE_FILING','立案信息'
  UNION ALL SELECT 'ENTERPRISE','LIMIT_CONSUMPTION','限制消费令'
  UNION ALL SELECT 'ENTERPRISE','CASE_FILING','立案信息'
  UNION ALL SELECT 'OTHER','LIMIT_CONSUMPTION','限制消费令'
  UNION ALL SELECT 'OTHER','CASE_FILING','立案信息'
) t
WHERE NOT EXISTS (SELECT 1 FROM er_risk_model_factor f WHERE f.model_type=t.model_type AND f.factor_code=t.factor_code);
