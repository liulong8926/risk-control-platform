package com.tehang.enterpriserisk;

import static org.junit.jupiter.api.Assertions.*;

import com.sun.net.httpserver.HttpServer;
import com.tehang.enterpriserisk.service.WecomRobotService;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootTest
class WecomRobotServiceTest {
  @Autowired WecomRobotService robot;
  @Autowired JdbcTemplate db;

  @Test void sameRunIsSentOnlyOnceAndMarksRiskAsNotified() throws Exception {
    AtomicInteger requests = new AtomicInteger();
    HttpServer server = server(requests, 0);
    TestData data = createData("idem");
    try {
      robot.save(true, webhook(server), List.of("HIGH"), "test");
      robot.notifyRun(data.runId, data.batch);
      robot.notifyRun(data.runId, data.batch);

      assertEquals(1, requests.get());
      assertEquals(1, db.queryForObject("select count(*) from er_wecom_notification_log where run_id=?", Integer.class, data.runId));
      assertEquals("SUCCEEDED", db.queryForObject("select status from er_wecom_notification_log where run_id=?", String.class, data.runId));
      assertEquals(0, db.queryForObject("select retry_count from er_wecom_notification_log where run_id=?", Integer.class, data.runId));
      assertNotNull(db.queryForObject("select notified_at from er_batch_risk_change where run_id=? and profile_id=?", Instant.class, data.runId, data.profileId));
    } finally {
      server.stop(0);
      cleanup(data);
    }
  }

  @Test void transientServerErrorsAreRetriedUntilSuccess() throws Exception {
    AtomicInteger requests = new AtomicInteger();
    HttpServer server = server(requests, 2);
    TestData data = createData("retry");
    try {
      robot.save(true, webhook(server), List.of("HIGH"), "test");
      robot.notifyRun(data.runId, data.batch);

      assertEquals(3, requests.get());
      assertEquals("SUCCEEDED", db.queryForObject("select status from er_wecom_notification_log where run_id=?", String.class, data.runId));
      assertEquals(2, db.queryForObject("select retry_count from er_wecom_notification_log where run_id=?", Integer.class, data.runId));
      assertNotNull(db.queryForObject("select notified_at from er_batch_risk_change where run_id=? and profile_id=?", Instant.class, data.runId, data.profileId));
    } finally {
      server.stop(0);
      cleanup(data);
    }
  }

  @Test void alreadyNotifiedRiskDoesNotCreateAnotherNotification() throws Exception {
    AtomicInteger requests = new AtomicInteger();
    HttpServer server = server(requests, 0);
    TestData data = createData("notified");
    try {
      db.update("update er_batch_risk_change set notified_at=? where run_id=? and profile_id=?", Instant.now(), data.runId, data.profileId);
      robot.save(true, webhook(server), List.of("HIGH"), "test");
      robot.notifyRun(data.runId, data.batch);

      assertEquals(0, requests.get());
      assertEquals(0, db.queryForObject("select count(*) from er_wecom_notification_log where run_id=?", Integer.class, data.runId));
    } finally {
      server.stop(0);
      cleanup(data);
    }
  }

  @Test void notificationGroupsCompaniesByManagerAndSortsByRiskPriority() throws Exception {
    AtomicInteger requests = new AtomicInteger();
    HttpServer server = server(requests, 0);
    TestData data = createData("groups");
    try {
      addCompany(data.runId, "陈金霞", "A严重升级机构", "CRITICAL", "UPGRADED");
      addCompany(data.runId, "陈金霞", "B高风险机构", "HIGH", "NEW");
      addCompany(data.runId, "", "C未分配机构", "HIGH", "NEW");
      robot.save(true, webhook(server), List.of("CRITICAL", "HIGH"), "test");
      robot.notifyRun(data.runId, data.batch);

      String message = notificationMessage(data.runId);
      assertTrue(message.contains("机构客户（按销售经理）"));
      assertTrue(message.contains("【陈金霞｜2家】\n- A严重升级机构\n- B高风险机构"));
      assertTrue(message.contains("【未分配销售经理｜1家】\n- C未分配机构"));
      assertTrue(message.indexOf("【陈金霞｜2家】") < message.indexOf("【测试经理｜1家】"));
      assertFalse(message.contains("销售经理分布："));
    } finally {
      server.stop(0);
      cleanup(data);
    }
  }

