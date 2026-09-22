package com.tehang.enterpriserisk.service;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;

@Service
public class CustomerInitializationService {
  public static final String CONFIRMATION = "确认初始化";
  private final JdbcTemplate db;
  private final RiskMaintenanceLock maintenanceLock;
  private final TransactionTemplate transactions;

  public CustomerInitializationService(JdbcTemplate db, RiskMaintenanceLock maintenanceLock,
      PlatformTransactionManager transactionManager) {
    this.db = db;
    this.maintenanceLock = maintenanceLock;
    this.transactions = new TransactionTemplate(transactionManager);
  }

  public Map<String,Object> preview() { return counts(); }

  public Map<String,Object> initialize(String confirmation, String actor) {
    if (!CONFIRMATION.equals(confirmation)) throw new IllegalArgumentException("请输入“确认初始化”");
    return maintenanceLock.withMaintenance(() -> transactions.execute(status -> {
      int activeRuns = count("select count(*) from er_collection_run where status='COLLECTING'");
      int activeAttempts = count("select count(*) from er_collection_attempt where status='COLLECTING'");
      if (activeRuns > 0 || activeAttempts > 0)
        throw new ResponseStatusException(HttpStatus.CONFLICT, "当前存在进行中的采集任务，请结束后再初始化");

      Map<String,Object> result = new LinkedHashMap<>(counts());
      db.update("delete from er_work_order where profile_id in (select id from er_enterprise_profile)");
      db.update("delete from er_risk_event where profile_id in (select id from er_enterprise_profile)");
      db.update("delete from er_risk_score where profile_id in (select id from er_enterprise_profile)");
      db.update("delete from er_batch_risk_change where profile_id in (select id from er_enterprise_profile)");
      db.update("delete from er_capability_result where attempt_id in (select id from er_collection_attempt where profile_id in (select id from er_enterprise_profile))");
      db.update("delete from er_collection_attempt where profile_id in (select id from er_enterprise_profile)");
      db.update("delete from er_enterprise_profile");
      db.update("insert into er_audit_log(actor_account,action,target_type,target_id,summary,created_at) values(?, 'CUSTOMER_INITIALIZE', 'ENTERPRISE_PROFILE', 'ALL', ?, ?)",
          actor, auditSummary(result), Instant.now());
      result.put("initialized", true);
      return result;
    }));
  }

  private Map<String,Object> counts() {
    Map<String,Object> result = new LinkedHashMap<>();
    result.put("enterpriseCount", count("select count(*) from er_enterprise_profile"));
    result.put("collectionAttemptCount", count("select count(*) from er_collection_attempt where profile_id in (select id from er_enterprise_profile)"));
    result.put("capabilityResultCount", count("select count(*) from er_capability_result where attempt_id in (select id from er_collection_attempt where profile_id in (select id from er_enterprise_profile))"));
    result.put("riskScoreCount", count("select count(*) from er_risk_score where profile_id in (select id from er_enterprise_profile)"));
    result.put("batchRiskChangeCount", count("select count(*) from er_batch_risk_change where profile_id in (select id from er_enterprise_profile)"));
    result.put("riskEventCount", count("select count(*) from er_risk_event where profile_id in (select id from er_enterprise_profile)"));
    result.put("workOrderCount", count("select count(*) from er_work_order where profile_id in (select id from er_enterprise_profile)"));
    return result;
  }

  private int count(String sql) {
    Integer value = db.queryForObject(sql, Integer.class);
    return value == null ? 0 : value;
  }

  private String auditSummary(Map<String,Object> counts) {
    return "客户初始化：企业 " + counts.get("enterpriseCount") + " 条，采集任务 " + counts.get("collectionAttemptCount")
        + " 条，采集结果 " + counts.get("capabilityResultCount") + " 条，风险评分 " + counts.get("riskScoreCount")
        + " 条，风险异动 " + counts.get("batchRiskChangeCount") + " 条";
  }
}
