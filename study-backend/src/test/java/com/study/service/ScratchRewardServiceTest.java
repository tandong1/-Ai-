package com.study.service;

import com.study.common.BusinessException;
import com.study.mapper.QuestionRecordMapper;
import com.study.vo.ScratchRewardVO;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import javax.sql.DataSource;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringJUnitConfig(ScratchRewardServiceTest.Config.class)
class ScratchRewardServiceTest {
    @Configuration
    @EnableTransactionManagement
    static class Config {
        @Bean DataSource dataSource() {
            JdbcDataSource source = new JdbcDataSource();
            source.setURL("jdbc:h2:mem:scratch;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1");
            return source;
        }
        @Bean JdbcTemplate jdbc(DataSource source) { return new JdbcTemplate(source); }
        @Bean DataSourceTransactionManager transactionManager(DataSource source) { return new DataSourceTransactionManager(source); }
        @Bean QuestionRecordMapper records() { return mock(QuestionRecordMapper.class); }
        @Bean AchievementService achievements() { return mock(AchievementService.class); }
        @Bean ScratchRewardService service(JdbcTemplate jdbc, QuestionRecordMapper records, AchievementService achievements) {
            return new ScratchRewardService(jdbc, records, achievements);
        }
    }
    @Autowired JdbcTemplate jdbc;
    @Autowired ScratchRewardService service;
    @Autowired QuestionRecordMapper records;
    @Autowired AchievementService achievements;

