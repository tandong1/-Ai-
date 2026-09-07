const http = require('../../utils/http.js');

Page({
  data: {
    date: '',
    subject: '',
    subjectName: '',
    questions: [],
    totalQuestions: 0,
    correctCount: 0,
    accuracy: 0
  },

  onLoad: function(options) {
    const date = options.date || '';
    const subject = options.subject || '';

    const subjectNames = {
      'math': '数学',
      'english': '英语',
      'chinese': '语文'
    };

    this.setData({
      date: date,
      subject: subject,
      subjectName: subjectNames[subject] || '未知科目'
    });

    this.loadDetail();
  },

  loadDetail: function() {
    const that = this;

    wx.showLoading({
      title: '加载中...'
    });

    http.get('/questions/records/detail', {
      date: this.data.date,
      subject: this.data.subject
    })
      .then(data => {
        wx.hideLoading();

        const questions = data.questions || [];
        const correctCount = questions.filter(q => q.isCorrect).length;
        const accuracy = questions.length > 0
          ? Math.round((correctCount / questions.length) * 100)
          : 0;

        that.setData({
          questions: questions,
          totalQuestions: questions.length,
          correctCount: correctCount,
          accuracy: accuracy
        });
      })
      .catch(err => {
        wx.hideLoading();
        console.error('加载详情失败:', err);

        wx.showModal({
          title: '加载失败',
          content: '无法获取答题详情',
          showCancel: false,
          success: function() {
            wx.navigateBack();
          }
        });
      });
  }
});
