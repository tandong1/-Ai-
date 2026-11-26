package com.study.service;

import com.study.common.BusinessException;
import com.study.dto.GiftExchangeDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;
import java.util.*;

@Service @RequiredArgsConstructor
public class GiftExchangeService {
 private final JdbcTemplate jdbc;
 @Value("${exchange.encryption-key:}") private String encryptionKey;
 private String text(String s) { return s == null ? "" : s.trim(); }
 private void required(String s, String name) {
  if (text(s).isEmpty()) throw new BusinessException("请填写" + name);
 }
 private String encrypt(String password) {
  if (text(password).isEmpty()) return null;
  try {
   byte[] key = Base64.getDecoder().decode(encryptionKey);
   if (key.length != 32) throw new IllegalArgumentException();
   byte[] iv = new byte[12]; new SecureRandom().nextBytes(iv);
   Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
   cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "AES"), new GCMParameterSpec(128, iv));
   return Base64.getEncoder().encodeToString(iv) + ":" + Base64.getEncoder().encodeToString(cipher.doFinal(password.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
  } catch (Exception e) { throw new BusinessException("虚拟商品密码存储尚未配置，请联系管理员"); }
 }
 @Transactional(rollbackFor=Exception.class)
 public Map<String,Object> exchange(Long userId, GiftExchangeDTO d) {
  List<Integer> balances = jdbc.query("SELECT total_points FROM users WHERE id=? FOR UPDATE", (rs,n)->rs.getInt(1), userId);
  if (balances.isEmpty()) throw new BusinessException("用户不存在");
  List<Long> previous = jdbc.queryForList("SELECT id FROM exchange_records WHERE user_id=? AND request_id=?", Long.class,userId,d.requestId);
  if (!previous.isEmpty()) return Map.of("id",previous.get(0),"totalPoints",balances.get(0));
  List<Map<String,Object>> gifts = jdbc.queryForList("SELECT * FROM gifts WHERE id=? FOR UPDATE",d.giftId);
  if (gifts.isEmpty()) throw new BusinessException("礼物不存在");
  Object active = gifts.get(0).get("is_active");
  if (!Boolean.TRUE.equals(active) && !(active instanceof Number && ((Number) active).intValue() == 1)) {
   throw new BusinessException("礼物已下架");
  }
  Map<String,Object> gift = gifts.get(0);
  int cost=((Number)gift.get("points_required")).intValue(), stock=((Number)gift.get("stock")).intValue();
  if (cost<1) throw new BusinessException("礼物积分配置有误");
  if (stock==0 || stock < -1) throw new BusinessException("库存不足");
  if (balances.get(0)<cost) throw new BusinessException("积分不足");
  String type=String.valueOf(gift.get("gift_type"));
  if ("physical".equals(type)) { required(d.recipient,"收货人"); required(d.phone,"联系电话"); required(d.address,"收货地址"); }
  else if ("virtual".equals(type)) { required(d.account,"账号"); required(d.gameName,"游戏名或平台名"); }
  else throw new BusinessException("礼物类型配置有误");
  String secret="virtual".equals(type)?encrypt(d.password):null;
  jdbc.update("INSERT INTO exchange_records (user_id,gift_id,gift_name,points_cost,status,request_id,gift_type,recipient,contact_info,delivery_address,account,password_encrypted,game_region,game_name,remark) VALUES (?,?,?,?,'pending',?,?,?,?,?,?,?,?,?,?)",
   userId,d.giftId,gift.get("name"),cost,d.requestId,type,"physical".equals(type)?text(d.recipient):null,"physical".equals(type)?text(d.phone):null,"physical".equals(type)?text(d.address):null,"virtual".equals(type)?text(d.account):null,secret,"virtual".equals(type)?text(d.region):null,"virtual".equals(type)?text(d.gameName):null,text(d.remark));
  Long id=jdbc.queryForObject("SELECT id FROM exchange_records WHERE user_id=? AND request_id=?",Long.class,userId,d.requestId);
  int balance=balances.get(0)-cost;
  jdbc.update("UPDATE users SET total_points=?,updated_at=NOW() WHERE id=?",balance,userId);
  if(stock>0) jdbc.update("UPDATE gifts SET stock=stock-1 WHERE id=?",d.giftId);
  jdbc.update("INSERT INTO points_records (user_id,change_type,change_amount,source_type,source_id,description,balance_after) VALUES (?,'consume',?,'exchange',?,'兑换礼物',?)",userId,cost,id,balance);
  return Map.of("id",id,"totalPoints",balance);
 }
 public Map<String,Object> balance(Long userId) {
  return Map.of("totalPoints",jdbc.queryForObject("SELECT total_points FROM users WHERE id=?",Integer.class,userId));
 }
 public List<Map<String,Object>> records(Long userId) {
  return jdbc.queryForList("SELECT id,gift_name AS giftName,points_cost AS points,status,created_at AS time,gift_type AS giftType,recipient,contact_info AS phone,delivery_address AS address,account,game_region AS region,game_name AS gameName,remark FROM exchange_records WHERE user_id=? ORDER BY id DESC LIMIT 100",userId);
 }
}