    @BeforeEach void setup() {
        reset(records, achievements);
        jdbc.execute("DROP ALL OBJECTS");
        jdbc.execute("CREATE TABLE users (id BIGINT PRIMARY KEY, total_points INT, updated_at TIMESTAMP)");
        jdbc.execute("CREATE TABLE scratch_config (id INT PRIMARY KEY, win_probability INT, reward_points INT)");
        jdbc.execute("CREATE TABLE daily_scratch_rewards (id BIGINT AUTO_INCREMENT PRIMARY KEY, user_id BIGINT, reward_date DATE, points INT, message VARCHAR(255), UNIQUE(user_id, reward_date))");
        jdbc.execute("CREATE TABLE points_records (user_id BIGINT, change_type VARCHAR(20), change_amount INT, source_type VARCHAR(20), description VARCHAR(255), balance_after INT)");
        jdbc.execute("CREATE TABLE questions (id BIGINT PRIMARY KEY, user_id BIGINT, subject VARCHAR(20), generated_date DATE, is_used BOOLEAN)");
        jdbc.execute("CREATE TABLE question_records (user_id BIGINT, question_id BIGINT, subject VARCHAR(20), user_answer VARCHAR(255), answered_at TIMESTAMP)");
        jdbc.update("INSERT INTO users (id,total_points) VALUES (1,10),(2,20)");
        jdbc.update("INSERT INTO scratch_config VALUES (1,10000,2)");
        jdbc.execute("CREATE TABLE scratch_messages (id BIGINT AUTO_INCREMENT PRIMARY KEY, message VARCHAR(255), enabled BOOLEAN DEFAULT TRUE)");
        jdbc.update("INSERT INTO scratch_messages (message,enabled) VALUES ('今天很棒，明天再来！',TRUE)");
        when(records.selectCompletedSubjects(anyLong(), any(LocalDate.class))).thenReturn(List.of("math", "english", "chinese"));
    }
    private LocalDate today() {
        return jdbc.queryForObject("SELECT CURDATE()", (rs, row) -> rs.getDate(1).toLocalDate());
    }
    @Test void unfinishedCannotDrawAndStatusDoesNotCreateReward() {
        when(records.selectCompletedSubjects(eq(1L), any(LocalDate.class))).thenReturn(List.of("math", "english"));
        assertFalse(service.status(1L).isEligible());
        assertThrows(BusinessException.class, () -> service.reveal(1L, today()));
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM daily_scratch_rewards", Integer.class));
    }
    @Test void winningDrawIsPersistentAndCreditsOnlyOnce() {
        assertFalse(service.status(1L).isRevealed());
        ScratchRewardVO first = service.reveal(1L, today());
        assertEquals(2, first.getPoints());
        assertEquals(12, first.getTotalPoints());
        assertNotNull(first.getMessage());
        jdbc.update("UPDATE scratch_config SET win_probability=0, reward_points=9");
        ScratchRewardVO repeated = service.reveal(1L, today());
        assertEquals(first.getMessage(), repeated.getMessage());
        assertEquals(2, repeated.getPoints());
        assertEquals(12, service.status(1L).getTotalPoints());
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM points_records", Integer.class));
    }
    @Test void configChangesApplyImmediatelyToNewDrawsAndZeroProbabilityStillHasMessage() {
        service.reveal(1L, today());
        jdbc.update("UPDATE scratch_config SET win_probability=0");
        ScratchRewardVO result = service.reveal(2L, today());
        assertTrue(result.isRevealed());
        assertEquals(0, result.getPoints());
        assertEquals(20, result.getTotalPoints());
        assertFalse(result.getMessage().isBlank());
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM points_records", Integer.class));
    }
    @Test void staleDateAndInvalidConfigurationAreRejected() {
        assertThrows(BusinessException.class, () -> service.reveal(1L, today().minusDays(1)));
        jdbc.update("UPDATE scratch_config SET win_probability=10001");
        assertThrows(BusinessException.class, () -> service.reveal(1L, today()));
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM daily_scratch_rewards", Integer.class));
    }
    @Test void settlementFailureRollsBackRewardBalanceAndLedger() {
        when(achievements.evaluateAndUnlock(1L)).thenThrow(new IllegalStateException("test failure"));
        assertThrows(IllegalStateException.class, () -> service.reveal(1L, today()));
        assertFalse(service.status(1L).isRevealed());
        assertEquals(10, service.status(1L).getTotalPoints());
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM points_records", Integer.class));
    }
    @Test void databaseMessageEditsApplyToNewDrawsButPreserveOldResults() {
        ScratchRewardVO first = service.reveal(1L, today());
        assertEquals("今天很棒，明天再来！", first.getMessage());
        jdbc.update("UPDATE scratch_messages SET message='继续努力，每天进步！'");
        assertEquals("继续努力，每天进步！", service.reveal(2L, today()).getMessage());
        assertEquals(first.getMessage(), service.status(1L).getMessage());
        assertEquals(first.getMessage(), service.reveal(1L, today()).getMessage());
    }
    @Test void disabledAndBlankMessagesAreExcluded() {
        jdbc.update("UPDATE scratch_messages SET enabled=FALSE");
        jdbc.update("INSERT INTO scratch_messages (message,enabled) VALUES ('   ',TRUE),('新的鼓励语',TRUE)");
        assertEquals("新的鼓励语", service.reveal(1L, today()).getMessage());
    }
    @Test void noEnabledMessagesDoesNotConsumeRewardAndCanBeRetriedAfterConfiguration() {
        jdbc.update("UPDATE scratch_messages SET enabled=FALSE");
        assertThrows(BusinessException.class, () -> service.reveal(1L, today()));
        assertFalse(service.status(1L).isRevealed());
        assertEquals(10, service.status(1L).getTotalPoints());
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM points_records", Integer.class));
        jdbc.update("UPDATE scratch_messages SET enabled=TRUE");
        assertTrue(service.reveal(1L, today()).isRevealed());
    }
    private void useRealCompletionQuery() {
        when(records.selectCompletedSubjects(anyLong(), any(LocalDate.class))).thenAnswer(invocation ->
                jdbc.queryForList(QuestionRecordMapper.COMPLETED_SUBJECTS_SQL
                        .replace("#{userId}", "?").replace("#{date}", "?"),
                        String.class, invocation.getArgument(0), invocation.getArgument(1)));
    }
    private void question(long id, String subject) {
        jdbc.update("INSERT INTO questions VALUES (?,1,?,?,FALSE)", id, subject, today());
    }
    private void answer(long id, String subject, String answer) {
        jdbc.update("INSERT INTO question_records VALUES (1,?,?,?,CURRENT_TIMESTAMP)", id, subject, answer);
    }
    @Test void everyQuestionInEverySubjectMustBeAnsweredBeforeScratch() {
        useRealCompletionQuery();
        question(1, "math"); question(2, "math");
        question(3, "english"); question(4, "chinese");
        answer(1, "math", "1"); answer(3, "english", "a"); answer(4, "chinese", "字");
        // 三科都有记录仍缺一道数学题，不能刮奖。
        assertFalse(service.status(1L).isEligible());
        assertThrows(BusinessException.class, () -> service.reveal(1L, today()));
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM daily_scratch_rewards", Integer.class));
        answer(2, "math", "2");
        assertTrue(service.status(1L).isEligible());
        assertTrue(service.reveal(1L, today()).isRevealed());
    }
    @Test void duplicateBlankForeignAndOldAnswersCannotFillMissingQuestion() {
        useRealCompletionQuery();
        question(1, "math"); question(2, "math"); question(3, "english"); question(4, "chinese");
        answer(1, "math", "1"); answer(1, "math", "1");
        answer(3, "english", "a"); answer(4, "chinese", "字");
        answer(2, "math", "   "); answer(2, "math", null);
        answer(2, "english", "2");
        jdbc.update("INSERT INTO question_records VALUES (2,2,'math','2',CURRENT_TIMESTAMP)");
        jdbc.update("INSERT INTO question_records VALUES (1,2,'math','2',?)", today().minusDays(1).atStartOfDay());
        jdbc.update("UPDATE questions SET is_used=TRUE");
        assertFalse(service.status(1L).isEligible());
        assertThrows(BusinessException.class, () -> service.reveal(1L, today()));
    }
    @Test void missingSubjectAndOtherDatesQuestionsDoNotQualify() {
        useRealCompletionQuery();
        question(1, "math"); question(2, "english");
        answer(1, "math", "1"); answer(2, "english", "a");
        jdbc.update("INSERT INTO questions VALUES (3,1,'chinese',?,TRUE)", today().minusDays(1));
        answer(3, "chinese", "字");
        assertFalse(service.status(1L).isEligible());
        jdbc.update("INSERT INTO questions VALUES (4,2,'chinese',?,TRUE)", today());
        answer(4, "chinese", "字");
        assertFalse(service.status(1L).isEligible());
        assertThrows(BusinessException.class, () -> service.reveal(1L, today()));
    }
    @Test void concurrentDrawsReturnSameRewardAndCreditOnce() throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);
        LocalDate date = today();
        try {
            Callable<ScratchRewardVO> draw = () -> { start.await(); return service.reveal(1L, date); };
            Future<ScratchRewardVO> a = pool.submit(draw);
            Future<ScratchRewardVO> b = pool.submit(draw);
            start.countDown();
            assertEquals(a.get(10, TimeUnit.SECONDS).getMessage(), b.get(10, TimeUnit.SECONDS).getMessage());
            assertEquals(12, service.status(1L).getTotalPoints());
            assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM daily_scratch_rewards", Integer.class));
            assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM points_records", Integer.class));
        } finally { pool.shutdownNow(); }
    }
}
