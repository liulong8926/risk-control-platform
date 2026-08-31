package com.tehang.enterpriserisk;

import static org.junit.jupiter.api.Assertions.*;
import db.migration.V14__remove_limit_consumption;
import com.tehang.enterpriserisk.api.RiskController.Factor;
import com.tehang.enterpriserisk.service.KeyService;
import com.tehang.enterpriserisk.service.BatchCollectionService;
import com.tehang.enterpriserisk.service.RiskService;
import java.sql.Connection;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.flywaydb.core.api.migration.Context;

@SpringBootTest
class RiskServiceTest {
  @Autowired RiskService risk;
  @Autowired KeyService keys;
  @Autowired JdbcTemplate db;
  @Autowired BatchCollectionService batches;

  @Test void modelRequiresHundredPercent() {
    assertThrows(IllegalArgumentException.class, () -> risk.saveModel("ENTERPRISE", List.of(new Factor("RISK_OVERVIEW", 50, true))));
  }

  @Test void limitConsumptionIsNotACollectionCapabilityOrRiskFactor() {
    assertEquals(11, RiskService.CAPS.size());
    assertFalse(RiskService.CAPS.contains("LIMIT_CONSUMPTION"));
    var factors = risk.models().stream().filter(x -> "ENTERPRISE".equals(x.get("modelType"))).toList();
    assertEquals(5, factors.size());
    assertTrue(factors.stream().noneMatch(x -> "LIMIT_CONSUMPTION".equals(x.get("factorCode"))));
    var configuration = factors.stream().map(x -> new Factor(
        String.valueOf(x.get("factorCode")), String.valueOf(x.get("riskLevel")), Boolean.TRUE.equals(x.get("enabled")))).toList();
    assertDoesNotThrow(() -> risk.saveModel("ENTERPRISE", configuration));
  }

  @Test void removalMigrationPurgesLegacyDataAndRecalculatesRiskLevel() throws Exception {
    String code = "91310000TEST000001";
    long profileId = ((Number) risk.upsert(code, "迁移测试企业", "ENTERPRISE", true, "TEST", "tester").get("id")).longValue();
    long attemptId = -1;
    try {
      Instant now = Instant.now();
      db.update("insert into er_collection_attempt(profile_id,status,attempt_no,started_at,created_at) values(?, 'SUCCEEDED', 1, ?, ?)", profileId, now, now);
      attemptId = db.queryForObject("select max(id) from er_collection_attempt where profile_id=?", Long.class, profileId);
      db.update("insert into er_capability_result(attempt_id,capability_code,status,hit,record_count,collected_at) values(?, 'LIMIT_CONSUMPTION', 'SUCCEEDED', true, 1, ?)", attemptId, now);
      db.update("insert into er_risk_model_factor(model_type,factor_code,factor_name,weight,enabled,version_no,updated_at,risk_level) values('ENTERPRISE','LIMIT_CONSUMPTION','限制消费令',0,true,1,?,'CRITICAL')", now);
      String evidence = "{\"factors\":[{\"factorCode\":\"LIMIT_CONSUMPTION\",\"riskLevel\":\"CRITICAL\",\"configuredRiskLevel\":\"CRITICAL\",\"result\":\"TRIGGERED\"},{\"factorCode\":\"JUDICIAL_CASE\",\"riskLevel\":\"HIGH\",\"configuredRiskLevel\":\"HIGH\",\"result\":\"TRIGGERED\"}],\"hitFactorCount\":2,\"provisional\":false}";
      db.update("insert into er_risk_score(profile_id,model_type,model_version,risk_level,evidence_json,scored_at) values(?, 'ENTERPRISE', 1, 'CRITICAL*1', ?, ?)", profileId, evidence, now);
      try (Connection connection = db.getDataSource().getConnection()) {
        new V14__remove_limit_consumption().migrate(new Context() {
          @Override public org.flywaydb.core.api.configuration.Configuration getConfiguration() { return null; }
          @Override public Connection getConnection() { return connection; }
        });
      }
      assertEquals(0, db.queryForObject("select count(*) from er_capability_result where attempt_id=? and capability_code='LIMIT_CONSUMPTION'", Integer.class, attemptId));
      assertEquals(0, db.queryForObject("select count(*) from er_risk_model_factor where factor_code='LIMIT_CONSUMPTION'", Integer.class));
      assertEquals("HIGH*1", db.queryForObject("select risk_level from er_enterprise_profile where id=?", String.class, profileId));
      String migrated = db.queryForObject("select evidence_json from er_risk_score where profile_id=?", String.class, profileId);
      assertFalse(migrated.contains("LIMIT_CONSUMPTION"));
      assertTrue(migrated.contains("\"hitFactorCount\":1"));
    } finally {
      db.update("delete from er_risk_score where profile_id=?", profileId);
      if (attemptId != -1) db.update("delete from er_capability_result where attempt_id=?", attemptId);
      db.update("delete from er_collection_attempt where profile_id=?", profileId);
      db.update("delete from er_enterprise_profile where id=?", profileId);
    }
  }

