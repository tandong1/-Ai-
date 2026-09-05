package com.study.vo;

import lombok.Data;
import java.util.List;

/**
 * 题目VO（不含答案和解析）
 */
@Data
public class QuestionVO {
    private Long id;
    private String questionType;
    private String questionText;
    private List<String> options;
    private String difficulty;
    private String knowledgePoint;
}
