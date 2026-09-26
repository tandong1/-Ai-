package com.study.controller;

import com.study.common.Result;
import com.study.dto.SubmitAnswerDTO;
import com.study.service.QuestionService;
import com.study.vo.CompletedQuestionsVO;
import com.study.vo.DailyQuestionsVO;
import com.study.vo.PageVO;
import com.study.vo.QuestionRecordVO;
import com.study.vo.RecordGroupVO;
import com.study.vo.RecordSummaryVO;
import com.study.vo.SubmitResultVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * 题目控制器
 */
@Slf4j
@RestController
@RequestMapping("/questions")
@RequiredArgsConstructor
public class QuestionController {

    private final QuestionService questionService;

    /**
     * 获取今日题目（未完成时）
     * @param subject 科目: math/english/chinese
     */
    @GetMapping("/daily")
    public Result<DailyQuestionsVO> getDailyQuestions(
            @RequestAttribute("userId") Long userId,
            @RequestParam String subject) {
        DailyQuestionsVO questions = questionService.getDailyQuestions(userId, subject);
        return Result.success(questions);
    }

    /**
     * 获取今日已完成题目详情（做完题后查看）
     * @param subject 科目: math/english/chinese
     */
    @GetMapping("/daily/completed")
    public Result<CompletedQuestionsVO> getTodayCompletedQuestions(
            @RequestAttribute("userId") Long userId,
            @RequestParam String subject) {
        CompletedQuestionsVO questions = questionService.getTodayCompletedQuestions(userId, subject);
        return Result.success(questions);
    }

    /**
     * 提交答案
     */
    @PostMapping("/submit")
    public Result<SubmitResultVO> submitAnswers(
            @RequestAttribute("userId") Long userId,
            @RequestBody @Validated SubmitAnswerDTO submitDTO) {
        SubmitResultVO result = questionService.submitAnswers(userId, submitDTO);
        return Result.success(result);
    }

    /**
     * 获取答题历史记录
     * @param subject 科目（可选）
     * @param startDate 开始日期（可选）
     * @param endDate 结束日期（可选）
     */
    @GetMapping("/records")
    public Result<List<QuestionRecordVO>> getQuestionRecords(
            @RequestAttribute("userId") Long userId,
            @RequestParam(required = false) String subject,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        List<QuestionRecordVO> records = questionService.getQuestionRecords(userId, subject, startDate, endDate);
        return Result.success(records);
    }

    /**
     * 分页获取答题历史（按「日期+科目」聚合，供历史列表下滑加载）
     * @param subject 科目（可选）
     * @param page 页码，从1开始
     * @param pageSize 每页条数，最大50
     */
    @GetMapping("/records/groups")
    public Result<PageVO<RecordGroupVO>> getRecordGroups(
            @RequestAttribute("userId") Long userId,
            @RequestParam(required = false) String subject,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        PageVO<RecordGroupVO> groups = questionService.getRecordGroups(userId, subject, page, pageSize);
        return Result.success(groups);
    }

    /**
     * 获取答题历史总体统计（累计天数、总题数、平均正确率）
     * @param subject 科目（可选）
     */
    @GetMapping("/records/summary")
    public Result<RecordSummaryVO> getRecordSummary(
            @RequestAttribute("userId") Long userId,
            @RequestParam(required = false) String subject) {
        RecordSummaryVO summary = questionService.getRecordSummary(userId, subject);
        return Result.success(summary);
    }

    /**
     * 获取某一天某个科目的答题详情
     * @param date 日期
     * @param subject 科目: math/english/chinese
     */
    @GetMapping("/records/detail")
    public Result<CompletedQuestionsVO> getDaySubjectRecords(
            @RequestAttribute("userId") Long userId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam String subject) {
        CompletedQuestionsVO detail = questionService.getDaySubjectRecords(userId, date, subject);
        return Result.success(detail);
    }
}
