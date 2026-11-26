package com.study.service;
import com.study.dto.GiftExchangeDTO;
import com.study.common.BusinessException;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import javax.sql.DataSource;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringJUnitConfig(GiftExchangeServiceTest.Config.class)
class GiftExchangeServiceTest {
 @Configuration @EnableTransactionManagement
 static class Config {
  @Bean DataSource dataSource() { var ds=new JdbcDataSource(); ds.setURL("jdbc:h2:mem:exchange;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1"); return ds; }
  @Bean JdbcTemplate jdbc(DataSource ds) { return new JdbcTemplate(ds); }
  @Bean DataSourceTransactionManager transactionManager(DataSource ds) { return new DataSourceTransactionManager(ds); }
  @Bean GiftExchangeService service(JdbcTemplate jdbc) { return new GiftExchangeService(jdbc); }
 }
 @Autowired JdbcTemplate jdbc;
 @Autowired GiftExchangeService service;
 @BeforeEach void setup() {
  jdbc.execute("DROP ALL OBJECTS");
  jdbc.execute("CREATE TABLE users(id BIGINT PRIMARY KEY,total_points INT,updated_at TIMESTAMP)");
  jdbc.execute("CREATE TABLE gifts(id BIGINT PRIMARY KEY,name VARCHAR(100),points_required INT,stock INT,is_active BOOLEAN,gift_type VARCHAR(20))");
  jdbc.execute("CREATE TABLE exchange_records(id BIGINT AUTO_INCREMENT PRIMARY KEY,user_id BIGINT,gift_id BIGINT,gift_name VARCHAR(100),points_cost INT,status VARCHAR(20),request_id VARCHAR(64),gift_type VARCHAR(20),recipient VARCHAR(80),contact_info VARCHAR(255),delivery_address TEXT,account VARCHAR(120),password_encrypted TEXT,game_region VARCHAR(100),game_name VARCHAR(100),remark VARCHAR(500),created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,UNIQUE(user_id,request_id))");
  jdbc.execute("CREATE TABLE points_records(user_id BIGINT,change_type VARCHAR(20),change_amount INT,source_type VARCHAR(20),source_id BIGINT,description VARCHAR(255),balance_after INT)");
  jdbc.update("INSERT INTO users VALUES(1,100,NULL),(2,100,NULL)");
  jdbc.update("INSERT INTO gifts VALUES(1,'礼物',30,1,TRUE,'physical')");
  ReflectionTestUtils.setField(service,"encryptionKey",Base64.getEncoder().encodeToString(new byte[32]));
 }
 GiftExchangeDTO request(String id) { var d=new GiftExchangeDTO(); d.giftId=1L; d.requestId=id; d.recipient="收货人";d.phone="13800000000";d.address="详细地址";return d; }
 int count(String table) { return jdbc.queryForObject("SELECT COUNT(*) FROM "+table,Integer.class); }
 int balance() { return jdbc.queryForObject("SELECT total_points FROM users WHERE id=1",Integer.class); }
 @Test void persistsBalanceStockOrderAndLedgerAndRetryIsIdempotent() {
  var result=service.exchange(1L,request("one"));
  assertEquals(70,result.get("totalPoints"));assertEquals(70,balance());
  assertEquals(0,jdbc.queryForObject("SELECT stock FROM gifts WHERE id=1",Integer.class));
  assertEquals(result,service.exchange(1L,request("one")));
  assertEquals(1,count("exchange_records"));assertEquals(1,count("points_records"));
  assertTrue(service.records(2L).isEmpty());assertEquals(1,service.records(1L).size());
 }
 @Test void insufficientPointsOrMissingAddressOrOutOfStockNeverCharge() {
  var d=request("a");d.address=" ";assertThrows(BusinessException.class,()->service.exchange(1L,d));
  jdbc.update("UPDATE gifts SET points_required=101");assertThrows(BusinessException.class,()->service.exchange(1L,request("b")));
  jdbc.update("UPDATE gifts SET stock=0,points_required=30");assertThrows(BusinessException.class,()->service.exchange(1L,request("c")));
  assertEquals(100,balance());assertEquals(0,count("exchange_records"));
 }
 @Test void ledgerFailureRollsBackEverything() {
  jdbc.execute("ALTER TABLE points_records ADD CONSTRAINT reject_ledger CHECK(change_amount<0)");
  assertThrows(Exception.class,()->service.exchange(1L,request("rollback")));
  assertEquals(100,balance());assertEquals(0,count("exchange_records"));
  assertEquals(1,jdbc.queryForObject("SELECT stock FROM gifts",Integer.class));
 }
 @Test void virtualPasswordEncryptedAndNeverReturned() {
  jdbc.update("UPDATE gifts SET gift_type='virtual',stock=-1");
  var d=request("virtual");d.account="account";d.password="secret-password";d.gameName="游戏";d.region="一区";
  service.exchange(1L,d);
  String encrypted=jdbc.queryForObject("SELECT password_encrypted FROM exchange_records",String.class);
  assertNotEquals(d.password,encrypted);assertTrue(encrypted.contains(":"));
  assertFalse(service.records(1L).get(0).containsKey("password_encrypted"));
  assertFalse(service.records(1L).toString().contains(d.password));
 }
 @Test void missingEncryptionKeyDoesNotCharge() {
  jdbc.update("UPDATE gifts SET gift_type='virtual'");ReflectionTestUtils.setField(service,"encryptionKey","");
  var d=request("virtual");d.account="account";d.password="secret";d.gameName="游戏";
  assertThrows(BusinessException.class,()->service.exchange(1L,d));assertEquals(100,balance());assertEquals(0,count("exchange_records"));
 }
 @Test void concurrentOrdersCannotOversell() throws Exception {
  var executor=Executors.newFixedThreadPool(2);
  try {
   Callable<Boolean> a=()-> {try {service.exchange(1L,request("a"));return true;}catch(BusinessException e){return false;}};
   Callable<Boolean> b=()-> {try {service.exchange(2L,request("b"));return true;}catch(BusinessException e){return false;}};
   var results=executor.invokeAll(List.of(a,b));
   int successes=0;for(var r:results) if(r.get())successes++;
   assertEquals(1,successes);assertEquals(1,count("exchange_records"));assertEquals(1,count("points_records"));
  } finally {executor.shutdownNow();}
 }
}
