package com.study.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 单个成就VO
 */
@Data
public class AchievementVO {

    /**
     * 成就类型
     */
    private String type;

    /**
     * 成就名称
     */
    private String name;

    /**
     * 成就描述
     */
    private String desc;

    /**
     * 图标emoji
     */
    private String icon;

    /**
     * 是否已解锁
     */
    private Boolean unlocked;

    /**
     * 解锁时间，未解锁为null
     */
    private LocalDateTime unlockedAt;
}
