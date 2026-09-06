package com.study.vo;

import lombok.Data;
                                         
@Data
public class SubmitResultVO {
    private String subject;
    private Integer totalQuestions;
    private Integer correctCount;
    private Integer pointsEarned;
    private Integer newBalance;
}
