package com.tehang.enterpriserisk.service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class TianyanchaClient {
  private final String endpoint;
  private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(15)).build();
  private final AtomicLong ids = new AtomicLong(1);
  public TianyanchaClient(@Value("${enterprise-risk.tianyancha.endpoint}") String endpoint) { this.endpoint = endpoint; }

  public Result call(String auth, String capability, String name, String code) {
    try {
      String tool = toolName(capability);
      String session = null;
      HttpResponse<String> init = send("{\"jsonrpc\":\"2.0\",\"id\":" + ids.getAndIncrement() + ",\"method\":\"initialize\",\"params\":{\"protocolVersion\":\"2025-03-26\",\"capabilities\":{},\"clientInfo\":{\"name\":\"enterprise-risk\",\"version\":\"0.1.0\"}}}", auth, null);
      session = init.headers().firstValue("Mcp-Session-Id").orElse(null);
      send("{\"jsonrpc\":\"2.0\",\"method\":\"notifications/initialized\"}", auth, session);
      String wrapper = "{\"company_name\":\"" + esc(name) + "\",\"company_id\":\"" + esc(code) + "\",\"tool_name\":\"" + tool + "\",\"arguments\":" + arguments(capability) + "}";
      String request = "{\"jsonrpc\":\"2.0\",\"id\":" + ids.getAndIncrement() + ",\"method\":\"tools/call\",\"params\":{\"name\":\"call_tool\",\"arguments\":" + wrapper + "}}";
      String text = extractText(data(send(request, auth, session).body()));
      if (text.contains("工具调用参数不足") || text.contains("请求失败") || text.contains("未查询到") || text.contains("未发现") || text.contains("空结果")) {
        if (text.contains("未查询到") || text.contains("未发现") || text.contains("空结果")) return new Result("SUCCEEDED", false, List.of(), null);
        return new Result("PROVIDER_ERROR", false, List.of(), "天眼查 MCP 工具返回错误：" + text);
      }
      if (text.isBlank() || text.contains("暂无数据")) return new Result("SUCCEEDED", false, List.of(), null);
      List<Map<String,Object>> records = parseRecords(text);
      return new Result("SUCCEEDED", !records.isEmpty() && !(records.size()==1 && records.getFirst().containsKey("rawSummary") && text.trim().isEmpty()), List.copyOf(records), null);
    } catch (Exception e) {
      return new Result("UNAVAILABLE", false, List.of(), "天眼查 MCP 调用失败：" + (e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage()));
    }
  }

  private HttpResponse<String> send(String body, String auth, String session) throws Exception {
    HttpRequest.Builder b = HttpRequest.newBuilder(URI.create(endpoint)).timeout(Duration.ofSeconds(35)).header("Authorization", auth).header("Content-Type", "application/json").header("Accept", "application/json, text/event-stream").POST(HttpRequest.BodyPublishers.ofString(body));
    if (session != null) b.header("Mcp-Session-Id", session);
    return http.send(b.build(), HttpResponse.BodyHandlers.ofString());
  }
  private String toolName(String c) { return switch (c) { case "COMPANY_REGISTRATION" -> "get_company_registration_info"; case "EQUITY_TREE" -> "get_equity_tree"; case "RISK_OVERVIEW" -> "get_risk_overview"; case "DISHONEST" -> "get_dishonest_info"; case "JUDGMENT_DEBTOR" -> "get_judgment_debtor_info"; case "JUDICIAL_CASE" -> "get_judicial_case"; case "CASE_FILING" -> "get_case_filing_info"; case "SHELL_COMPANY" -> "get_shell_company_check"; case "BIDDING" -> "get_bidding_info"; case "FINANCING" -> "get_financing_records"; case "CREDIT_EVALUATION" -> "get_credit_evaluation"; default -> c; }; }
  private String arguments(String c) { return Set.of("DISHONEST", "JUDGMENT_DEBTOR", "JUDICIAL_CASE", "CASE_FILING", "BIDDING", "FINANCING", "CREDIT_EVALUATION").contains(c) ? "{\"page\":1,\"page_size\":20}" : "{}"; }
  private int recordCount(String text) { int rows = 0; for (String line : text.split("\\R")) { String x = line.trim(); if (x.startsWith("|") && x.endsWith("|") && !x.contains("---")) rows++; } return rows >= 2 ? Math.max(0, rows - 2) : 1; }
  private List<Map<String,Object>> parseRecords(String text) {
    List<String[]> rows=new ArrayList<>();
    for(String line:text.split("\\R")){String x=line.trim();if(x.startsWith("|")&&x.endsWith("|")&&!x.replace("|","").trim().matches("[-: ]+")){String[] p=x.substring(1,x.length()-1).split("\\|",-1);for(int i=0;i<p.length;i++)p[i]=p[i].trim();rows.add(p);}}
    if(rows.size()>=2){String[] header=rows.get(0);int start=1;if(rows.get(1).length==header.length&&Arrays.stream(rows.get(1)).allMatch(v->v.matches("[-: ]+")))start=2;List<Map<String,Object>> out=new ArrayList<>();for(int r=start;r<rows.size();r++){Map<String,Object> m=new LinkedHashMap<>();String[] row=rows.get(r);for(int c=0;c<row.length;c++)m.put(c<header.length?header[c]:"字段"+(c+1),row[c]);if(!m.isEmpty())out.add(m);}if(!out.isEmpty())return out;}
    return List.of(Map.of("rawSummary",text.substring(0,Math.min(4000,text.length()))));
  }
  private String data(String s) { for (String line : s.split("\\R")) if (line.startsWith("data:")) return line.substring(5).trim(); return s == null ? "" : s; }
  private String extractText(String s) { if (s == null) return ""; int i = s.indexOf("\"text\":\""); if (i < 0) return s; int start = i + 8; int end = s.indexOf("\\\"}],", start); if (end < 0) end = s.length(); return s.substring(start, end).replace("\\n", "\n").replace("\\\"", "\""); }
  private String esc(String s) { return s == null ? "" : s.replace("\\", "\\\\").replace("\"", "\\\""); }
  public record Result(String status, boolean hit, List<Map<String,Object>> records, String message) {}
}