  @Test void keyIsMaskedAndCanBeRotated() {
    var created = keys.save(null, "test", "abcdefgh1234", 10);
    assertEquals("abc****1234", created.get("maskedKey"));
    assertFalse(created.containsValue("abcdefgh1234"));
    keys.delete(((Number) created.get("id")).longValue());
  }

  @Test void eachAllocationConsumesOneDailyQuota() {
    var created = keys.save(null, "quota-test", "abcdefgh5678", 2);
    long id = ((Number) created.get("id")).longValue();
    try {
      keys.nextAllocation();
      keys.nextAllocation();
      var row = db.queryForMap("select usage_count from er_tianyancha_key where id=?", id);
      assertEquals(2, ((Number) row.get("usage_count")).intValue());
      assertThrows(IllegalStateException.class, () -> keys.nextAllocation());
    } finally {
      keys.delete(id);
    }
  }

  @Test void partialHitsUseHighestRiskLevel() {
    assertEquals("CRITICAL*1", RiskService.riskDisplay(new int[]{1, 0, 1, 1}, true, true));
  }

  @Test void partialWithoutHitsButUnavailableIsInsufficientData() {
    assertEquals("INSUFFICIENT_DATA", RiskService.riskDisplay(new int[]{0, 0, 0, 0}, true, true));
  }

  @Test void completeWithoutHitsRemainsNoRisk() {
    assertEquals("NONE", RiskService.riskDisplay(new int[]{0, 0, 0, 0}, false, false));
  }

  @Test void weeklyScheduleOnlyRunsOnConfiguredDayAt2300() {
    var schedule = Map.<String,Object>of("enabled", true, "frequency", "WEEKLY", "dayOfWeek", 1);
    assertTrue(BatchCollectionService.scheduleDue(schedule, LocalDateTime.of(2026, 8, 31, 23, 0)));
    assertFalse(BatchCollectionService.scheduleDue(schedule, LocalDateTime.of(2026, 9, 1, 23, 0)));
    assertFalse(BatchCollectionService.scheduleDue(schedule, LocalDateTime.of(2026, 8, 31, 22, 59)));
  }

  @Test void saveSchedulePersistsFrequencyAndFixedTime() {
    batches.saveSchedule(true, "WEEKLY", "02:00", 3, "test");
    var weekly = db.queryForMap("select enabled,frequency,run_time,day_of_week from er_collection_schedule where id=1");
    assertEquals("WEEKLY", weekly.get("FREQUENCY"));
    assertEquals("23:00", weekly.get("RUN_TIME"));
    assertEquals(3, ((Number) weekly.get("DAY_OF_WEEK")).intValue());
    batches.saveSchedule(true, "DAILY", "08:00", null, "test");
    var daily = db.queryForMap("select frequency,run_time,day_of_week from er_collection_schedule where id=1");
    assertEquals("DAILY", daily.get("FREQUENCY"));
    assertEquals("23:00", daily.get("RUN_TIME"));
    assertNull(daily.get("DAY_OF_WEEK"));
  }
}
