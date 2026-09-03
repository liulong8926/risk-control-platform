package com.tehang.enterpriserisk.api;

import com.tehang.enterpriserisk.service.RiskService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/risk-management")
public class RiskExportController {
  private final RiskService risk;
  public RiskExportController(RiskService risk) { this.risk = risk; }

  @GetMapping("/organization-risk/export")
  public ResponseEntity<byte[]> export(@RequestParam(required=false) String companyName,
      @RequestParam(required=false) String unifiedCreditCode,
      @RequestParam(required=false) String salesManager,
      @RequestParam(required=false) String batchNo,
      @RequestParam(required=false) String riskLevel,
      @RequestParam(required=false) String collectionStatus,
      @RequestParam(required=false) String batchRiskStatus,
      @RequestParam(required=false) String latestSuccessStart,
      @RequestParam(required=false) String latestSuccessEnd) {
    byte[] data = risk.exportCsv(companyName, unifiedCreditCode, salesManager, batchNo,
        riskLevel, collectionStatus, batchRiskStatus, latestSuccessStart, latestSuccessEnd);
    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=enterprise-risk-export.csv")
        .contentType(MediaType.parseMediaType("text/csv;charset=UTF-8"))
        .body(data);
  }
}
