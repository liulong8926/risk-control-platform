package com.tehang.enterpriserisk.api;
import com.tehang.enterpriserisk.service.AuthService;
import com.tehang.enterpriserisk.service.RiskService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/risk-management/organization-risk")
public class SalesManagerController {
 private final RiskService risk; private final AuthService auth;
 public SalesManagerController(RiskService risk,AuthService auth){this.risk=risk;this.auth=auth;}
 @GetMapping("/sales-managers") public java.util.List<java.util.Map<String,Object>> list(){return auth.accounts().stream().filter(x->Boolean.TRUE.equals(x.get("enabled"))).toList();}
 @PatchMapping("/{id}/sales-manager") public void update(@PathVariable long id,@Valid @RequestBody Request body,HttpServletRequest req){if(!auth.hasPermission((String)req.getAttribute("role"),"RISK_WRITE"))throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.FORBIDDEN,"当前角色无操作权限");risk.updateSalesManager(id,body.salesManager());}
 public record Request(@NotBlank String salesManager){}
}
