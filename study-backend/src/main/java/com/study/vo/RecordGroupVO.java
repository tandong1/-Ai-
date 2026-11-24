package com.study.vo;

import lombok.Data;

import java.time.LocalDate;

/**
 * 答题记录分组VO（按日期+科目聚合，用于历史列表）
 */
@Data
public class RecordGroupVO {
    private Integer earnedPoints;

    /**
     * 答题日期
     */
    private LocalDate recordDate;

    /**
     * 科目: math/english/chinese
     */
    private String subject;

    /**
     * 当日该科目总题数
     */
    private Integer totalQuestions;

    /**
     * 当日该科目正确题数
     */
    private Integer correctCount;
}