  @Test void longCompanyListIsTruncatedWithinWecomUtf8Limit() throws Exception {
    AtomicInteger requests = new AtomicInteger();
    HttpServer server = server(requests, 0);
    TestData data = createData("truncate");
    try {
      for (int i = 0; i < 18; i++)
        addCompany(data.runId, "长名单经理", "机构" + String.format("%02d", i) + "-" + "超长客户名称".repeat(18), i == 0 ? "CRITICAL" : "HIGH", i == 0 ? "UPGRADED" : "NEW");
      robot.save(true, webhook(server), List.of("CRITICAL", "HIGH"), "test");
      robot.notifyRun(data.runId, data.batch);

      String message = notificationMessage(data.runId);
      assertTrue(message.getBytes(StandardCharsets.UTF_8).length <= 2048);
      assertTrue(message.contains("另有 "));
      assertTrue(message.contains(" 家未展示，请登录系统查看"));
      assertTrue(message.contains("机构00-"));
      assertFalse(message.contains("机构17-"));
    } finally {
      server.stop(0);
      cleanup(data);
    }
  }

  private HttpServer server(AtomicInteger requests, int failuresBeforeSuccess) throws Exception {
    HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    server.createContext("/webhook", exchange -> {
      int request = requests.incrementAndGet();
      exchange.getRequestBody().readAllBytes();
      boolean fail = request <= failuresBeforeSuccess;
      byte[] body = (fail ? "temporary failure" : "{\"errcode\":0,\"errmsg\":\"ok\"}").getBytes(StandardCharsets.UTF_8);
      exchange.sendResponseHeaders(fail ? 500 : 200, body.length);
      exchange.getResponseBody().write(body);
      exchange.close();
    });
    server.start();
    return server;
  }

  private String webhook(HttpServer server) {
    return "http://127.0.0.1:" + server.getAddress().getPort() + "/webhook";
  }

  private TestData createData(String prefix) {
    String suffix = Long.toUnsignedString(System.nanoTime());
    String code = (prefix + suffix).substring(0, Math.min(32, prefix.length() + suffix.length()));
    String batch = "TEST-" + prefix + "-" + suffix;
    Instant now = Instant.now();
    db.update("insert into er_enterprise_profile(company_name,unified_credit_code,subject_type,source,monitoring_enabled,collection_status,risk_level,sales_manager,created_at,updated_at) values(?,?, 'ENTERPRISE','TEST',true,'SUCCEEDED','HIGH','测试经理',?,?)",
        "机器人测试企业" + suffix, code, now, now);
    long profileId = db.queryForObject("select id from er_enterprise_profile where unified_credit_code=?", Long.class, code);
    db.update("insert into er_collection_run(batch_no,trigger_type,status,total_count,created_by,scope_description,created_at,completed_at) values(?,'MANUAL','SUCCEEDED',1,'test','test',?,?)", batch, now, now);
    long runId = db.queryForObject("select id from er_collection_run where batch_no=?", Long.class, batch);
    db.update("insert into er_batch_risk_change(run_id,profile_id,previous_risk_level,current_risk_level,risk_status,notification_eligible,created_at,updated_at) values(?,?, 'NONE','HIGH','NEW',true,?,?)",
        runId, profileId, now, now);
    return new TestData(runId, profileId, batch);
  }

  private long addCompany(long runId, String manager, String company, String level, String status) {
    String suffix = Long.toUnsignedString(System.nanoTime());
    Instant now = Instant.now();
    db.update("insert into er_enterprise_profile(company_name,unified_credit_code,subject_type,source,monitoring_enabled,collection_status,risk_level,sales_manager,created_at,updated_at) values(?,?, 'ENTERPRISE','TEST',true,'SUCCEEDED',?,?,?,?)",
        company, "extra" + suffix, level, manager, now, now);
    long profileId = db.queryForObject("select id from er_enterprise_profile where unified_credit_code=?", Long.class, "extra" + suffix);
    db.update("insert into er_batch_risk_change(run_id,profile_id,previous_risk_level,current_risk_level,risk_status,notification_eligible,created_at,updated_at) values(?,?, 'NONE',?,?,true,?,?)",
        runId, profileId, level, status, now, now);
    return profileId;
  }

  private String notificationMessage(long runId) {
    return db.queryForObject("select message_text from er_wecom_notification_log where run_id=?", String.class, runId);
  }

  private void cleanup(TestData data) {
    List<Long> profileIds = db.queryForList("select profile_id from er_batch_risk_change where run_id=?", Long.class, data.runId);
    db.update("delete from er_wecom_notification_log where run_id=?", data.runId);
    db.update("delete from er_batch_risk_change where run_id=?", data.runId);
    db.update("delete from er_collection_run where id=?", data.runId);
    for (Long profileId : profileIds) db.update("delete from er_enterprise_profile where id=?", profileId);
    robot.save(false, null, List.of("CRITICAL", "HIGH"), "test");
  }

  private record TestData(long runId, long profileId, String batch) {}
}
