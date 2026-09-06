package com.study.service;

import com.study.dto.SubmitAnswerDTO;
import com.study.vo.CompletedQuestionsVO;
import com.study.vo.DailyQuestionsVO;
import com.study.vo.QuestionRecordVO;
import com.study.vo.SubmitResultVO;

import java.time.LocalDate;
import java.util.List;

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

    /**
     * 获取今日已完成题目详情（含答案和解析）
     */
    CompletedQuestionsVO getTodayCompletedQuestions(Long userId, String subject);

    /**
     * 获取答题历史记录
     */
    List<QuestionRecordVO> getQuestionRecords(Long userId, String subject, LocalDate startDate, LocalDate endDate);
}
