package com.tehang.enterpriserisk.service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.time.Instant;
import java.util.*;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class WecomRobotService {
  static final int WECOM_TEXT_MAX_BYTES = 2048;
  private static final int MAX_ATTEMPTS = 3;
  private static final long[] RETRY_DELAYS_MS = {200, 500};
  private final JdbcTemplate db;
  private final byte[] secret;
  private final HttpClient http = HttpClient.newHttpClient();
  private final TransactionTemplate transactions;

  public WecomRobotService(JdbcTemplate db,
      @Value("${enterprise-risk.key-encryption-secret}") String secret,
      PlatformTransactionManager transactionManager) {
    this.db = db;
    this.secret = Arrays.copyOf(secret.getBytes(StandardCharsets.UTF_8), 32);
    this.transactions = new TransactionTemplate(transactionManager);
  }

  public Map<String,Object> config() {
    Map<String,Object> row = db.queryForMap("select enabled,webhook_ciphertext,risk_levels riskLevels,updated_by updatedBy,updated_at updatedAt from er_wecom_robot_config where id=1");
    String cipher = (String) row.remove("webhook_ciphertext");
    boolean configured = cipher != null && !cipher.isBlank();
    row.put("configured", configured);
    row.put("webhook", configured ? decrypt(cipher) : null);
    row.put("riskLevels", parseLevels(String.valueOf(row.get("riskLevels"))));
    return row;
  }

  public void save(boolean enabled, String webhook, List<String> riskLevels, String actor) {
    if (enabled && (webhook == null || webhook.isBlank()) && !Boolean.TRUE.equals(config().get("configured")))
      throw new IllegalArgumentException("启用机器人时必须填写 Webhook");
    String levels = String.join(",", normalizeLevels(riskLevels));
    if (webhook == null || webhook.isBlank())
      db.update("update er_wecom_robot_config set enabled=?,risk_levels=?,updated_by=?,updated_at=? where id=1", enabled, levels, actor, Instant.now());
    else
      db.update("update er_wecom_robot_config set enabled=?,webhook_ciphertext=?,risk_levels=?,updated_by=?,updated_at=? where id=1", enabled, encrypt(webhook.trim()), levels, actor, Instant.now());
  }

  private List<String> parseLevels(String raw) {
    return Arrays.stream(raw.split(",")).map(String::trim).filter(x -> Set.of("LOW","MEDIUM","HIGH","CRITICAL").contains(x)).toList();
  }
  private List<String> normalizeLevels(List<String> levels) {
    return levels == null ? List.of("CRITICAL","HIGH") : levels.stream().filter(x -> Set.of("LOW","MEDIUM","HIGH","CRITICAL").contains(x)).distinct().toList();
  }
  public void test() { send("企业风控机器人测试消息"); }

  public void notifyRun(long runId, String batch) {
    Map<String,Object> cfg = config();
    if (!Boolean.TRUE.equals(cfg.get("enabled"))) return;
    @SuppressWarnings("unchecked") List<String> levels = (List<String>) cfg.get("riskLevels");
    if (levels == null || levels.isEmpty()) return;
    String cond = String.join(" or ", Collections.nCopies(levels.size(), "c.current_risk_level=? or c.current_risk_level like ?"));
    List<Object> args = new ArrayList<>(); args.add(runId);
    for (String level : levels) { args.add(level); args.add(level + "*%"); }
    List<Map<String,Object>> rows = db.queryForList(
        "select c.profile_id profileId,c.risk_status riskStatus,c.current_risk_level level,p.company_name companyName,coalesce(nullif(p.sales_manager,''),'未分配销售经理') manager " +
        "from er_batch_risk_change c join er_enterprise_profile p on p.id=c.profile_id " +
        "where c.run_id=? and c.notification_eligible=true and c.notified_at is null " +
        "and c.risk_status in ('NEW','UPGRADED') and (" + cond + ") order by c.id", args.toArray());
    if (rows.isEmpty()) return;

    List<Long> profileIds = rows.stream().map(row -> ((Number) row.get("profileId")).longValue()).toList();
    sendLogged(runId, batch, buildNotificationMessage(batch, rows, levels, WECOM_TEXT_MAX_BYTES), profileIds);
  }

  static String buildNotificationMessage(String batch, List<Map<String,Object>> rows, List<String> levels, int maxBytes) {
    Map<String,Integer> statuses = new LinkedHashMap<>();
    Map<String,Map<String,Integer>> statusLevels = new LinkedHashMap<>();
    Map<String,List<NotificationItem>> grouped = new HashMap<>();
    for (Map<String,Object> row : rows) {
      String level = baseLevel(String.valueOf(row.get("level")));
      String status = String.valueOf(row.get("riskStatus"));
      String manager = cleanText(row.get("manager"), "未分配销售经理");
      String company = cleanText(row.get("companyName"), "未命名机构");
      statuses.merge(status, 1, Integer::sum);
      statusLevels.computeIfAbsent(status, key -> new LinkedHashMap<>()).merge(level, 1, Integer::sum);
      grouped.computeIfAbsent(manager, key -> new ArrayList<>()).add(new NotificationItem(company, level, status));
    }

    StringBuilder message = new StringBuilder("企业风控采集完成\n批次号：").append(batch)
        .append("\n需跟进风险企业：").append(rows.size()).append(" 家");
    appendStatusLine(message, "新增风险", "NEW", statuses, statusLevels, levels);
    appendStatusLine(message, "风险升级", "UPGRADED", statuses, statusLevels, levels);
    message.append("\n\n机构客户（按销售经理）");

    Comparator<NotificationItem> itemOrder = Comparator
        .comparingInt((NotificationItem item) -> riskRank(item.level())).reversed()
        .thenComparingInt(item -> "UPGRADED".equals(item.status()) ? 0 : 1)
        .thenComparing(NotificationItem::company);
    grouped.values().forEach(items -> items.sort(itemOrder));
    List<Map.Entry<String,List<NotificationItem>>> groups = new ArrayList<>(grouped.entrySet());
    groups.sort((left, right) -> {
      NotificationItem leftTop = left.getValue().getFirst(), rightTop = right.getValue().getFirst();
      int priority = Integer.compare(riskRank(rightTop.level()), riskRank(leftTop.level()));
      if (priority == 0) priority = Integer.compare("UPGRADED".equals(leftTop.status()) ? 0 : 1, "UPGRADED".equals(rightTop.status()) ? 0 : 1);
      return priority != 0 ? priority : left.getKey().compareTo(right.getKey());
    });

    int shown = 0;
    boolean full = false;
    for (Map.Entry<String,List<NotificationItem>> group : groups) {
      boolean headerAdded = false;
      for (NotificationItem item : group.getValue()) {
        String addition = (headerAdded ? "" : "\n【" + group.getKey() + "｜" + group.getValue().size() + "家】") + "\n- " + item.company();
        int hiddenAfter = rows.size() - shown - 1;
        String suffix = hiddenAfter > 0 ? truncationSuffix(hiddenAfter) : "";
        String fallbackSuffix = truncationSuffix(rows.size() - shown);
        if (utf8Length(message.toString() + addition + suffix) > maxBytes || utf8Length(message.toString() + fallbackSuffix) > maxBytes) { full = true; break; }
        message.append(addition);
        headerAdded = true;
        shown++;
      }
      if (full) break;
    }
    int hidden = rows.size() - shown;
    if (hidden > 0) message.append(truncationSuffix(hidden));
    return message.toString();
  }

  private static String truncationSuffix(int hidden) { return "\n另有 " + hidden + " 家未展示，请登录系统查看"; }
  private static int utf8Length(String value) { return value.getBytes(StandardCharsets.UTF_8).length; }
  private static String baseLevel(String level) { return level.split("\\*", 2)[0]; }
  private static String cleanText(Object value, String fallback) {
    String text = value == null ? "" : String.valueOf(value).replace('\n', ' ').replace('\r', ' ').trim();
    return text.isEmpty() ? fallback : text;
  }
  private static int riskRank(String level) { return switch (level) { case "CRITICAL" -> 4; case "HIGH" -> 3; case "MEDIUM" -> 2; case "LOW" -> 1; default -> 0; }; }
  private record NotificationItem(String company, String level, String status) {}

  private static void appendStatusLine(StringBuilder text, String name, String status, Map<String,Integer> statuses,
      Map<String,Map<String,Integer>> statusLevels, List<String> levels) {
    int count = statuses.getOrDefault(status, 0); if (count == 0) return;
    text.append("\n").append(name).append("：").append(count).append(" 家");
    List<String> details = new ArrayList<>(); Map<String,Integer> byLevel = statusLevels.getOrDefault(status, Map.of());
    for (String level : levels) { int n = byLevel.getOrDefault(level, 0); if (n > 0) details.add(levelName(level) + "：" + n + "家"); }
    if (!details.isEmpty()) text.append("【").append(String.join("、", details)).append("】");
  }
  private static String levelName(String level) { return switch(level) { case "CRITICAL" -> "严重风险"; case "HIGH" -> "高风险"; case "MEDIUM" -> "中风险"; case "LOW" -> "低风险"; default -> level; }; }

  public List<Map<String,Object>> logs() {
    return db.queryForList("select id,batch_no batchNo,status,retry_count retryCount,message_text messageText,response_summary responseSummary,sent_at sentAt,created_at createdAt from er_wecom_notification_log order by id desc");
  }

  private void sendLogged(long runId, String batch, String text, List<Long> profileIds) {
    Long logId = createNotification(runId, batch, text); if (logId == null) return;
    for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
      try {
        String result = send(text); Instant sentAt = Instant.now();
        try { markSucceeded(logId, runId, profileIds, attempt - 1, result, sentAt); }
        catch (RuntimeException persistenceFailure) { markPersistenceFailure(logId, attempt - 1, persistenceFailure); }
        return; // Already accepted externally: never resend because local persistence failed.
      } catch (SendException failure) {
        if (!failure.retryable || attempt == MAX_ATTEMPTS) { markFailed(logId, attempt - 1, failure.getMessage()); return; }
        db.update("update er_wecom_notification_log set retry_count=?,response_summary=? where id=?", attempt, summary(failure.getMessage()), logId);
        if (!pauseBeforeRetry(attempt - 1)) { markFailed(logId, attempt - 1, "发送重试被中断"); return; }
      }
    }
  }

  private Long createNotification(long runId, String batch, String text) {
    KeyHolder keys = new GeneratedKeyHolder();
    try {
      db.update(connection -> {
        PreparedStatement statement = connection.prepareStatement(
            "insert into er_wecom_notification_log(run_id,batch_no,status,message_text,created_at,idempotency_key) values(?,?, 'PENDING',?,?,?)", Statement.RETURN_GENERATED_KEYS);
        statement.setLong(1, runId); statement.setString(2, batch); statement.setString(3, text);
        statement.setObject(4, Instant.now()); statement.setString(5, "RUN:" + runId); return statement;
      }, keys);
      Number key = keys.getKey(); if (key == null) throw new IllegalStateException("未能获取通知日志 ID");
      return key.longValue();
    } catch (DuplicateKeyException duplicate) { return null; }
  }

  private void markSucceeded(long logId, long runId, List<Long> profileIds, int retries, String response, Instant sentAt) {
    transactions.executeWithoutResult(status -> {
      db.update("update er_wecom_notification_log set status='SUCCEEDED',retry_count=?,response_summary=?,sent_at=? where id=?", retries, summary(response), sentAt, logId);
      String placeholders = String.join(",", Collections.nCopies(profileIds.size(), "?"));
      List<Object> changeArgs = new ArrayList<>(); changeArgs.add(sentAt); changeArgs.add(sentAt); changeArgs.add(runId); changeArgs.addAll(profileIds);
      db.update("update er_batch_risk_change set notified_at=?,updated_at=? where run_id=? and profile_id in (" + placeholders + ")", changeArgs.toArray());
      List<Object> watermarkArgs = new ArrayList<>(); watermarkArgs.add(runId); watermarkArgs.addAll(profileIds);
      db.update("update er_enterprise_profile p set highest_notified_risk_level=(select c.current_risk_level from er_batch_risk_change c where c.run_id=? and c.profile_id=p.id) where p.id in (" + placeholders + ")", watermarkArgs.toArray());
    });
  }

  private void markPersistenceFailure(long logId, int retries, RuntimeException failure) {
    try { db.update("update er_wecom_notification_log set status='FAILED',retry_count=?,response_summary=? where id=?", retries, summary("消息已发送，但成功状态落库失败：" + failure.getMessage()), logId); }
    catch (RuntimeException ignored) { /* Never risk another external send. */ }
  }
  private void markFailed(long logId, int retries, String message) {
    db.update("update er_wecom_notification_log set status='FAILED',retry_count=?,response_summary=? where id=?", retries, summary(message == null ? "发送失败" : message), logId);
  }
  private boolean pauseBeforeRetry(int index) {
    try { Thread.sleep(RETRY_DELAYS_MS[index]); return true; }
    catch (InterruptedException interrupted) { Thread.currentThread().interrupt(); return false; }
  }
  private String summary(String value) { return value == null ? null : value.substring(0, Math.min(1000, value.length())); }

  private String send(String text) {
    Map<String,Object> config = db.queryForMap("select enabled,webhook_ciphertext from er_wecom_robot_config where id=1");
    if (!Boolean.TRUE.equals(config.get("enabled"))) throw new SendException("企业微信机器人未启用", false);
    String url;
    try { url = decrypt((String) config.get("webhook_ciphertext")); }
    catch (RuntimeException invalidConfig) { throw new SendException(invalidConfig.getMessage(), false, invalidConfig); }
    String body = "{\"msgtype\":\"text\",\"text\":{\"content\":\"" + escape(text) + "\"}}";
    HttpResponse<String> response;
    try {
      response = http.send(HttpRequest.newBuilder(URI.create(url)).header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString(body)).build(), HttpResponse.BodyHandlers.ofString());
    } catch (IllegalArgumentException invalidUrl) { throw new SendException("Webhook 地址无效", false, invalidUrl); }
    catch (IOException networkFailure) { throw new SendException(networkFailure.getMessage(), true, networkFailure); }
    catch (InterruptedException interrupted) { Thread.currentThread().interrupt(); throw new SendException("发送被中断", false, interrupted); }
    int status = response.statusCode();
    if (status == 429 || status >= 500) throw new SendException("企业微信 HTTP " + status + "：" + response.body(), true);
    if (status / 100 != 2) throw new SendException("企业微信 HTTP " + status + "：" + response.body(), false);
    if (response.body().contains("\"errcode\":") && !response.body().contains("\"errcode\":0")) {
      boolean rateLimited = response.body().matches("(?s).*\"errcode\"\\s*:\\s*45009(?:\\D.*|$)");
      throw new SendException("企业微信返回：" + response.body(), rateLimited);
    }
    return response.body();
  }

  private String escape(String value) { return value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n"); }
  private String encrypt(String value) {
    try { byte[] iv = new byte[12]; new SecureRandom().nextBytes(iv); Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding"); cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(secret,"AES"), new GCMParameterSpec(128,iv)); return Base64.getEncoder().encodeToString(iv) + ":" + Base64.getEncoder().encodeToString(cipher.doFinal(value.getBytes(StandardCharsets.UTF_8))); }
    catch (Exception failure) { throw new IllegalStateException(failure); }
  }
  private String decrypt(String value) {
    try { String[] parts = value.split(":"); Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding"); cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(secret,"AES"), new GCMParameterSpec(128,Base64.getDecoder().decode(parts[0]))); return new String(cipher.doFinal(Base64.getDecoder().decode(parts[1])), StandardCharsets.UTF_8); }
    catch (Exception failure) { throw new IllegalStateException("Webhook 解密失败", failure); }
  }

  private static final class SendException extends IllegalStateException {
    private final boolean retryable;
    private SendException(String message, boolean retryable) { super(message); this.retryable = retryable; }
    private SendException(String message, boolean retryable, Throwable cause) { super(message, cause); this.retryable = retryable; }
  }
}
