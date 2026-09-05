package com.study.vo;

import lombok.Data;
import java.time.LocalDate;
import java.util.List;

/**
 * 今日题目VO
 */
@Data
public class DailyQuestionsVO {
    private String subject;
    private LocalDate generatedDate;
    private List<QuestionVO> questions;
}
