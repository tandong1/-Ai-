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
              throw new BusinessException("今日题目尚未生成,请稍后再试");
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
      public SubmitResultVO submitAnswers(Long userId, SubmitAnswerDTO
  submitDTO) {
          User user = userService.getUserById(userId);
          int correctCount = 0;

          for (SubmitAnswerDTO.AnswerItem answerItem : submitDTO.getAnswers()) {
              // 查询题目
              Question question =        
  questionMapper.selectById(answerItem.getQuestionId());
              if (question == null) {
                  throw new BusinessException("题目不存在: " +
  answerItem.getQuestionId());
              }

              // 使用前端传来的判断结果
              boolean isCorrect = answerItem.getIsCorrect() != null &&
  answerItem.getIsCorrect();
              int attemptCount = answerItem.getAttemptCount() != null ?
  answerItem.getAttemptCount() : 1;

              if (isCorrect) {           
                  correctCount++;
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
              record.setAttemptCount(attemptCount);  // 记录尝试次数
              record.setFirstAttemptCorrect(isCorrect && attemptCount == 1);
              questionRecordMapper.insert(record);

              // 如果答错，添加到错题本
              if (!isCorrect) {
                  addToWrongQuestions(userId, question.getId(),
  question.getSubject());
              }

              // 标记题目已使用
              if (!question.getIsUsed()) {
                  question.setIsUsed(true);
                  questionMapper.updateById(question);
              }
                                         
              log.info("答题记录: userId={}, questionId={}, isCorrect={}, 
  attemptCount={}",
                       userId, question.getId(), isCorrect, attemptCount);
          }

          // 新积分规则：完成一个科目 = +1积分
          int pointsEarned = 1;
          int newBalance = user.getTotalPoints() + pointsEarned;
          userService.updateUserPoints(userId, newBalance);

          // 保存积分记录
          savePointsRecord(userId, pointsEarned, newBalance,
                         "daily_challenge", submitDTO.getSubject() +
  "挑战完成");

          // 构建返回结果
          SubmitResultVO resultVO = new SubmitResultVO();
          resultVO.setSubject(submitDTO.getSubject());
          resultVO.setTotalQuestions(submitDTO.getAnswers().size());
          resultVO.setCorrectCount(correctCount);
          resultVO.setPointsEarned(pointsEarned);
          resultVO.setNewBalance(newBalance);

          log.info("提交答案完成: userId={}, subject={}, correct={}/{}, 
  points={}, newBalance={}",
                   userId, submitDTO.getSubject(), correctCount,
                   submitDTO.getAnswers().size(), pointsEarned, newBalance);

          return resultVO;
      }
  
      /**                                
       * 添加到错题本
       */
      private void addToWrongQuestions(Long userId, Long questionId, String
  subject) {
          // 查询是否已存在
          LambdaQueryWrapper<WrongQuestion> wrapper = new
  LambdaQueryWrapper<>();
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
              log.info("新增错题记录: userId={}, questionId={}", userId,
  questionId);
          }
      }
  
      /**                                
       * 保存积分记录
       */
      private void savePointsRecord(Long userId, Integer changeAmount,
                                    Integer balanceAfter, String sourceType,
  String description) {
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