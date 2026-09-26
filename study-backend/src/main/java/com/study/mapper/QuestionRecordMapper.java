package com.study.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.study.entity.QuestionRecord;
import com.study.vo.RecordGroupVO;
import com.study.vo.RecordSummaryVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

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
            "       COALESCE(ROUND(AVG(g.acc)), 0)             AS avg_accuracy " +
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
}
