package com.study.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.study.dto.SubmitAnswerDTO;
import com.study.entity.Question;
import com.study.entity.QuestionRecord;
import com.study.entity.User;
import com.study.common.BusinessException;
import com.study.mapper.QuestionMapper;
import com.study.mapper.QuestionRecordMapper;
import com.study.service.AchievementService;
import com.study.service.QuestionService;
import com.study.service.UserService;
import com.study.vo.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
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
    private final AchievementService achievementService;

    /**
     * 历史列表单页最大条数，防止客户端传入过大的 pageSize
     */
    private static final int MAX_PAGE_SIZE = 50;

    /**
     * 按id批量查题目时的单批大小
     */
    private static final int QUESTION_BATCH_SIZE = 100;

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
        List<SubmitResultVO.QuestionResultDetail> details = new ArrayList<>();

        for (SubmitAnswerDTO.AnswerItem answerItem : submitDTO.getAnswers()) {
            Question question = questionMapper.selectById(answerItem.getQuestionId());
            if (question == null) {
                throw new BusinessException("题目不存在: " + answerItem.getQuestionId());
            }

            log.info("题目 {}: correctAnswer={}, analysis={}",
                    question.getId(), question.getCorrectAnswer(),
                    question.getAnalysis() != null ? question.getAnalysis().substring(0, Math.min(50, question.getAnalysis().length())) : "null");

            // 后端判断答案是否正确
            String userAnswer = normalizeAnswer(answerItem.getUserAnswer());
            String correctAnswer = normalizeAnswer(question.getCorrectAnswer());
            boolean isCorrect = userAnswer.equals(correctAnswer);

            Integer attemptCount = answerItem.getAttemptCount();
            if (attemptCount == null || attemptCount <= 0) {
                attemptCount = 1;
            }

            if (isCorrect) {
                correctCount++;
            }

            // 保存答题记录
            QuestionRecord record = new QuestionRecord();
            record.setUserId(userId);
            record.setQuestionId(question.getId());
            record.setSubject(submitDTO.getSubject());
            record.setUserAnswer(answerItem.getUserAnswer());
            record.setIsCorrect(isCorrect);
            record.setCorrectAnswer(question.getCorrectAnswer());
            record.setAnalysis(question.getAnalysis());
            record.setAttemptCount(attemptCount);
            record.setFirstAttemptCorrect(attemptCount == 1 && isCorrect);

            questionRecordMapper.insert(record);

            // 标记题目已使用
            question.setIsUsed(true);
            questionMapper.updateById(question);

            // 构建详细结果
            SubmitResultVO.QuestionResultDetail detail = new SubmitResultVO.QuestionResultDetail();
            detail.setQuestionId(question.getId());
            detail.setQuestionType(question.getQuestionType());
            detail.setQuestionText(question.getQuestionText());
            detail.setOptions(question.getOptions());
            detail.setUserAnswer(answerItem.getUserAnswer());
            detail.setCorrectAnswer(question.getCorrectAnswer());
            detail.setIsCorrect(isCorrect);
            detail.setAnalysis(question.getAnalysis());
            detail.setAttemptCount(attemptCount);
            detail.setPointsEarned(0); // 单题不计分
            detail.setKnowledgePoint(question.getKnowledgePoint());

            details.add(detail);
        }

        // 完成一个科目，统一加1分
        int pointsEarned = 1;
        int newTotalPoints = user.getTotalPoints() + pointsEarned;
        userService.updateUserPoints(userId, newTotalPoints);

        // 积分结算之后再评估成就，这样「积分大户」当次提交就能解锁
        List<AchievementVO> newAchievements = achievementService.evaluateAndUnlock(userId);

        SubmitResultVO result = new SubmitResultVO();
        result.setSubject(submitDTO.getSubject());
        result.setTotalQuestions(totalQuestions);
        result.setCorrectCount(correctCount);
        result.setPointsEarned(pointsEarned);
        result.setNewBalance(newTotalPoints);
        result.setDetails(details);
        result.setNewAchievements(newAchievements);

        log.info("提交结果: userId={}, correct={}/{}, points=+{}, details_size={}",
                userId, correctCount, totalQuestions, pointsEarned, details.size());

        // 打印第一题的详细信息用于调试
        if (!details.isEmpty()) {
            SubmitResultVO.QuestionResultDetail firstDetail = details.get(0);
            log.info("第一题详情: questionId={}, correctAnswer={}, analysis={}",
                    firstDetail.getQuestionId(),
                    firstDetail.getCorrectAnswer(),
                    firstDetail.getAnalysis() != null ?
                        firstDetail.getAnalysis().substring(0, Math.min(30, firstDetail.getAnalysis().length())) + "..." : "null");
        }

        return result;
    }

    private QuestionVO convertToQuestionVO(Question question) {
        QuestionVO vo = new QuestionVO();
        BeanUtils.copyProperties(question, vo);
        return vo;
    }

    /**
     * 标准化答案，用于比较
     * - 去除首尾空格
     * - 转为小写（英文）
     * - 去除所有空格（数学公式）
     */
    private String normalizeAnswer(String answer) {
        if (answer == null) {
            return "";
        }
        // 去除首尾空格，转小写
        answer = answer.trim().toLowerCase();
        // 去除所有空格（方便数学答案比较，如 "1/2" 和 " 1 / 2 "）
        answer = answer.replaceAll("\\s+", "");
        return answer;
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

        Map<Long, Question> questionMap = loadQuestionMap(questionIds);

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

        wrapper.orderByDesc(QuestionRecord::getAnsweredAt)
               .orderByDesc(QuestionRecord::getId);

        List<QuestionRecord> records = questionRecordMapper.selectList(wrapper);

        List<Long> questionIds = records.stream()
                .map(QuestionRecord::getQuestionId)
                .distinct()
                .collect(Collectors.toList());

        Map<Long, Question> questionMap = loadQuestionMap(questionIds);

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

    @Override
    public PageVO<RecordGroupVO> getRecordGroups(Long userId, String subject, int page, int pageSize) {
        int current = Math.max(page, 1);
        int size = Math.min(Math.max(pageSize, 1), MAX_PAGE_SIZE);

        log.info("分页获取答题历史分组: userId={}, subject={}, page={}, pageSize={}",
                userId, subject, current, size);

        long total = questionRecordMapper.countRecordGroups(userId, subject);
        if (total == 0) {
            return PageVO.empty(current, size);
        }

        int offset = (current - 1) * size;
        List<RecordGroupVO> groups = questionRecordMapper.selectRecordGroups(userId, subject, offset, size);

        return PageVO.of(groups, total, current, size);
    }

    @Override
    public DailyProgressVO getDailyProgress(Long userId) {
        log.info("获取今日完成状态: userId={}", userId);

        List<String> subjects = questionRecordMapper.selectTodaySubjects(userId);

        DailyProgressVO progress = new DailyProgressVO();
        for (String subject : subjects) {
            if ("math".equals(subject)) {
                progress.setMath(true);
            } else if ("english".equals(subject)) {
                progress.setEnglish(true);
            } else if ("chinese".equals(subject)) {
                progress.setChinese(true);
            }
        }

        return progress;
    }

    @Override
    public RecordSummaryVO getRecordSummary(Long userId, String subject) {
        log.info("获取答题历史统计: userId={}, subject={}", userId, subject);

        RecordSummaryVO summary = questionRecordMapper.selectRecordSummary(userId, subject);

        if (summary == null) {
            summary = new RecordSummaryVO();
            summary.setTotalDays(0);
            summary.setTotalQuestions(0);
            summary.setTotalCorrect(0);
            summary.setAvgAccuracy(0);
        }

        return summary;
    }

    @Override
    public CompletedQuestionsVO getDaySubjectRecords(Long userId, LocalDate date, String subject) {
        log.info("获取答题详情: userId={}, date={}, subject={}", userId, date, subject);

        LambdaQueryWrapper<QuestionRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(QuestionRecord::getUserId, userId)
               .eq(QuestionRecord::getSubject, subject)
               .apply("DATE(answered_at) = {0}", date)
               .orderByAsc(QuestionRecord::getQuestionId);

        List<QuestionRecord> records = questionRecordMapper.selectList(wrapper);

        if (records.isEmpty()) {
            throw new BusinessException("该日期没有答题记录");
        }

        // 获取题目详情
        List<Long> questionIds = records.stream()
                .map(QuestionRecord::getQuestionId)
                .collect(Collectors.toList());

        Map<Long, Question> questionMap = loadQuestionMap(questionIds);

        // 构建详情列表
        List<CompletedQuestionsVO.CompletedQuestionDetailVO> details = records.stream()
                .map(record -> {
                    Question question = questionMap.get(record.getQuestionId());
                    if (question == null) {
                        return null;
                    }

                    CompletedQuestionsVO.CompletedQuestionDetailVO detail = new CompletedQuestionsVO.CompletedQuestionDetailVO();
                    detail.setQuestionId(question.getId());
                    detail.setQuestionType(question.getQuestionType());
                    detail.setQuestionText(question.getQuestionText());
                    detail.setOptions(question.getOptions());
                    detail.setUserAnswer(record.getUserAnswer());
                    detail.setCorrectAnswer(record.getCorrectAnswer());
                    detail.setIsCorrect(record.getIsCorrect());
                    detail.setAnalysis(record.getAnalysis());
                    detail.setAttemptCount(record.getAttemptCount());
                    detail.setKnowledgePoint(question.getKnowledgePoint());

                    return detail;
                })
                .filter(detail -> detail != null)
                .collect(Collectors.toList());

        CompletedQuestionsVO result = new CompletedQuestionsVO();
        result.setSubject(subject);
        result.setCompletedDate(date);
        result.setQuestions(details);

        return result;
    }

    /**
     * 按id批量加载题目
     * 分批查询，避免 id 过多时 IN (...) 把单条SQL撑到超过 max_allowed_packet
     */
    private Map<Long, Question> loadQuestionMap(List<Long> questionIds) {
        if (questionIds == null || questionIds.isEmpty()) {
            return Map.of();
        }

        List<Long> distinctIds = questionIds.stream()
                .distinct()
                .collect(Collectors.toList());

        Map<Long, Question> questionMap = new HashMap<>();
        for (int i = 0; i < distinctIds.size(); i += QUESTION_BATCH_SIZE) {
            List<Long> batch = distinctIds.subList(i, Math.min(i + QUESTION_BATCH_SIZE, distinctIds.size()));
            questionMapper.selectBatchIds(batch)
                    .forEach(question -> questionMap.put(question.getId(), question));
        }

        return questionMap;
    }
}
