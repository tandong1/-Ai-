const http = require('../../utils/http.js');

Page({
  data: {
    historyList: [],
    totalDays: 0,
    totalQuestions: 0,
    avgAccuracy: 0
  },

  onLoad() {
    this.loadHistory();
  },

  onShow() {
    this.loadHistory();
  },

  loadHistory() {
    const that = this;

    wx.showLoading({
      title: '加载中...'
    });

    // 从后端获取答题记录
    http.get('/questions/records', {})
      .then(records => {
        wx.hideLoading();

        if (!records || records.length === 0) {
          that.setData({
            historyList: [],
            totalDays: 0,
            totalQuestions: 0,
            avgAccuracy: 0
          });
          return;
        }

        // 按日期分组
        const recordsByDate = {};
        records.forEach(record => {
          const date = record.answeredAt.split('T')[0]; // 获取日期部分
          if (!recordsByDate[date]) {
            recordsByDate[date] = {
              date: date,
              subject: record.subject,
              records: []
            };
          }
          recordsByDate[date].records.push(record);
        });

        // 转换为数组并计算统计
        const historyList = Object.values(recordsByDate).map(item => {
          const totalQuestions = item.records.length;
          const correctCount = item.records.filter(r => r.isCorrect).length;
          const accuracy = totalQuestions > 0
            ? Math.round((correctCount / totalQuestions) * 100)
            : 0;

          let level = 'normal';
          let levelText = '继续努力';

          if (accuracy >= 80) {
            level = 'excellent';
            levelText = '优秀';
          } else if (accuracy >= 60) {
            level = 'good';
            levelText = '良好';
          }

          return {
            date: item.date,
            subject: item.subject,
            totalQuestions: totalQuestions,
            correctCount: correctCount,
            accuracy: accuracy,
            level: level,
            levelText: levelText
          };
        });

        // 按日期倒序排序
        historyList.sort((a, b) => b.date.localeCompare(a.date));

        // 计算总体统计
        const totalDays = historyList.length;
        const totalQuestions = historyList.reduce((sum, item) => sum + item.totalQuestions, 0);
        const avgAccuracy = totalDays > 0
          ? Math.round(historyList.reduce((sum, item) => sum + item.accuracy, 0) / totalDays)
          : 0;

        that.setData({
          historyList: historyList,
          totalDays: totalDays,
          totalQuestions: totalQuestions,
          avgAccuracy: avgAccuracy
        });
      })
      .catch(err => {
        wx.hideLoading();
        console.error('加载历史记录失败:', err);

        // 加载失败时显示空状态
        that.setData({
          historyList: [],
          totalDays: 0,
          totalQuestions: 0,
          avgAccuracy: 0
        });
      });
  }
});
