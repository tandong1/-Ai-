package com.study.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.study.entity.QuestionRecord;
import com.study.vo.RecordGroupVO;
import com.study.vo.RecordSummaryVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.util.List;

/**
 * 答题记录Mapper
 */
@Mapper
public interface QuestionRecordMapper extends BaseMapper<QuestionRecord> {

    /**
     * 分页查询「日期+科目」维度的答题分组，按日期倒序
     * 聚合下推到数据库，避免把全部逐题记录读到内存
     */
    @Select("<script>" +
            "SELECT DATE(answered_at) AS record_date, subject, " +
            "       COUNT(*) AS total_questions, " +
            "       SUM(CASE WHEN is_correct = 1 THEN 1 ELSE 0 END) AS correct_count " +
            "FROM question_records " +
            "WHERE user_id = #{userId} " +
            "<if test='subject != null and subject != \"\"'> AND subject = #{subject} </if>" +
            "GROUP BY DATE(answered_at), subject " +
            "ORDER BY record_date DESC, subject ASC " +
            "LIMIT #{offset}, #{size}" +
            "</script>")
    List<RecordGroupVO> selectRecordGroups(@Param("userId") Long userId,
                                           @Param("subject") String subject,
                                           @Param("offset") int offset,
                                           @Param("size") int size);

    /**
     * 统计「日期+科目」分组总数，用于分页的 total / hasMore
     */
    @Select("<script>" +
            "SELECT COUNT(*) FROM ( " +
            "  SELECT 1 FROM question_records " +
            "  WHERE user_id = #{userId} " +
            "  <if test='subject != null and subject != \"\"'> AND subject = #{subject} </if>" +
            "  GROUP BY DATE(answered_at), subject " +
            ") g" +
            "</script>")
    long countRecordGroups(@Param("userId") Long userId,
                           @Param("subject") String subject);

    /**
     * 答题历史总体统计（累计天数、总题数、正确数、平均正确率）
     * 平均正确率取各「日期+科目」正确率的平均值，与列表页每张卡片的口径一致
     */
    @Select("<script>" +
            "SELECT COALESCE(COUNT(DISTINCT g.record_date), 0) AS total_days, " +
            "       COALESCE(SUM(g.total_questions), 0)        AS total_questions, " +
            "       COALESCE(SUM(g.correct_count), 0)          AS total_correct, " +
            "       COALESCE(ROUND(AVG(g.acc)), 0)             AS avg_accuracy, " +
            "       COALESCE(MAX(g.acc = 100), 0)              AS has_perfect " +
            "FROM ( " +
            "  SELECT DATE(answered_at) AS record_date, " +
            "         COUNT(*) AS total_questions, " +
            "         SUM(CASE WHEN is_correct = 1 THEN 1 ELSE 0 END) AS correct_count, " +
            "         SUM(CASE WHEN is_correct = 1 THEN 1 ELSE 0 END) * 100.0 / COUNT(*) AS acc " +
            "  FROM question_records " +
            "  WHERE user_id = #{userId} " +
            "  <if test='subject != null and subject != \"\"'> AND subject = #{subject} </if>" +
            "  GROUP BY DATE(answered_at), subject " +
            ") g" +
            "</script>")
    RecordSummaryVO selectRecordSummary(@Param("userId") Long userId,
                                        @Param("subject") String subject);

    /**
     * 查询用户所有答题日期（去重、升序）
     * 行数上限就是用户使用过的天数（几百行量级），连续天数在Java里算，
     * 避免在SQL里写可读性很差的 gaps-and-islands
     */
    @Select("SELECT DISTINCT DATE(answered_at) FROM question_records " +
            "WHERE user_id = #{userId} ORDER BY 1")
    List<LocalDate> selectDistinctAnswerDates(@Param("userId") Long userId);

    /**
     * 当天有题且每道题都有该用户当天的非空作答，才算科目完成。
     * EXISTS 按题目ID匹配，重复作答不抵扣缺题，is_used 标记不作为完成依据。
     */
    String COMPLETED_SUBJECTS_SQL =
            "SELECT DISTINCT q.subject FROM questions q " +
            "WHERE q.user_id = #{userId} AND q.generated_date = #{date} " +
            "AND q.subject IN ('math', 'english', 'chinese') " +
            "AND NOT EXISTS (SELECT 1 FROM questions pending " +
            "  WHERE pending.user_id = q.user_id AND pending.subject = q.subject " +
            "  AND pending.generated_date = q.generated_date " +
            "  AND NOT EXISTS (SELECT 1 FROM question_records r " +
            "    WHERE r.user_id = pending.user_id AND r.question_id = pending.id " +
            "    AND r.subject = pending.subject AND DATE(r.answered_at) = pending.generated_date " +
            "    AND r.user_answer IS NOT NULL AND CHAR_LENGTH(TRIM(r.user_answer)) > 0))";

    @Select(COMPLETED_SUBJECTS_SQL)
    List<String> selectCompletedSubjects(@Param("userId") Long userId, @Param("date") LocalDate date);

    // 所有题首次答对，且没有任何重试或错误记录，才享受全对奖励。
    String FIRST_TRY_PERFECT_SQL = "SELECT DISTINCT q.subject FROM questions q " +
            "WHERE q.user_id = #{userId} AND q.generated_date = #{date} " +
            "AND q.subject IN ('math', 'english', 'chinese') " +
            "AND NOT EXISTS (SELECT 1 FROM questions pending " +
            "  WHERE pending.user_id = q.user_id AND pending.subject = q.subject " +
            "  AND pending.generated_date = q.generated_date " +
            "  AND NOT EXISTS (SELECT 1 FROM question_records r " +
            "    WHERE r.user_id = pending.user_id AND r.question_id = pending.id " +
            "    AND r.subject = pending.subject AND DATE(r.answered_at) = pending.generated_date " +
            "    AND r.first_attempt_correct = TRUE AND r.user_answer IS NOT NULL AND CHAR_LENGTH(TRIM(r.user_answer)) > 0))" +
            " AND NOT EXISTS (SELECT 1 FROM questions asked JOIN question_records previous " +
            "ON previous.question_id = asked.id AND previous.user_id = asked.user_id " +
            "WHERE asked.user_id = q.user_id AND asked.subject = q.subject " +
            "AND asked.generated_date = q.generated_date AND DATE(previous.answered_at) = q.generated_date " +
            "AND (previous.first_attempt_correct IS NULL OR previous.first_attempt_correct = FALSE))";

    @Select(FIRST_TRY_PERFECT_SQL)
    List<String> selectFirstTryPerfectSubjects(@Param("userId") Long userId, @Param("date") LocalDate date);

    @Select("SELECT CURDATE()")
    LocalDate selectDatabaseToday();
}
