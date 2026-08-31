package db.migration;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

/** Removes retired limit-consumption data and recalculates historical warning evidence. */
public class V14__remove_limit_consumption extends BaseJavaMigration {
  private static final String REMOVED_FACTOR = "LIMIT_CONSUMPTION";
  private static final ObjectMapper JSON = new ObjectMapper();

  @Override
  public void migrate(Context context) throws Exception {
    Connection connection = context.getConnection();
    try (PreparedStatement statement = connection.prepareStatement(
        "delete from er_capability_result where capability_code=?")) {
      statement.setString(1, REMOVED_FACTOR);
      statement.executeUpdate();
    }
    try (PreparedStatement statement = connection.prepareStatement(
        "delete from er_risk_model_factor where factor_code=?")) {
      statement.setString(1, REMOVED_FACTOR);
      statement.executeUpdate();
    }

    rewriteScores(connection);
    syncLatestProfileLevels(connection);
  }

  private void rewriteScores(Connection connection) throws Exception {
    try (PreparedStatement select = connection.prepareStatement(
             "select id, evidence_json from er_risk_score");
         ResultSet rows = select.executeQuery();
         PreparedStatement update = connection.prepareStatement(
             "update er_risk_score set evidence_json=?, risk_level=? where id=?")) {
      while (rows.next()) {
        Map<String, Object> evidence = parseEvidence(rows.getString("evidence_json"));
        List<Map<String, Object>> factors = retainedFactors(evidence.get("factors"));
        evidence.put("factors", factors);
        evidence.put("hitFactorCount", factors.stream()
            .filter(factor -> "TRIGGERED".equals(String.valueOf(factor.get("result"))))
            .count());

        update.setString(1, JSON.writeValueAsString(evidence));
        update.setString(2, riskLevel(evidence, factors));
        update.setLong(3, rows.getLong("id"));
        update.addBatch();
      }
      update.executeBatch();
    }
  }

  private Map<String, Object> parseEvidence(String value) {
    try {
      return JSON.readValue(value, new TypeReference<LinkedHashMap<String, Object>>() {});
    } catch (Exception ignored) {
      Map<String, Object> empty = new LinkedHashMap<>();
      empty.put("factors", List.of());
      empty.put("provisional", false);
      empty.put("compatibility", true);
      return empty;
    }
  }

  @SuppressWarnings("unchecked")
  private List<Map<String, Object>> retainedFactors(Object value) {
    List<Map<String, Object>> retained = new ArrayList<>();
    if (!(value instanceof List<?> factors)) return retained;
    for (Object item : factors) {
      if (!(item instanceof Map<?, ?> factor)
          || REMOVED_FACTOR.equals(String.valueOf(factor.get("factorCode")))) continue;
      retained.add(new LinkedHashMap<>((Map<String, Object>) factor));
    }
    return retained;
  }

  private String riskLevel(Map<String, Object> evidence, List<Map<String, Object>> factors) {
    int[] counts = new int[4];
    boolean unavailable = false;
    for (Map<String, Object> factor : factors) {
      String result = String.valueOf(factor.get("result"));
      if ("UNAVAILABLE".equals(result)) unavailable = true;
      if (!"TRIGGERED".equals(result)) continue;
      switch (String.valueOf(factor.get("configuredRiskLevel") == null
          ? factor.get("riskLevel") : factor.get("configuredRiskLevel"))) {
        case "LOW" -> counts[0]++;
        case "MEDIUM" -> counts[1]++;
        case "HIGH" -> counts[2]++;
        case "CRITICAL" -> counts[3]++;
        default -> { }
      }
    }
    for (int i = counts.length - 1; i >= 0; i--) {
      if (counts[i] > 0) return switch (i) {
        case 0 -> "LOW*" + counts[i];
        case 1 -> "MEDIUM*" + counts[i];
        case 2 -> "HIGH*" + counts[i];
        default -> "CRITICAL*" + counts[i];
      };
    }
    return Boolean.TRUE.equals(evidence.get("provisional")) && unavailable ? "INSUFFICIENT_DATA" : "NONE";
  }

  private void syncLatestProfileLevels(Connection connection) throws SQLException {
    try (PreparedStatement profiles = connection.prepareStatement("select id from er_enterprise_profile");
         ResultSet rows = profiles.executeQuery();
         PreparedStatement latestScore = connection.prepareStatement(
             "select risk_level from er_risk_score where profile_id=? order by scored_at desc, id desc limit 1");
         PreparedStatement update = connection.prepareStatement(
             "update er_enterprise_profile set risk_level=?, risk_score=null, coverage=null where id=?")) {
      while (rows.next()) {
        long profileId = rows.getLong("id");
        latestScore.setLong(1, profileId);
        try (ResultSet score = latestScore.executeQuery()) {
          if (!score.next()) continue;
          update.setString(1, score.getString("risk_level"));
          update.setLong(2, profileId);
          update.addBatch();
        }
      }
      update.executeBatch();
    }
  }
}
