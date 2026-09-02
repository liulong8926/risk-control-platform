ALTER TABLE er_risk_model_factor ADD COLUMN risk_level VARCHAR(16);
UPDATE er_risk_model_factor SET risk_level=CASE WHEN weight>=80 THEN 'CRITICAL' WHEN weight>=50 THEN 'HIGH' WHEN weight>=20 THEN 'MEDIUM' ELSE 'LOW' END WHERE risk_level IS NULL;
UPDATE er_risk_model_factor SET risk_level='LOW' WHERE risk_level IS NULL;
