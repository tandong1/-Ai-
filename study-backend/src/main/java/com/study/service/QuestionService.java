package com.study.service;

import com.study.dto.SubmitAnswerDTO;
import com.study.vo.CompletedQuestionsVO;
import com.study.vo.DailyProgressVO;
import com.study.vo.DailyQuestionsVO;
import com.study.vo.PageVO;
import com.study.vo.QuestionRecordVO;
import com.study.vo.RecordGroupVO;
import com.study.vo.RecordSummaryVO;
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

    /**
     * 分页获取答题历史（按「日期+科目」聚合，供历史列表下滑加载）
     */
    PageVO<RecordGroupVO> getRecordGroups(Long userId, String subject, int page, int pageSize);

    /**
     * 获取今日各科目完成状态
     */
    DailyProgressVO getDailyProgress(Long userId);

    /**
     * 获取答题历史总体统计
     */
    RecordSummaryVO getRecordSummary(Long userId, String subject);

    /**
     * 获取某一天某个科目的答题详情
     */
    CompletedQuestionsVO getDaySubjectRecords(Long userId, LocalDate date, String subject);
}
