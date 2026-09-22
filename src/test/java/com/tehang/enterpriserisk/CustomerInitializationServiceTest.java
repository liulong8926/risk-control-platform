package com.tehang.enterpriserisk;

import static org.junit.jupiter.api.Assertions.*;

import com.tehang.enterpriserisk.api.RiskController;
import com.tehang.enterpriserisk.service.CustomerInitializationService;
import com.tehang.enterpriserisk.service.RiskService;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.server.ResponseStatusException;

@SpringBootTest
class CustomerInitializationServiceTest {
  @Autowired CustomerInitializationService initialization;
  @Autowired RiskController controller;
  @Autowired RiskService risk;
  @Autowired JdbcTemplate db;

  @Test void initializationDeletesEnterpriseDataButKeepsRunAndNotificationHistory() {
    Fixture fixture = fixture("complete", "SUCCEEDED");
    try {
      Map<String,Object> preview = initialization.preview();
      assertTrue(number(preview, "enterpriseCount") >= 1);
      assertTrue(number(preview, "collectionAttemptCount") >= 1);
      assertTrue(number(preview, "capabilityResultCount") >= 1);
      assertTrue(number(preview, "riskScoreCount") >= 1);
      assertTrue(number(preview, "batchRiskChangeCount") >= 1);
      assertTrue(number(preview, "riskEventCount") >= 1);
      assertTrue(number(preview, "workOrderCount") >= 1);

      Map<String,Object> result = initialization.initialize("确认初始化", "admin-test");

      assertEquals(Boolean.TRUE, result.get("initialized"));
      assertEquals(0, count("select count(*) from er_enterprise_profile"));
      assertEquals(0, count("select count(*) from er_collection_attempt"));
      assertEquals(0, count("select count(*) from er_capability_result"));
      assertEquals(0, count("select count(*) from er_risk_score"));
      assertEquals(0, count("select count(*) from er_batch_risk_change"));
      assertEquals(0, count("select count(*) from er_risk_event"));
      assertEquals(0, count("select count(*) from er_work_order"));
      assertEquals(1, count("select count(*) from er_collection_run where id=" + fixture.runId));
      assertEquals(1, count("select count(*) from er_wecom_notification_log where run_id=" + fixture.runId));
      assertEquals(1, count("select count(*) from er_audit_log where action='CUSTOMER_INITIALIZE' and actor_account='admin-test'"));

      Map<String,Object> imported = risk.upsert(fixture.code, "重新导入企业", "ENTERPRISE", true, "RISK_IMPORT", "新销售", false);
      Object importedIdValue = imported.get("id") != null ? imported.get("id") : imported.get("ID");
      long importedId = ((Number) importedIdValue).longValue();
      assertEquals("UNSCANNED", db.queryForObject("select collection_status from er_enterprise_profile where id=?", String.class, importedId));
    } finally {
      initialization.initialize("确认初始化", "admin-test");
      cleanupHistory(fixture);
    }
  }

  @Test void invalidConfirmationDoesNotDeleteAnything() {
    Fixture fixture = fixture("confirm", "SUCCEEDED");
    try {
      assertThrows(IllegalArgumentException.class, () -> initialization.initialize("初始化", "admin-test"));
      assertEquals(1, count("select count(*) from er_enterprise_profile where id=" + fixture.profileId));
    } finally {
      cleanupAll(fixture);
    }
  }

  @Test void collectingRunBlocksInitializationWithoutDeletingData() {
    Fixture fixture = fixture("collecting", "COLLECTING");
    try {
      ResponseStatusException error = assertThrows(ResponseStatusException.class,
          () -> initialization.initialize("确认初始化", "admin-test"));
      assertEquals(HttpStatus.CONFLICT, error.getStatusCode());
      assertEquals(1, count("select count(*) from er_enterprise_profile where id=" + fixture.profileId));
    } finally {
      cleanupAll(fixture);
    }
  }

  @Test void initializationEndpointsRequireTheBuiltInAdminRole() {
    MockHttpServletRequest operations = new MockHttpServletRequest();
    operations.setAttribute("role", "OPERATIONS");
    ResponseStatusException denied = assertThrows(ResponseStatusException.class,
        () -> controller.initializationPreview(operations));
    assertEquals(HttpStatus.FORBIDDEN, denied.getStatusCode());

    MockHttpServletRequest custom = new MockHttpServletRequest();
    custom.setAttribute("role", "CUSTOM_ADMIN");
    assertThrows(ResponseStatusException.class, () -> controller.initializationPreview(custom));

    MockHttpServletRequest admin = new MockHttpServletRequest();
    admin.setAttribute("role", "ADMIN");
    assertDoesNotThrow(() -> controller.initializationPreview(admin));
  }

