package com.tehang.enterpriserisk.api;

import com.tehang.enterpriserisk.service.KeyService; import com.tehang.enterpriserisk.service.AuthService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.*; import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/basic-data/tianyancha-mcp-keys")
public class KeyController {
  private final KeyService service; private final AuthService auth;
  public KeyController(KeyService service, AuthService auth) { this.service = service; this.auth=auth; }
  @GetMapping public List<Map<String,Object>> list(){return service.list();}
  private void admin(HttpServletRequest q){if(!auth.hasPermission((String)q.getAttribute("role"),"KEY_MANAGE"))throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.FORBIDDEN,"需要密钥管理权限");}
  @PostMapping public Map<String,Object> create(@Valid @RequestBody Request r,HttpServletRequest q){admin(q);return service.save(null,r.name(),r.accessKey(),r.dailyLimit());}
  @PutMapping("/{id}") public Map<String,Object> update(@PathVariable long id,@RequestBody Request r,HttpServletRequest q){admin(q);return service.save(id,r.name(),r.accessKey(),r.dailyLimit());}
  @PostMapping("/{id}/status") public Map<String,Object> status(@PathVariable long id,@RequestBody Status r,HttpServletRequest q){admin(q);return service.status(id,r.enabled());}
  @DeleteMapping("/{id}") public void delete(@PathVariable long id,HttpServletRequest q){admin(q);service.delete(id);}
  public record Request(@NotBlank String name,String accessKey,@Positive Integer dailyLimit) {}
  public record Status(boolean enabled) {}
}
