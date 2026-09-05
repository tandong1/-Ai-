package com.study.service;

import com.study.dto.SubmitAnswerDTO;
import com.study.vo.DailyQuestionsVO;
import com.study.vo.SubmitResultVO;

/**
 * 题目服务接口
 */
public interface QuestionService {

    /**
     * 获取今日题目（不含答案和解析）
     */
    DailyQuestionsVO getDailyQuestions(Long userId, String subject);

    /**
     * 提交答案并批改
     */
    SubmitResultVO submitAnswers(Long userId, SubmitAnswerDTO submitDTO);
}
