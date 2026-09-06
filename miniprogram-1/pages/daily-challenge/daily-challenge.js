const http = require('../../utils/http.js');
const app = getApp();

Page({
  data: {
    currentDate: '',
    totalPoints: 0,
    todayProgress: {
      math: false,
      english: false,
      chinese: false
    }
  },

  onLoad: function() {
    this.initPage();
  },

  onShow: function() {
    this.loadProgress();
    this.updatePoints();
  },

  initPage: function() {
    const date = new Date();
    const currentDate = date.getMonth() + 1 + '月' + date.getDate() + '日';

    // 从本地存储读取用户信息
    const user = wx.getStorageSync('currentUser');
    const totalPoints = user ? user.totalPoints : 0;

    this.setData({
      currentDate: currentDate,
      totalPoints: totalPoints
    });

    // 同步到全局数据
    if (user) {
      app.globalData.currentUser = user;
      app.globalData.totalPoints = totalPoints;
    }

    this.loadProgress();
  },

  loadProgress: function() {
    const todayKey = this.getTodayKey();
    const progress = wx.getStorageSync(todayKey + '_progress') || {
      math: false,
      english: false,
      chinese: false
    };

    this.setData({
      todayProgress: progress
    });
  },

  updatePoints: function() {
    const user = wx.getStorageSync('currentUser');
    if (user && user.totalPoints !== undefined) {
      app.globalData.totalPoints = user.totalPoints;
      this.setData({
        totalPoints: user.totalPoints
      });
    }
  },

  getTodayKey: function() {
    const date = new Date();
    return 'challenge_' + date.getFullYear() + '_' + (date.getMonth() + 1) + '_' + date.getDate();
  },

  goToSubject: function(e) {
    const subject = e.currentTarget.dataset.subject;
    const subjectMap = {
      'math': 'math',
      'english': 'english',
      'chinese': 'chinese'
    };

    wx.navigateTo({
      url: '/pages/subject-challenge/subject-challenge?subject=' + subjectMap[subject]
    });
  }
});
