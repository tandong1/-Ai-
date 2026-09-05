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
    const history = wx.getStorageSync('history') || [];

    // 处理历史数据
    const processedHistory = history.map(item => {
      const accuracy = item.accuracy;
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
        ...item,
        level,
        levelText
      };
    });

    // 计算统计数据
    const totalDays = history.length;
    const totalQuestions = history.reduce((sum, item) => sum + item.totalQuestions, 0);
    const avgAccuracy = totalDays > 0
      ? Math.round(history.reduce((sum, item) => sum + item.accuracy, 0) / totalDays)
      : 0;

    this.setData({
      historyList: processedHistory,
      totalDays,
      totalQuestions,
      avgAccuracy
    });
  }
});
