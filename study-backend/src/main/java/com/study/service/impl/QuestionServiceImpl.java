package com.study.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.study.common.BusinessException;
import com.study.dto.SubmitAnswerDTO;
import com.study.entity.*;
import com.study.mapper.*;
import com.study.service.QuestionService;
import com.study.service.UserService;
import com.study.vo.AnswerResultVO;
import com.study.vo.DailyQuestionsVO;
import com.study.vo.QuestionVO;
import com.study.vo.SubmitResultVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
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
    private final WrongQuestionMapper wrongQuestionMapper;
    private final PointsRecordMapper pointsRecordMapper;
    private final UserService userService;

    @Override
    public DailyQuestionsVO getDailyQuestions(Long userId, String subject) {
        // 查询今日题目
        LambdaQueryWrapper<Question> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Question::getUserId, userId)
               .eq(Question::getSubject, subject)
               .eq(Question::getGeneratedDate, LocalDate.now())
               .orderByAsc(Question::getId);

        List<Question> questions = questionMapper.selectList(wrapper);

        if (questions.isEmpty()) {
            throw new BusinessException("今日题目尚未生成，请稍后再试");
        }

        // 转换为VO（不包含答案和解析）
        List<QuestionVO> questionVOs = questions.stream()
                .map(question -> {
                    QuestionVO vo = new QuestionVO();
                    vo.setId(question.getId());
                    vo.setQuestionType(question.getQuestionType());
                    vo.setQuestionText(question.getQuestionText());
                    vo.setOptions(question.getOptions());
                    vo.setDifficulty(question.getDifficulty());
                    vo.setKnowledgePoint(question.getKnowledgePoint());
                    // 注意：不设置correctAnswer和analysis
                    return vo;
                })
                .collect(Collectors.toList());

        DailyQuestionsVO result = new DailyQuestionsVO();
        result.setSubject(subject);
        result.setGeneratedDate(LocalDate.now());
        result.setQuestions(questionVOs);

        log.info("获取今日题目: userId={}, subject={}, count={}",
                 userId, subject, questionVOs.size());

        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SubmitResultVO submitAnswers(Long userId, SubmitAnswerDTO submitDTO) {
        User user = userService.getUserById(userId);
        List<AnswerResultVO> results = new ArrayList<>();
        int totalPointsEarned = 0;
        int correctCount = 0;

        for (SubmitAnswerDTO.AnswerItem answerItem : submitDTO.getAnswers()) {
            // 查询题目
            Question question = questionMapper.selectById(answerItem.getQuestionId());
            if (question == null) {
                throw new BusinessException("题目不存在: " + answerItem.getQuestionId());
            }

            // 判断答案是否正确
            String userAnswer = normalizeAnswer(answerItem.getUserAnswer());
            String correctAnswer = normalizeAnswer(question.getCorrectAnswer());
            boolean isCorrect = userAnswer.equals(correctAnswer);

            // 计算得分
            int pointsEarned = isCorrect ? 10 : 0;
            if (isCorrect) {
                correctCount++;
                totalPointsEarned += pointsEarned;
            }

            // 保存答题记录
            QuestionRecord record = new QuestionRecord();
            record.setUserId(userId);
            record.setQuestionId(question.getId());
            record.setSubject(question.getSubject());
            record.setUserAnswer(answerItem.getUserAnswer());
            record.setIsCorrect(isCorrect);
            record.setCorrectAnswer(question.getCorrectAnswer());
            record.setAnalysis(question.getAnalysis());
            record.setAttemptCount(1);
            record.setFirstAttemptCorrect(isCorrect);
            questionRecordMapper.insert(record);

            // 如果答错，添加到错题本
            if (!isCorrect) {
                addToWrongQuestions(userId, question.getId(), question.getSubject());
            }

            // 标记题目已使用
            if (!question.getIsUsed()) {
                question.setIsUsed(true);
                questionMapper.updateById(question);
            }

            // 构建结果
            AnswerResultVO resultVO = AnswerResultVO.builder()
                    .questionId(question.getId())
                    .isCorrect(isCorrect)
                    .userAnswer(answerItem.getUserAnswer())
                    .correctAnswer(question.getCorrectAnswer())
                    .analysis(question.getAnalysis())
                    .pointsEarned(pointsEarned)
                    .build();
            results.add(resultVO);

            log.info("答题记录: userId={}, questionId={}, isCorrect={}, points={}",
                     userId, question.getId(), isCorrect, pointsEarned);
        }

        // 更新用户积分
        int newBalance = user.getTotalPoints() + totalPointsEarned;
        userService.updateUserPoints(userId, newBalance);

        // 保存积分记录
        if (totalPointsEarned > 0) {
            savePointsRecord(userId, totalPointsEarned, newBalance,
                           "daily_challenge", submitDTO.getSubject() + "挑战完成");
        }

        // 计算正确率
        double accuracy = submitDTO.getAnswers().isEmpty() ? 0 :
                         (correctCount * 100.0 / submitDTO.getAnswers().size());

        // 构建返回结果
        SubmitResultVO resultVO = new SubmitResultVO();
        resultVO.setResults(results);
        resultVO.setTotalPointsEarned(totalPointsEarned);
        resultVO.setCorrectCount(correctCount);
        resultVO.setTotalCount(submitDTO.getAnswers().size());
        resultVO.setAccuracy(accuracy);
        resultVO.setNewBalance(newBalance);

        log.info("提交答案完成: userId={}, subject={}, correct={}/{}, points={}, newBalance={}",
                 userId, submitDTO.getSubject(), correctCount,
                 submitDTO.getAnswers().size(), totalPointsEarned, newBalance);

        return resultVO;
    }

    /**
     * 添加到错题本
     */
    private void addToWrongQuestions(Long userId, Long questionId, String subject) {
        // 查询是否已存在
        LambdaQueryWrapper<WrongQuestion> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WrongQuestion::getUserId, userId)
               .eq(WrongQuestion::getQuestionId, questionId);

        WrongQuestion existing = wrongQuestionMapper.selectOne(wrapper);

        if (existing != null) {
            // 已存在，增加错误次数
            existing.setWrongCount(existing.getWrongCount() + 1);
            wrongQuestionMapper.updateById(existing);
            log.info("更新错题记录: userId={}, questionId={}, wrongCount={}",
                     userId, questionId, existing.getWrongCount());
        } else {
            // 不存在，新增错题记录
            WrongQuestion wrongQuestion = new WrongQuestion();
            wrongQuestion.setUserId(userId);
            wrongQuestion.setQuestionId(questionId);
            wrongQuestion.setSubject(subject);
            wrongQuestion.setWrongCount(1);
            wrongQuestion.setIsMastered(false);
            wrongQuestionMapper.insert(wrongQuestion);
            log.info("新增错题记录: userId={}, questionId={}", userId, questionId);
        }
    }

    /**
     * 保存积分记录
     */
    private void savePointsRecord(Long userId, Integer changeAmount,
                                  Integer balanceAfter, String sourceType, String description) {
        PointsRecord record = new PointsRecord();
        record.setUserId(userId);
        record.setChangeType("earn");
        record.setChangeAmount(changeAmount);
        record.setSourceType(sourceType);
        record.setDescription(description);
        record.setBalanceAfter(balanceAfter);
        pointsRecordMapper.insert(record);

        log.info("保存积分记录: userId={}, changeAmount={}, balanceAfter={}",
                 userId, changeAmount, balanceAfter);
    }

    /**
     * 标准化答案（去除空格、转小写）
     */
    private String normalizeAnswer(String answer) {
        if (answer == null) {
            return "";
        }
        return answer.trim().toLowerCase().replaceAll("\\s+", "");
    }
}
