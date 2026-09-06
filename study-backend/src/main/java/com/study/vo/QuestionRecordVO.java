package com.study.vo;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 答题记录VO
 */
@Data
public class QuestionRecordVO {
    private Long id;
    private Long questionId;
    private String subject;
    private String questionType;
    private String questionText;
    private List<String> options;
    private String userAnswer;
    private String correctAnswer;
    private Boolean isCorrect;
    private String analysis;
    private Integer attemptCount;
    private Boolean firstAttemptCorrect;
    private LocalDateTime answeredAt;
}
