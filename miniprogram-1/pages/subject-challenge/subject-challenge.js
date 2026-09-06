const http = require('../../utils/http.js');
const app = getApp();

Page({
  data: {
    subject: '',
    subjectName: '',
    subjectIcon: '',
    subjectClass: '',
    questions: [],
    currentQuestion: 0,
    currentQuestionData: {},
    totalQuestions: 0,
    answers: {},
    optionLabels: ['A', 'B', 'C', 'D'],
    currentAnswerLength: 0,
    progress: 0,

    // 答题状态
    attemptCount: {},  // 每题的尝试次数 {questionIndex: count}
    showAnalysis: false,  // 是否显示解析
    showCorrectAnswer: false,  // 是否显示正确答案
    currentFeedback: '',  // 当前反馈信息
    isCorrect: false,  // 当前题是否答对

    // 完成状态
    showResult: false,
    correctCount: 0,
    totalPoints: 1  // 完成一个科目得1分
  },

  onLoad: function(options) {
    const subject = options.subject || 'math';
    const subjectNames = {
      'math': '数学',
      'english': '英语',
      'chinese': '语文'
    };
    const subjectIcons = {
      'math': '📐',
      'english': '📚',
      'chinese': '📖'
    };

    this.setData({
      subject: subject,
      subjectName: subjectNames[subject] || '未知科目',
      subjectIcon: subjectIcons[subject] || '📝',
      subjectClass: subject
    });

    this.loadQuestions();
  },

  loadQuestions: function() {
    const that = this;

    wx.showLoading({
      title: '加载题目中...'
    });

    http.get('/questions/daily', { subject: this.data.subject })
      .then(data => {
        wx.hideLoading();

        if (!data.questions || data.questions.length === 0) {
          wx.showModal({
            title: '提示',
            content: '今日题目尚未生成，请稍后再试',
            showCancel: false,
            success: function() {
              wx.navigateBack();
            }
          });
          return;
        }

        // 格式化题目数据
        const questions = data.questions.map(q => {
          let options = [];
          if (q.options) {
            if (typeof q.options === 'string') {
              try {
                options = JSON.parse(q.options);
              } catch (e) {
                console.error('解析options失败:', e, q.options);
                options = [];
              }
            } else if (Array.isArray(q.options)) {
              options = q.options;
            }
          }

          return {
            id: q.id,
            type: q.questionType,
            question: q.questionText,
            options: options,
            correctAnswer: q.correctAnswer,
            analysis: q.analysis || '暂无解析'
          };
        });

        that.setData({
          questions: questions,
          totalQuestions: questions.length,
          currentQuestionData: questions[0]
        });

        that.updateProgress();
      })
      .catch(err => {
        wx.hideLoading();
        console.error('加载题目失败:', err);
        wx.showModal({
          title: '加载失败',
          content: '无法获取题目,请检查网络连接',
          showCancel: false,
          success: function() {
            wx.navigateBack();
          }
        });
      });
  },

  selectOption: function(e) {
    const index = e.currentTarget.dataset.index;
    const answers = this.data.answers;
    answers[this.data.currentQuestion] = index;

    this.setData({
      answers: answers
    });
  },

  onAnswerInput: function(e) {
    const answers = this.data.answers;
    answers[this.data.currentQuestion] = e.detail.value;

    this.setData({
      answers: answers,
      currentAnswerLength: e.detail.value.length
    });
  },

  // 提交当前题目的答案
  submitCurrentAnswer: function() {
    const currentIndex = this.data.currentQuestion;
    const userAnswer = this.data.answers[currentIndex];

    if (userAnswer === undefined || userAnswer === '') {
      wx.showToast({
        title: '请先作答',
        icon: 'none'
      });
      return;
    }

    const question = this.data.currentQuestionData;
    const attemptCount = this.data.attemptCount;
    const currentAttempt = attemptCount[currentIndex] || 0;

    // 获取用户答案（选择题转换为字母）
    let userAnswerValue;
    if (question.type === 'choice') {
      userAnswerValue = this.data.optionLabels[userAnswer];
    } else {
      userAnswerValue = userAnswer;
    }

    // 判断是否正确
    const isCorrect = userAnswerValue === question.correctAnswer;

    if (isCorrect) {
      // 答对了
      this.setData({
        isCorrect: true,
        currentFeedback: '🎉 回答正确！',
        showAnalysis: true,
        showCorrectAnswer: false
      });

      // 更新正确题数
      this.setData({
        correctCount: this.data.correctCount + 1
      });

    } else {
      // 答错了
      attemptCount[currentIndex] = currentAttempt + 1;

      if (currentAttempt === 0) {
        // 第一次答错：显示解析
        this.setData({
          attemptCount: attemptCount,
          isCorrect: false,
          currentFeedback: '❌ 回答错误，请查看解析后再试一次',
          showAnalysis: true,
          showCorrectAnswer: false
        });
      } else {
        // 第二次答错：显示正确答案
        this.setData({
          attemptCount: attemptCount,
          isCorrect: false,
          currentFeedback: '❌ 再次答错，正确答案是：' + question.correctAnswer,
          showAnalysis: true,
          showCorrectAnswer: true
        });
      }
    }
  },

  // 下一题
  nextQuestion: function() {
    const currentIndex = this.data.currentQuestion;
    const attemptCount = this.data.attemptCount[currentIndex] || 0;
    const isCorrect = this.data.isCorrect;

    // 必须答对或者尝试了2次才能下一题
    if (!isCorrect && attemptCount < 2) {
      wx.showToast({
        title: '请再次尝试作答',
        icon: 'none'
      });
      return;
    }

    // 重置当前题状态
    this.setData({
      showAnalysis: false,
      showCorrectAnswer: false,
      currentFeedback: '',
      isCorrect: false
    });

    if (currentIndex < this.data.questions.length - 1) {
      const nextIndex = currentIndex + 1;
      this.setData({
        currentQuestion: nextIndex,
        currentQuestionData: this.data.questions[nextIndex]
      });
      this.updateProgress();
    } else {
      // 最后一题，提交所有答案
      this.finishChallenge();
    }
  },

  prevQuestion: function() {
    if (this.data.currentQuestion > 0) {
      const prevIndex = this.data.currentQuestion - 1;

      // 重置状态
      this.setData({
        currentQuestion: prevIndex,
        currentQuestionData: this.data.questions[prevIndex],
        showAnalysis: false,
        showCorrectAnswer: false,
        currentFeedback: '',
        isCorrect: false
      });
      this.updateProgress();
    }
  },

  updateProgress: function() {
    const progress = ((this.data.currentQuestion + 1) / this.data.totalQuestions) * 100;
    this.setData({
      progress: progress
    });
  },

  // 完成挑战
  finishChallenge: function() {
    const that = this;
    const answers = [];
    const optionMap = ['A', 'B', 'C', 'D'];

    // 构建答案数组
    this.data.questions.forEach((question, index) => {
      const userAnswer = this.data.answers[index];
      if (userAnswer !== undefined) {
        const answerValue = question.type === 'choice' ? optionMap[userAnswer] : userAnswer;
        const attemptCount = this.data.attemptCount[index] || 0;
        const isCorrect = answerValue === question.correctAnswer;

        answers.push({
          questionId: question.id,
          userAnswer: answerValue,
          isCorrect: isCorrect,
          attemptCount: attemptCount + 1
        });
      }
    });

    wx.showLoading({
      title: '提交中...'
    });

    // 提交答案到后端
    http.post('/questions/submit', {
      subject: this.data.subject,
      answers: answers
    })
      .then(data => {
        wx.hideLoading();

        // ========== 调试日志开始 ==========
        console.log('=== 提交答案完整响应 ===');
        console.log('完整data:', JSON.stringify(data, null, 2));
        console.log('data.details:', data.details);

        if (data.details && data.details.length > 0) {
          console.log('第一题详情:');
          console.log('  - questionId:', data.details[0].questionId);
          console.log('  - questionText:', data.details[0].questionText);
          console.log('  - userAnswer:', data.details[0].userAnswer);
          console.log('  - correctAnswer:', data.details[0].correctAnswer);
          console.log('  - isCorrect:', data.details[0].isCorrect);
          console.log('  - analysis:', data.details[0].analysis);
          console.log('  - attemptCount:', data.details[0].attemptCount);
          console.log('  - pointsEarned:', data.details[0].pointsEarned);

          console.log('所有题目的正确答案和解析:');
          data.details.forEach((detail, index) => {
            console.log(`题目${index + 1}:`, {
              correctAnswer: detail.correctAnswer,
              analysis: detail.analysis
            });
          });
        } else {
          console.log('警告: data.details 为空或不存在');
        }
        // ========== 调试日志结束 ==========

        // 更新用户积分（每个科目1分）
        const currentUser = wx.getStorageSync('currentUser');
        if (currentUser) {
          currentUser.totalPoints = (currentUser.totalPoints || 0) + 1;
          wx.setStorageSync('currentUser', currentUser);
          app.globalData.totalPoints = currentUser.totalPoints;
        }

        // 保存今日完成状态
        const todayKey = that.getTodayKey();
        const progress = wx.getStorageSync(todayKey + '_progress') || {};
        progress[that.data.subject] = true;
        wx.setStorageSync(todayKey + '_progress', progress);

        // 显示结果
        that.setData({
          showResult: true
        });

        wx.showToast({
          title: '完成挑战！+1积分',
          icon: 'success',
          duration: 2000
        });
      })
      .catch(err => {
        wx.hideLoading();
        console.error('提交失败:', err);
        wx.showToast({
          title: '提交失败，请重试',
          icon: 'none'
        });
      });
  },

  getTodayKey: function() {
    const date = new Date();
    return 'challenge_' + date.getFullYear() + '_' + (date.getMonth() + 1) + '_' + date.getDate();
  },

  backToHome: function() {
    wx.navigateBack();
  },

  retryQuestion: function() {
    // 清除当前答案，允许重新作答
    const answers = this.data.answers;
    delete answers[this.data.currentQuestion];

    this.setData({
      answers: answers,
      showAnalysis: false,
      currentFeedback: '',
      isCorrect: false
    });
  }
});
