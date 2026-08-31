package com.tehang.enterpriserisk.api;
import com.tehang.enterpriserisk.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api")
public class RoleController {
  private final AuthService auth; public RoleController(AuthService a){auth=a;}
  private void guard(HttpServletRequest r){if(!auth.hasPermission((String)r.getAttribute("role"),"ROLE_MANAGE"))throw new org.springframework.web.server.ResponseStatusException(HttpStatus.FORBIDDEN,"需要角色管理权限");}
  @GetMapping("/roles") public List<Map<String,Object>> list(HttpServletRequest r){guard(r);return auth.roles().stream().map(x->{Map<String,Object> y=new LinkedHashMap<>(x);y.put("permissions",auth.permissions(String.valueOf(x.get("roleCode"))));return y;}).toList();}
  @GetMapping("/permissions") public List<Map<String,Object>> permissions(HttpServletRequest r){guard(r);return auth.permissionDefinitions();}
  @PostMapping("/roles") public Map<String,Object> create(@RequestBody Req x,HttpServletRequest r){guard(r);return auth.saveRole(x.roleCode(),x.roleName(),x.enabled(),x.permissions());}
  @PutMapping("/roles/{code}") public Map<String,Object> update(@PathVariable String code,@RequestBody Req x,HttpServletRequest r){guard(r);return auth.saveRole(code,x.roleName(),x.enabled(),x.permissions());}
  @PostMapping("/roles/{code}/status") public void status(@PathVariable String code,@RequestBody Status x,HttpServletRequest r){guard(r);auth.roleStatus(code,x.enabled());}
  record Req(String roleCode,String roleName,Boolean enabled,List<String> permissions){} record Status(boolean enabled){}
}
