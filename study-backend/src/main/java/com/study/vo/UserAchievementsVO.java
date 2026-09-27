package com.study.vo;

import lombok.Data;

import java.util.List;

/**
 * 成就墙VO
 */
@Data
public class UserAchievementsVO {

    /**
     * 已解锁数量
     */
    private Integer unlockedCount;

    /**
     * 成就总数
     */
    private Integer totalCount;

    /**
     * 当前连续答题天数，用于未解锁的连续类成就展示进度
     */
    private Integer currentStreak;

    /**
     * 最长连续答题天数
     */
    private Integer maxStreak;

    /**
     * 累计答题数
     */
    private Integer totalQuestions;

    /**
     * 全部成就（含未解锁），顺序即展示顺序
     */
    private List<AchievementVO> achievements;
}
