package com.study.vo;

import lombok.Data;
import java.time.LocalDate;
import java.util.List;

/**
 * 已完成题目详情VO（做完题后显示）
 */
@Data
public class CompletedQuestionsVO {
    private String subject;
    private LocalDate completedDate;
    private List<CompletedQuestionDetailVO> questions;

    @Data
    public static class CompletedQuestionDetailVO {
        private Long questionId;
        private String questionType;
        private String questionText;
        private List<String> options;
        private String userAnswer;
        private String correctAnswer;
        private Boolean isCorrect;
        private String analysis;
        private Integer attemptCount;
        private String knowledgePoint;
    }
}
