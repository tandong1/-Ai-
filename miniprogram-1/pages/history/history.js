const http = require('../../utils/http.js');

const PAGE_SIZE = 20;

Page({
  data: {
    historyList: [],
    totalDays: 0,
    totalQuestions: 0,
    avgAccuracy: 0,
    page: 0,          // 已加载到第几页，0表示还未加载
    hasMore: true,
    loading: false,
    loaded: false     // 首屏是否已有结果，用于控制空状态
  },

  // 首次进入时 onShow 也会触发，不需要再写 onLoad
  onShow() {
    this.refresh();
  },

  onReachBottom() {
    if (this.data.loading || !this.data.hasMore) {
      return;
    }
    this.loadPage(this.data.page + 1);
  },

  onPullDownRefresh() {
    this.refresh(true);
  },

  // 回到第一页并刷新统计，不清空旧列表，避免闪一下空状态
  refresh(fromPullDown) {
    this.loadSummary();
    this.loadPage(1, fromPullDown);
  },

  loadSummary() {
    const that = this;

    http.get('/questions/records/summary', {})
      .then(summary => {
        that.setData({
          totalDays: summary.totalDays || 0,
          totalQuestions: summary.totalQuestions || 0,
          avgAccuracy: summary.avgAccuracy || 0
        });
      })
      .catch(err => {
        console.error('加载历史统计失败:', err);
      });
  },

  loadPage(page, fromPullDown) {
    const that = this;

    if (this.data.loading) {
      if (fromPullDown) {
        wx.stopPullDownRefresh();
      }
      return;
    }

    // 只有首屏才显示全屏loading，翻页用列表底部的提示
    const firstLoad = !this.data.loaded;

    this.setData({ loading: true });

    if (firstLoad) {
      wx.showLoading({
        title: '加载中...'
      });
    }

    http.get('/questions/records/groups', { page: page, pageSize: PAGE_SIZE })
      .then(res => {
        const cards = (res.list || []).map(group => that.buildCard(group));

        that.setData({
          historyList: page === 1 ? cards : that.data.historyList.concat(cards),
          page: page,
          hasMore: !!res.hasMore,
          loading: false,
          loaded: true
        });
      })
      .catch(err => {
        console.error('加载历史记录失败:', err);

        // 首屏失败显示空状态；翻页失败保留已加载的内容，下次触底可重试
        that.setData({
          historyList: page === 1 ? [] : that.data.historyList,
          loading: false,
          loaded: true
        });
      })
      .then(() => {
        if (firstLoad) {
          wx.hideLoading();
        }
        if (fromPullDown) {
          wx.stopPullDownRefresh();
        }
      });
  },

  // 把后端聚合好的「日期+科目」分组转成卡片
  buildCard(group) {
    const totalQuestions = group.totalQuestions || 0;
    const correctCount = group.correctCount || 0;
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
      key: `${group.recordDate}_${group.subject}`, // 唯一标识
      date: group.recordDate,
      subject: group.subject,
      totalQuestions: totalQuestions,
      correctCount: correctCount,
      accuracy: accuracy,
      level: level,
      levelText: levelText,
      earnedPoints: group.earnedPoints === undefined ? 1 : group.earnedPoints
    };
  },

  viewDetail: function(e) {
    const date = e.currentTarget.dataset.date;
    const subject = e.currentTarget.dataset.subject;

    wx.navigateTo({
      url: `/pages/history-detail/history-detail?date=${date}&subject=${subject}`
    });
  }
});
