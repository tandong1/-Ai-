package com.study.vo;

import lombok.Data;

/**
 * 学习统计VO，成就判定的输入
 */
@Data
public class LearningStatsVO {

    /**
     * 累计答题天数（去重日期数）
     */
    private Integer totalDays;

    /**
     * 累计答题数
     */
    private Integer totalQuestions;

    /**
     * 是否有过「单次全对」（某个日期+科目正确率100%）
     */
    private Boolean hasPerfect;

    /**
     * 最长连续答题天数（历史最高，达成后不会因断签回落）
     */
    private Integer maxStreak;

    /**
     * 当前连续答题天数（以今天或昨天结尾才算在延续中）
     */
    private Integer currentStreak;

    /**
     * 累计积分
     */
    private Integer totalPoints;
}
