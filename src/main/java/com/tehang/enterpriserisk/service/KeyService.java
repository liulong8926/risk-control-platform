package com.tehang.enterpriserisk.service;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service public class KeyService {
  private final JdbcTemplate db; private final byte[] secret;
  public KeyService(JdbcTemplate db,@Value("${enterprise-risk.key-encryption-secret}") String s){this.db=db;this.secret=Arrays.copyOf(s.getBytes(StandardCharsets.UTF_8),32);}
  public List<Map<String,Object>> list(){LocalDate today=LocalDate.now();return db.queryForList("select id,name,masked_key \"maskedKey\",enabled,daily_limit \"dailyLimit\",case when usage_date=? then usage_count else 0 end \"usageCount\",usage_date \"usageDate\",last_used_at \"lastUsedAt\",failure_reason \"failureReason\",case when enabled=false then 'DISABLED' when usage_date=? and usage_count>=daily_limit then 'EXHAUSTED' else 'AVAILABLE' end \"dailyStatus\",version from er_tianyancha_key order by id desc",today,today);}
  @Transactional public Map<String,Object> save(Long id,String name,String key,Integer limit){ if(name==null||name.isBlank()) throw new IllegalArgumentException("名称不能为空"); Instant now=Instant.now(); int daily=limit==null?600:limit; if(id==null){if(key==null||key.isBlank())throw new IllegalArgumentException("Key 不能为空"); db.update("insert into er_tianyancha_key(name,ciphertext,masked_key,daily_limit,created_at,updated_at) values(?,?,?,?,?,?)",name,encrypt(key),mask(key),daily,now,now); id=db.queryForObject("select max(id) from er_tianyancha_key",Long.class);} else { if(key==null||key.isBlank()) db.update("update er_tianyancha_key set name=?,daily_limit=?,updated_at=?,version=version+1 where id=?",name,daily,now,id); else db.update("update er_tianyancha_key set name=?,ciphertext=?,masked_key=?,daily_limit=?,updated_at=?,version=version+1 where id=?",name,encrypt(key),mask(key),daily,now,id);} return db.queryForMap("select id,name,masked_key \"maskedKey\",enabled,daily_limit \"dailyLimit\",usage_count \"usageCount\",version from er_tianyancha_key where id=?",id); }
  public Map<String,Object> status(long id,boolean enabled){db.update("update er_tianyancha_key set enabled=?,updated_at=?,version=version+1 where id=?",enabled,Instant.now(),id);return db.queryForMap("select id,name,masked_key \"maskedKey\",enabled,daily_limit \"dailyLimit\",usage_count \"usageCount\",version from er_tianyancha_key where id=?",id);}
  public void delete(long id){db.update("delete from er_tianyancha_key where id=?",id);}
  public Allocation nextAllocation(){LocalDate today=LocalDate.now(); List<Map<String,Object>> rows=db.queryForList("select id,ciphertext from er_tianyancha_key where enabled=true and (usage_date is null or usage_date<>? or usage_count<daily_limit) order by id",today);if(rows.isEmpty())throw new QuotaExhaustedException();Map<String,Object> r=rows.getFirst();long id=((Number)r.get("id")).longValue();int changed=db.update("update er_tianyancha_key set usage_date=?,usage_count=case when usage_date=? then usage_count+1 else 1 end,last_used_at=?,updated_at=? where id=? and enabled=true and (usage_date is null or usage_date<>? or usage_count<daily_limit)",today,today,Instant.now(),Instant.now(),id,today);if(changed==0)return nextAllocation();return new Allocation(id,decrypt((String)r.get("ciphertext")));}
  public int remainingToday(){LocalDate today=LocalDate.now();Integer n=db.queryForObject("select coalesce(sum(case when enabled=true then case when usage_date=? then greatest(daily_limit-usage_count,0) else daily_limit end else 0 end),0) from er_tianyancha_key",Integer.class,today);return n==null?0:n;}
  public int dailyCapacity(){Integer n=db.queryForObject("select coalesce(sum(case when enabled=true then daily_limit else 0 end),0) from er_tianyancha_key",Integer.class);return n==null?0:n;}
  public String nextAuthorization(){return nextAllocation().authorization();}
  public void markSuccess(long id){db.update("update er_tianyancha_key set failure_reason=null,updated_at=? where id=?",Instant.now(),id);}
  public void markFailure(long id,String reason){String text=reason==null||reason.isBlank()?"调用失败":reason;db.update("update er_tianyancha_key set failure_reason=?,updated_at=? where id=?",text.substring(0,Math.min(255,text.length())),Instant.now(),id);}
  public record Allocation(long id,String authorization) {}
  public static final class QuotaExhaustedException extends IllegalStateException { public QuotaExhaustedException(){super("当日天眼查Key配额已用完");} }
  private String mask(String k){String x=k.trim();return x.length()<8?"****":""+x.substring(0,3)+"****"+x.substring(x.length()-4);}
  private String encrypt(String value){try{byte[] iv=new byte[12];new SecureRandom().nextBytes(iv);Cipher c=Cipher.getInstance("AES/GCM/NoPadding");c.init(Cipher.ENCRYPT_MODE,new SecretKeySpec(secret,"AES"),new GCMParameterSpec(128,iv));return Base64.getEncoder().encodeToString(iv)+":"+Base64.getEncoder().encodeToString(c.doFinal(value.getBytes(StandardCharsets.UTF_8)));}catch(Exception e){throw new IllegalStateException("Key 加密失败",e);}}
  private String decrypt(String value){try{String[] p=value.split(":");Cipher c=Cipher.getInstance("AES/GCM/NoPadding");c.init(Cipher.DECRYPT_MODE,new SecretKeySpec(secret,"AES"),new GCMParameterSpec(128,Base64.getDecoder().decode(p[0])));return new String(c.doFinal(Base64.getDecoder().decode(p[1])),StandardCharsets.UTF_8);}catch(Exception e){throw new IllegalStateException("Key 解密失败",e);}}
}
