package com.study.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.study.entity.PointsRecord;
import org.apache.ibatis.annotations.Mapper;

/**
 * 积分记录Mapper
 */
@Mapper
public interface PointsRecordMapper extends BaseMapper<PointsRecord> {
    @org.apache.ibatis.annotations.Select("SELECT COALESCE(SUM(p.change_amount), 1) FROM points_records p " +
            "JOIN question_records r ON r.id = p.source_id AND r.user_id = p.user_id " +
            "WHERE p.user_id = #{userId} AND p.source_type = 'daily_challenge' " +
            "AND r.subject = #{subject} AND DATE(r.answered_at) = #{date}")
    Integer selectDailyChallengePoints(@org.apache.ibatis.annotations.Param("userId") Long userId,
            @org.apache.ibatis.annotations.Param("date") java.time.LocalDate date,
            @org.apache.ibatis.annotations.Param("subject") String subject);

}
