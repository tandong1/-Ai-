package com.study.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.study.entity.Achievement;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Insert;

/**
 * 成就Mapper
 */
@Mapper
public interface AchievementMapper extends BaseMapper<Achievement> {

    /**
     * 幂等插入成就解锁记录
     * 依赖 achievements 表的 UNIQUE KEY uk_user_type (user_id, achievement_type)：
     * 已解锁过则什么都不改，不会覆盖原始的 unlocked_at
     *
     * @return 实际插入行数，1表示本次新解锁，0表示早已解锁
     */
    @Insert("INSERT INTO achievements (user_id, achievement_type, achievement_name, achievement_desc, icon) " +
            "VALUES (#{userId}, #{type}, #{name}, #{desc}, #{icon}) " +
            "ON DUPLICATE KEY UPDATE id = id")
    int insertIfAbsent(@Param("userId") Long userId,
                       @Param("type") String type,
                       @Param("name") String name,
                       @Param("desc") String desc,
                       @Param("icon") String icon);
}
