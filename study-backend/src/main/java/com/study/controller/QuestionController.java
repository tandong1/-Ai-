package com.study.controller;

import com.study.common.Result;
import com.study.dto.SubmitAnswerDTO;
import com.study.service.QuestionService;
import com.study.vo.DailyQuestionsVO;
import com.study.vo.SubmitResultVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

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
     * 获取今日题目
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
     * 提交答案
     */
    @PostMapping("/submit")
    public Result<SubmitResultVO> submitAnswers(
            @RequestAttribute("userId") Long userId,
            @RequestBody @Validated SubmitAnswerDTO submitDTO) {
        SubmitResultVO result = questionService.submitAnswers(userId, submitDTO);
        return Result.success(result);
    }
}
