-- 自动化采集统一在 23:00 执行；历史周频率配置缺失执行日时默认星期一。
UPDATE er_collection_schedule
SET run_time = '23:00',
    day_of_week = CASE
      WHEN frequency = 'WEEKLY' AND (day_of_week IS NULL OR day_of_week < 1 OR day_of_week > 7) THEN 1
      WHEN frequency <> 'WEEKLY' THEN NULL
      ELSE day_of_week
    END
WHERE id = 1;
