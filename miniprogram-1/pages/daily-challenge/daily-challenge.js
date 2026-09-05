var app = getApp();

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
  },

  initPage: function() {
    var date = new Date();
    var currentDate = date.getMonth() + 1 + '月' + date.getDate() + '日';

    this.setData({
      currentDate: currentDate,
      totalPoints: app.globalData.totalPoints || 0
    });

    this.loadProgress();
  },

  loadProgress: function() {
    var todayKey = this.getTodayKey();
    var progress = wx.getStorageSync(todayKey + '_progress') || {
      math: false,
      english: false,
      chinese: false
    };

    this.setData({
      todayProgress: progress
    });
  },

  getTodayKey: function() {
    var date = new Date();
    return 'challenge_' + date.getFullYear() + '_' + (date.getMonth() + 1) + '_' + date.getDate();
  },

  goToSubject: function(e) {
    var subject = e.currentTarget.dataset.subject;
    wx.navigateTo({
      url: '/pages/subject-challenge/subject-challenge?subject=' + subject
    });
  }
});
