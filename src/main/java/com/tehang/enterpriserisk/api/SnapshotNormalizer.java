package com.tehang.enterpriserisk.api;

import java.util.LinkedHashMap;
import java.util.Map;

/** Normalizes H2 lower-cased aliases so the frontend receives its camelCase contract. */
final class SnapshotNormalizer {
  private SnapshotNormalizer() {}

  static Map<String,Object> normalize(Map<String,Object> snapshot) {
    Object value = snapshot.get("profile");
    if (value instanceof Map<?,?> raw) {
      Map<String,Object> profile = new LinkedHashMap<>();
      raw.forEach((k,v) -> profile.put(String.valueOf(k), v));
      copy(profile, "companyName", "companyname");
      copy(profile, "unifiedCreditCode", "unifiedcreditcode");
      copy(profile, "subjectType", "subjecttype");
      copy(profile, "monitoringEnabled", "monitoringenabled");
      copy(profile, "collectionStatus", "collectionstatus");
      copy(profile, "latestSuccessAt", "latestsuccessat");
      copy(profile, "riskScore", "riskscore");
      copy(profile, "riskLevel", "risklevel");
      copy(profile, "createdAt", "createdat");
      copy(profile, "salesManager", "salesmanager");
      copy(profile, "latestCollectionBatchNo", "latestcollectionbatchno");
      snapshot.put("profile", profile);
    }
    Object capsValue = snapshot.get("capabilities");
    if (capsValue instanceof java.util.List<?> caps) {
      for (Object item : caps) if (item instanceof Map<?,?> raw) {
        Map<String,Object> cap = new LinkedHashMap<>(); raw.forEach((k,v) -> cap.put(String.valueOf(k), v));
        copy(cap,"recordCount","recordcount"); copy(cap,"errorCode","errorcode"); copy(cap,"errorMessage","errormessage"); copy(cap,"collectedAt","collectedat");
        ((java.util.List) caps).set(((java.util.List)caps).indexOf(item), cap);
      }
    }
    return snapshot;
  }

  private static void copy(Map<String,Object> m, String target, String source) {
    if (!m.containsKey(target) && m.containsKey(source)) m.put(target, m.get(source));
  }
}
