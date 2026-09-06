package com.study.vo;

import lombok.Data;
import java.util.List;

@Data
public class SubmitResultVO {
    private String subject;
    private Integer totalQuestions;
    private Integer correctCount;
    private Integer pointsEarned;
    private Integer newBalance;
    private List<QuestionResultDetail> details;  // 每道题的详细结果

    @Data
    public static class QuestionResultDetail {
        private Long questionId;
        private String questionType;
        private String questionText;
        private List<String> options;
        private String userAnswer;
        private String correctAnswer;
        private Boolean isCorrect;
        private String analysis;
        private Integer attemptCount;
        private Integer pointsEarned;
        private String knowledgePoint;
    }
}