  private Fixture fixture(String prefix, String runStatus) {
    String suffix = Long.toUnsignedString(System.nanoTime());
    String code = (prefix + suffix).substring(0, Math.min(18, prefix.length() + suffix.length()));
    code = String.format("%-18s", code).replace(' ', '0');
    String batch = "INIT-" + prefix + "-" + suffix;
    Instant now = Instant.now();
    db.update("insert into er_enterprise_profile(company_name,unified_credit_code,subject_type,source,monitoring_enabled,collection_status,sales_manager,created_at,updated_at) values(?,?, 'ENTERPRISE','TEST',true,'SUCCEEDED','测试销售',?,?)",
        "初始化测试企业", code, now, now);
    long profileId = db.queryForObject("select id from er_enterprise_profile where unified_credit_code=?", Long.class, code);
    db.update("insert into er_collection_run(batch_no,trigger_type,status,total_count,created_by,scope_description,created_at,completed_at) values(?,'MANUAL',?,1,'test','test',?,?)",
        batch, runStatus, now, "COLLECTING".equals(runStatus) ? null : now);
    long runId = db.queryForObject("select id from er_collection_run where batch_no=?", Long.class, batch);
    db.update("insert into er_collection_attempt(profile_id,run_id,status,attempt_no,started_at,finished_at,created_at) values(?,?,'SUCCEEDED',1,?,?,?)",
        profileId, runId, now, now, now);
    long attemptId = db.queryForObject("select max(id) from er_collection_attempt where profile_id=?", Long.class, profileId);
    db.update("insert into er_capability_result(attempt_id,capability_code,status,hit,record_count,collected_at) values(?,'RISK_OVERVIEW','SUCCEEDED',true,1,?)", attemptId, now);
    db.update("insert into er_risk_score(profile_id,model_type,model_version,risk_level,evidence_json,scored_at) values(?,'ENTERPRISE',1,'HIGH*1','{}',?)", profileId, now);
    long scoreId = db.queryForObject("select max(id) from er_risk_score where profile_id=?", Long.class, profileId);
    db.update("insert into er_risk_event(profile_id,score_id,risk_level,status,evidence_json,created_at) values(?,?,'HIGH','OPEN','{}',?)", profileId, scoreId, now);
    long eventId = db.queryForObject("select max(id) from er_risk_event where profile_id=?", Long.class, profileId);
    db.update("insert into er_work_order(profile_id,event_id,status,created_at) values(?,?,'OPEN',?)", profileId, eventId, now);
    db.update("insert into er_batch_risk_change(run_id,profile_id,previous_risk_level,current_risk_level,risk_status,notification_eligible,created_at,updated_at) values(?,?, 'NONE','HIGH','NEW',true,?,?)", runId, profileId, now, now);
    db.update("insert into er_wecom_notification_log(run_id,batch_no,status,message_text,created_at,idempotency_key) values(?,?,'SUCCEEDED','test',?,?)", runId, batch, now, "INIT:" + runId);
    return new Fixture(profileId, runId, code);
  }

  private void cleanupAll(Fixture fixture) {
    db.update("delete from er_work_order where profile_id=?", fixture.profileId);
    db.update("delete from er_risk_event where profile_id=?", fixture.profileId);
    db.update("delete from er_risk_score where profile_id=?", fixture.profileId);
    db.update("delete from er_batch_risk_change where profile_id=?", fixture.profileId);
    db.update("delete from er_capability_result where attempt_id in (select id from er_collection_attempt where profile_id=?)", fixture.profileId);
    db.update("delete from er_collection_attempt where profile_id=?", fixture.profileId);
    db.update("delete from er_enterprise_profile where id=?", fixture.profileId);
    cleanupHistory(fixture);
  }

  private void cleanupHistory(Fixture fixture) {
    db.update("delete from er_wecom_notification_log where run_id=?", fixture.runId);
    db.update("delete from er_batch_risk_change where run_id=?", fixture.runId);
    db.update("delete from er_capability_result where attempt_id in (select id from er_collection_attempt where run_id=?)", fixture.runId);
    db.update("delete from er_collection_attempt where run_id=?", fixture.runId);
    db.update("delete from er_collection_run where id=?", fixture.runId);
    db.update("delete from er_audit_log where actor_account='admin-test' and action='CUSTOMER_INITIALIZE'");
  }

  private int count(String sql) { return db.queryForObject(sql, Integer.class); }
  private int number(Map<String,Object> values, String key) { return ((Number) values.get(key)).intValue(); }
  private record Fixture(long profileId, long runId, String code) {}
}
