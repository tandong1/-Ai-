package com.study.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.study.dto.SubmitAnswerDTO;
import com.study.entity.Question;
import com.study.entity.QuestionRecord;
import com.study.entity.User;
import com.study.common.BusinessException;
import com.study.mapper.QuestionMapper;
import com.study.mapper.QuestionRecordMapper;
import com.study.service.QuestionService;
import com.study.service.UserService;
import com.study.vo.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 题目服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class QuestionServiceImpl implements QuestionService {

    private final QuestionMapper questionMapper;
    private final QuestionRecordMapper questionRecordMapper;
    private final UserService userService;

    @Override
    public DailyQuestionsVO getDailyQuestions(Long userId, String subject) {
        log.info("获取今日题目: userId={}, subject={}", userId, subject);

        LocalDate today = LocalDate.now();

        LambdaQueryWrapper<Question> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Question::getUserId, userId)
                   .eq(Question::getSubject, subject)
                   .eq(Question::getGeneratedDate, today)
                   .eq(Question::getIsUsed, false)
                   .orderByAsc(Question::getId);

        List<Question> questions = questionMapper.selectList(queryWrapper);

        if (questions.isEmpty()) {
            throw new BusinessException("今日还没有生成题目，请稍后再试");
        }

        List<QuestionVO> questionVOs = questions.stream()
                .map(this::convertToQuestionVO)
                .collect(Collectors.toList());

        DailyQuestionsVO result = new DailyQuestionsVO();
        result.setSubject(subject);
        result.setGeneratedDate(today);
        result.setQuestions(questionVOs);

        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SubmitResultVO submitAnswers(Long userId, SubmitAnswerDTO submitDTO) {
        log.info("提交答案: userId={}, subject={}", userId, submitDTO.getSubject());

        User user = userService.getUserById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }

        int totalQuestions = submitDTO.getAnswers().size();
        int correctCount = 0;
        int pointsEarned = 0;

        for (SubmitAnswerDTO.AnswerItem answerItem : submitDTO.getAnswers()) {
            Question question = questionMapper.selectById(answerItem.getQuestionId());
            if (question == null) {
                throw new BusinessException("题目不存在: " + answerItem.getQuestionId());
            }

            Boolean isCorrect = answerItem.getIsCorrect();
            Integer attemptCount = answerItem.getAttemptCount();

            QuestionRecord record = new QuestionRecord();
            record.setUserId(userId);
            record.setQuestionId(question.getId());
            record.setSubject(submitDTO.getSubject());
            record.setUserAnswer(answerItem.getUserAnswer());
            record.setIsCorrect(isCorrect);
            record.setCorrectAnswer(question.getCorrectAnswer());
            record.setAnalysis(question.getAnalysis());
            record.setAttemptCount(attemptCount);
            record.setFirstAttemptCorrect(attemptCount != null && attemptCount == 1 && isCorrect);

            questionRecordMapper.insert(record);

            if (isCorrect) {
                correctCount++;
                int questionPoints = calculatePoints(attemptCount);
                pointsEarned += questionPoints;
            }

            question.setIsUsed(true);
            questionMapper.updateById(question);
        }

        if (pointsEarned > 0) {
            int newTotalPoints = user.getTotalPoints() + pointsEarned;
            userService.updateUserPoints(userId, newTotalPoints);
        }

        SubmitResultVO result = new SubmitResultVO();
        result.setSubject(submitDTO.getSubject());
        result.setTotalQuestions(totalQuestions);
        result.setCorrectCount(correctCount);
        result.setPointsEarned(pointsEarned);
        result.setNewBalance(user.getTotalPoints() + pointsEarned);

        log.info("提交结果: userId={}, correct={}/{}, points=+{}", 
                userId, correctCount, totalQuestions, pointsEarned);

        return result;
    }

    private QuestionVO convertToQuestionVO(Question question) {
        QuestionVO vo = new QuestionVO();
        BeanUtils.copyProperties(question, vo);
        return vo;
    }

    private int calculatePoints(Integer attemptCount) {
        if (attemptCount == null || attemptCount <= 0) {
            return 0;
        }
        if (attemptCount == 1) {
            return 10;
        } else if (attemptCount == 2) {
            return 5;
        } else {
            return 2;
        }
    }

    @Override
    public CompletedQuestionsVO getTodayCompletedQuestions(Long userId, String subject) {
        log.info("获取今日已完成题目: userId={}, subject={}", userId, subject);

        LocalDate today = LocalDate.now();

        LambdaQueryWrapper<QuestionRecord> recordWrapper = new LambdaQueryWrapper<>();
        recordWrapper.eq(QuestionRecord::getUserId, userId)
                    .eq(QuestionRecord::getSubject, subject)
                    .apply("DATE(answered_at) = {0}", today)
                    .orderByAsc(QuestionRecord::getQuestionId);

        List<QuestionRecord> records = questionRecordMapper.selectList(recordWrapper);

        if (records.isEmpty()) {
            throw new BusinessException("今日还没有完成答题");
        }

        List<Long> questionIds = records.stream()
                .map(QuestionRecord::getQuestionId)
                .collect(Collectors.toList());

        List<Question> questions = questionMapper.selectBatchIds(questionIds);
        Map<Long, Question> questionMap = questions.stream()
                .collect(Collectors.toMap(Question::getId, q -> q));

        List<CompletedQuestionsVO.CompletedQuestionDetailVO> details = records.stream()
                .map(record -> {
                    CompletedQuestionsVO.CompletedQuestionDetailVO detail =
                        new CompletedQuestionsVO.CompletedQuestionDetailVO();
                    Question question = questionMap.get(record.getQuestionId());

                    if (question != null) {
                        detail.setQuestionId(question.getId());
                        detail.setQuestionType(question.getQuestionType());
                        detail.setQuestionText(question.getQuestionText());
                        detail.setOptions(question.getOptions());
                        detail.setKnowledgePoint(question.getKnowledgePoint());
                    }

                    detail.setUserAnswer(record.getUserAnswer());
                    detail.setCorrectAnswer(record.getCorrectAnswer());
                    detail.setIsCorrect(record.getIsCorrect());
                    detail.setAnalysis(record.getAnalysis());
                    detail.setAttemptCount(record.getAttemptCount());

                    return detail;
                })
                .collect(Collectors.toList());
}

        CompletedQuestionsVO result = new CompletedQuestionsVO();
        result.setSubject(subject);
        result.setCompletedDate(today);
        result.setQuestions(details);

        return result;
    }

    @Override
    public List<QuestionRecordVO> getQuestionRecords(Long userId, String subject, 
                                                      LocalDate startDate, LocalDate endDate) {
        log.info("获取答题历史: userId={}, subject={}, startDate={}, endDate={}", 
                userId, subject, startDate, endDate);

        LambdaQueryWrapper<QuestionRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(QuestionRecord::getUserId, userId);

        if (subject != null && !subject.isEmpty()) {
            wrapper.eq(QuestionRecord::getSubject, subject);
        }

        if (startDate != null) {
            wrapper.apply("DATE(answered_at) >= {0}", startDate);
        }

        if (endDate != null) {
            wrapper.apply("DATE(answered_at) <= {0}", endDate);
        }

        wrapper.orderByDesc(QuestionRecord::getAnsweredAt);

        List<QuestionRecord> records = questionRecordMapper.selectList(wrapper);

        List<Long> questionIds = records.stream()
                .map(QuestionRecord::getQuestionId)
                .distinct()
                .collect(Collectors.toList());

        Map<Long, Question> questionMap = questionIds.isEmpty() ? 
            Map.of() : 
            questionMapper.selectBatchIds(questionIds).stream()
                .collect(Collectors.toMap(Question::getId, q -> q));

        return records.stream()
                .map(record -> {
                    QuestionRecordVO vo = new QuestionRecordVO();
                    BeanUtils.copyProperties(record, vo);

                    Question question = questionMap.get(record.getQuestionId());
                    if (question != null) {
                        vo.setQuestionType(question.getQuestionType());
                        vo.setQuestionText(question.getQuestionText());
                        vo.setOptions(question.getOptions());
                    }

                    return vo;
                })
                .collect(Collectors.toList());
    }
}
