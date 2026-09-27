package com.study.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 成就实体
 */
@Data
@TableName("achievements")
public class Achievement implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 成就类型，与 AchievementDefinition 的 type 对应，(user_id, achievement_type) 唯一
     */
    private String achievementType;

    /**
     * 成就名称（解锁当时的快照，展示时以 AchievementDefinition 的当前文案为准）
     */
    private String achievementName;

    /**
     * 成就描述（快照）
     */
    private String achievementDesc;

    /**
     * 图标emoji（快照）
     */
    private String icon;

    /**
     * 解锁时间
     */
    private LocalDateTime unlockedAt;
}
