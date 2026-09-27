package com.study.vo;

import lombok.Data;

/**
 * 答题历史总体统计VO
 */
@Data
public class RecordSummaryVO {

    /**
     * 累计天数（去重后的答题日期数）
     */
    private Integer totalDays;

    /**
     * 累计总题数
     */
    private Integer totalQuestions;

    /**
     * 累计正确题数
     */
    private Integer totalCorrect;

    /**
     * 平均正确率（各「日期+科目」正确率的平均值，百分比整数）
     */
    private Integer avgAccuracy;

    /**
     * 是否有过「单次全对」（某个日期+科目正确率100%）
     */
    private Boolean hasPerfect;
}
