package com.study.service;

import com.study.dto.SubmitAnswerDTO;
import com.study.entity.Question;
import com.study.entity.QuestionRecord;
import com.study.entity.User;
import com.study.mapper.QuestionMapper;
import com.study.mapper.QuestionRecordMapper;
import com.study.mapper.UserMapper;
import com.study.service.impl.QuestionServiceImpl;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import java.time.LocalDate;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class QuestionRewardServiceTest {
    JdbcTemplate jdbc;
    QuestionServiceImpl service;
    UserService users;
    com.study.mapper.PointsRecordMapper points;
    LocalDate date = LocalDate.of(2026, 11, 24);

    @BeforeEach void setup() {
        JdbcDataSource source = new JdbcDataSource();
        source.setURL("jdbc:h2:mem:question_reward;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1");
        jdbc = new JdbcTemplate(source);
        jdbc.execute("DROP ALL OBJECTS");
        jdbc.execute("CREATE TABLE questions (id BIGINT, user_id BIGINT, subject VARCHAR(20), generated_date DATE)");
        jdbc.execute("CREATE TABLE question_records (question_id BIGINT, user_id BIGINT, subject VARCHAR(20), user_answer VARCHAR(255), first_attempt_correct BOOLEAN, answered_at TIMESTAMP)");
        jdbc.update("INSERT INTO questions VALUES (1,1,'math',?),(2,1,'math',?)", date, date);
        UserMapper userMapper = mock(UserMapper.class);
        User user = new User(); user.setId(1L); user.setTotalPoints(10);
        when(userMapper.selectForUpdate(1L)).thenReturn(user);
        QuestionMapper questionMapper = mock(QuestionMapper.class);
        when(questionMapper.selectById(anyLong())).thenAnswer(call -> {
            Question q = new Question(); q.setId(call.getArgument(0)); q.setCorrectAnswer("A");
            return q;
        });
        QuestionRecordMapper records = mock(QuestionRecordMapper.class);
        when(records.selectDatabaseToday()).thenReturn(date);
        when(records.insert(any(QuestionRecord.class))).thenAnswer(call -> {
            QuestionRecord r = call.getArgument(0);
            return jdbc.update("INSERT INTO question_records VALUES (?,?,?,?,?,?)", r.getQuestionId(), r.getUserId(),
                    r.getSubject(), r.getUserAnswer(), r.getFirstAttemptCorrect(), date.atStartOfDay());
        });
        when(records.selectFirstTryPerfectSubjects(eq(1L), eq(date))).thenAnswer(call ->
                jdbc.queryForList(QuestionRecordMapper.FIRST_TRY_PERFECT_SQL
                        .replace("#{userId}", "?").replace("#{date}", "?"), String.class, 1L, date));
        users = mock(UserService.class);
        points = mock(com.study.mapper.PointsRecordMapper.class);
        service = new QuestionServiceImpl(userMapper, points, questionMapper, records, users, mock(AchievementService.class));
    }
    SubmitAnswerDTO.AnswerItem answer(long id, String value, Integer attempts) {
        SubmitAnswerDTO.AnswerItem a = new SubmitAnswerDTO.AnswerItem();
        a.setQuestionId(id); a.setUserAnswer(value); a.setAttemptCount(attempts); return a;
    }
    void assertReward(int points, SubmitAnswerDTO.AnswerItem... answers) {
        SubmitAnswerDTO dto = new SubmitAnswerDTO(); dto.setSubject("math"); dto.setAnswers(List.of(answers));
        var result = service.submitAnswers(1L, dto);
        assertEquals(points, result.getPointsEarned());
        assertEquals(10 + points, result.getNewBalance());
        verify(users).updateUserPoints(1L, 10 + points);
        var ledger = org.mockito.ArgumentCaptor.forClass(com.study.entity.PointsRecord.class);
        verify(this.points).insert(ledger.capture());
        assertEquals(points, ledger.getValue().getChangeAmount());
        assertEquals(10 + points, ledger.getValue().getBalanceAfter());
        assertEquals("daily_challenge", ledger.getValue().getSourceType());
    }
    @Test void allQuestionsCorrectFirstTryAwardsTwo() {
        assertReward(2, answer(1,"A",1), answer(2,"A",1));
    }
    @Test void retryEvenIfFinallyCorrectAwardsOne() {
        assertReward(1, answer(1,"A",2), answer(2,"A",1));
    }
    @Test void wrongAnswerAwardsOne() {
        assertReward(1, answer(1,"B",1), answer(2,"A",1));
    }
    @Test void partialSubmissionCannotEarnPerfectBonus() {
        assertReward(1, answer(1,"A",1));
    }
    @Test void duplicateAnswerCannotReplaceMissingQuestion() {
        assertReward(1, answer(1,"A",1), answer(1,"A",1));
    }
    @Test void missingAttemptCountDoesNotQualifyForPerfectBonus() {
        assertReward(1, answer(1,"A",null), answer(2,"A",1));
    }
    @Test void earlierWrongAnswerCannotBeOverwrittenByLaterCorrectSubmission() {
        jdbc.update("INSERT INTO question_records VALUES (1,1,'math','B',FALSE,?)",date.atStartOfDay());
        assertReward(1, answer(1,"A",1), answer(2,"A",1));
    }
}
