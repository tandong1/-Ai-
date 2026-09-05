var app = getApp();

Page({
  data: {
    subject: '',
    subjectName: '',
    subjectIcon: '',
    subjectClass: '',
    currentQuestion: 0,
    totalQuestions: 0,
    questions: [],
    currentQuestionData: {},
    answers: [],
    optionLabels: ['A', 'B', 'C', 'D'],
    progress: 0,
    currentAnswerLength: 0
  },

  onLoad: function(options) {
    var subject = options.subject || 'math';
    this.setData({ subject: subject });
    this.initSubject(subject);
    this.loadQuestions(subject);
  },

  initSubject: function(subject) {
    var config = {
      math: { name: '数学', icon: '📐', class: 'math-theme' },
      english: { name: '英语', icon: '📚', class: 'english-theme' },
      chinese: { name: '语文', icon: '✍️', class: 'chinese-theme' }
    };

    var subjectConfig = config[subject];
    this.setData({
      subjectName: subjectConfig.name,
      subjectIcon: subjectConfig.icon,
      subjectClass: subjectConfig.class
    });

    wx.setNavigationBarTitle({ title: subjectConfig.name + '练习' });
  },

  loadQuestions: function(subject) {
    var questionBanks = {
      math: [
        { type: 'choice', question: '计算: 15 + 28 = ?', options: ['41', '42', '43', '44'], answer: 2 },
        { type: 'fill', question: '一个长方形的长是8cm，宽是5cm，它的周长是____cm。', answer: '26' },
        { type: 'choice', question: '下列哪个数是偶数？', options: ['13', '15', '18', '21'], answer: 2 },
        { type: 'fill', question: '小明有24个苹果，平均分给6个小朋友，每人分到____个。', answer: '4' },
        { type: 'choice', question: '3 × 7 = ?', options: ['18', '20', '21', '24'], answer: 2 }
      ],
      english: [
        { type: 'choice', question: 'What is the capital of France?', options: ['London', 'Berlin', 'Paris', 'Madrid'], answer: 2 },
        { type: 'fill', question: 'The opposite of "hot" is ____.', answer: 'cold' },
        { type: 'choice', question: 'Which word means "beautiful"?', options: ['ugly', 'pretty', 'bad', 'dirty'], answer: 1 },
        { type: 'fill', question: 'I ____ (am/is/are) a student.', answer: 'am' },
        { type: 'choice', question: 'How many days are there in a week?', options: ['5', '6', '7', '8'], answer: 2 }
      ],
      chinese: [
        { type: 'choice', question: '"春眠不觉晓"的下一句是？', options: ['处处闻啼鸟', '夜来风雨声', '花落知多少', '春江水暖'], answer: 0 },
        { type: 'fill', question: '"白日依山尽，黄河____海流"', answer: '入' },
        { type: 'choice', question: '下列哪个字是多音字？', options: ['山', '水', '行', '云'], answer: 2 },
        { type: 'fill', question: '《静夜思》的作者是____', answer: '李白' },
        { type: 'choice', question: '"桃李满天下"中的"桃李"指的是？', options: ['水果', '学生', '朋友', '花朵'], answer: 1 }
      ]
    };

    var questions = questionBanks[subject] || questionBanks.math;
    var answers = [];
    for (var i = 0; i < questions.length; i++) {
      answers.push(null);
    }

    this.setData({
      questions: questions,
      totalQuestions: questions.length,
      currentQuestionData: questions[0],
      answers: answers,
      progress: (1 / questions.length) * 100
    });

    this.updateAnswerLength();
  },

  updateAnswerLength: function() {
    var currentAnswer = this.data.answers[this.data.currentQuestion];
    var length = 0;
    if (currentAnswer && typeof currentAnswer === 'string') {
      length = currentAnswer.length;
    }
    this.setData({ currentAnswerLength: length });
  },

  selectOption: function(e) {
    console.log('selectOption被调用', e);
    var index = parseInt(e.currentTarget.dataset.index);
    console.log('选择索引:', index, '当前题号:', this.data.currentQuestion);

    var answers = this.data.answers;
    answers[this.data.currentQuestion] = index;

    console.log('更新后的answers:', answers);

    this.setData({
      answers: answers
    });

    console.log('setData完成');
  },

  onAnswerInput: function(e) {
    var value = e.detail.value;
    var answers = this.data.answers;
    answers[this.data.currentQuestion] = value;
    this.setData({
      answers: answers,
      currentAnswerLength: value.length
    });
  },

  prevQuestion: function() {
    if (this.data.currentQuestion > 0) {
      var prev = this.data.currentQuestion - 1;
      this.setData({
        currentQuestion: prev,
        currentQuestionData: this.data.questions[prev],
        progress: ((prev + 1) / this.data.totalQuestions) * 100
      });
      this.updateAnswerLength();
    }
  },

  nextQuestion: function() {
    if (this.data.currentQuestion < this.data.totalQuestions - 1) {
      var next = this.data.currentQuestion + 1;
      this.setData({
        currentQuestion: next,
        currentQuestionData: this.data.questions[next],
        progress: ((next + 1) / this.data.totalQuestions) * 100
      });
      this.updateAnswerLength();
    }
  },

  submitAllAnswers: function() {
    console.log('submitAllAnswers被调用');
    console.log('当前answers:', this.data.answers);

    var that = this;
    var unanswered = [];

    for (var i = 0; i < this.data.answers.length; i++) {
      var ans = this.data.answers[i];
      console.log('第' + (i+1) + '题答案:', ans, '类型:', typeof ans);
      if (ans === null || ans === undefined || ans === '') {
        unanswered.push(i + 1);
      }
    }

    console.log('未答题目:', unanswered);

    if (unanswered.length > 0) {
      wx.showModal({
        title: '提示',
        content: '还有' + unanswered.length + '题未作答，确定要提交吗？',
        confirmText: '继续答题',
        cancelText: '确定提交',
        success: function(res) {
          if (res.cancel) {
            that.doSubmit();
          }
        }
      });
    } else {
      this.doSubmit();
    }
  },

  doSubmit: function() {
    console.log('开始提交');
    wx.showLoading({ title: '提交中...', mask: true });

    var that = this;

    var submitData = {
      subject: this.data.subject,
      subjectName: this.data.subjectName,
      questions: this.data.questions,
      answers: this.data.answers,
      submitTime: Date.now()
    };

    console.log('提交数据:', JSON.stringify(submitData));

    setTimeout(function() {
      wx.hideLoading();
      wx.showToast({ title: '提交成功！', icon: 'success' });

      var todayKey = that.getTodayKey();
      var progress = wx.getStorageSync(todayKey + '_progress') || {};
      progress[that.data.subject] = true;
      wx.setStorageSync(todayKey + '_progress', progress);

      console.log('保存进度:', progress);

      setTimeout(function() {
        wx.navigateBack();
      }, 1500);
    }, 1000);
  },

  getTodayKey: function() {
    var date = new Date();
    return 'challenge_' + date.getFullYear() + '_' + (date.getMonth() + 1) + '_' + date.getDate();
  }
});

