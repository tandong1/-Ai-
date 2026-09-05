package com.study.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 答题结果VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnswerResultVO {
    private Long questionId;
    private Boolean isCorrect;
    private String userAnswer;
    private String correctAnswer;
    private String analysis;
    private Integer pointsEarned;
}
