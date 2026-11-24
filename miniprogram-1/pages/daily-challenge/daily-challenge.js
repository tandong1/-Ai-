const http = require('../../utils/http.js');
const app = getApp();

Page({
  data: {
    scratch: { eligible: false, revealed: false },
    scratchStarted: false,
    scratchLoading: false,
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
    // 保留右上角菜单分享入口，与页面内按钮独立。
    wx.showShareMenu({
      menus: ['shareAppMessage', 'shareTimeline']
    });
    this.loadProgress();
    this.updatePoints();
    this.loadScratch();
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

    // 进度由 onShow 统一加载，这里不重复请求
  },

  loadProgress: function() {
    const that = this;

    // 先用本地缓存让界面立刻有内容（乐观显示），再用后端结果覆盖
    const cached = wx.getStorageSync(this.getProgressKey());
    if (cached) {
      this.setData({
        todayProgress: {
          math: !!cached.math,
          english: !!cached.english,
          chinese: !!cached.chinese
        }
      });
    }

    // 后端 question_records 才是权威：换设备、清缓存后也能正确显示已完成
    http.get('/questions/daily/progress', {})
      .then(progress => {
        that.setData({
          todayProgress: {
            math: !!progress.math,
            english: !!progress.english,
            chinese: !!progress.chinese
          }
        });
        wx.setStorageSync(that.getProgressKey(), that.data.todayProgress);
      })
      .catch(err => {
        console.error('加载今日进度失败:', err);
        // 失败时沿用缓存值，不要把已完成回退成未完成
      });
  },

  loadScratch: function() {
    if (this.data.scratchLoading) return;
    const version = (this.scratchVersion || 0) + 1;
    this.scratchVersion = version;
    http.get('/rewards/scratch').then(result => {
      if (this.scratchVersion !== version) return;
      const changedDay = this.data.scratch.rewardDate !== result.rewardDate;
      this.setData({ scratch: result, scratchStarted: changedDay ? false : this.data.scratchStarted });
      this.syncScratchBalance(result.totalPoints);
    }).catch(err => console.error('加载刮奖状态失败:', err));
  },

  revealScratch: function() {
    if (this.data.scratchLoading || this.data.scratch.revealed || !this.data.scratch.eligible) return;
    this.scratchVersion = (this.scratchVersion || 0) + 1;
    this.setData({ scratchStarted: true, scratchLoading: true });
    http.post('/rewards/scratch/reveal', { rewardDate: this.data.scratch.rewardDate })
      .then(result => {
        this.setData({ scratch: result });
        this.syncScratchBalance(result.totalPoints);
      })
      .catch(err => {
        console.error('揭晓刮奖失败，可安全重试:', err);
        if (err.message && err.message.indexOf('日期已更新') >= 0) this.refreshScratchAfterClaim = true;
      })
      .finally(() => {
        this.setData({ scratchLoading: false });
        if (this.refreshScratchAfterClaim) {
          this.refreshScratchAfterClaim = false;
          this.loadScratch();
        }
      });
  },

  syncScratchBalance: function(totalPoints) {
    this.setData({ totalPoints });
    app.globalData.totalPoints = totalPoints;
    const user = wx.getStorageSync('currentUser');
    if (user) {
      user.totalPoints = totalPoints;
      wx.setStorageSync('currentUser', user);
      app.globalData.currentUser = user;
    }
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

  // 缓存 key 按用户隔离：一台设备给多个孩子用时不会串号
  getProgressKey: function() {
    const userId = wx.getStorageSync('currentUserId') || 'unknown';
    const date = new Date();
    const month = ('0' + (date.getMonth() + 1)).slice(-2);
    const day = ('0' + date.getDate()).slice(-2);
    return 'challenge_' + userId + '_' + date.getFullYear() + '-' + month + '-' + day + '_progress';
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
  },

  // 分享到聊天窗：由右上角“...”菜单触发
  onShareAppMessage: function() {
    const progress = this.data.todayProgress;
    const completedCount = [progress.math, progress.english, progress.chinese]
      .filter(Boolean).length;

    const title = completedCount > 0
      ? `我在「小小学霸」今天已闯关${completedCount}科，快来一起挑战吧！`
      : '「小小学霸」每日学习挑战，快来一起闯关吧！';

    return {
      title: title,
      path: '/pages/daily-challenge/daily-challenge'
    };
  },

  // 分享到朋友圈：内容需更中性，不依赖当次答题状态
  onShareTimeline: function() {
    return {
      title: '小小学霸 - 每天进步一点点'
    };
  }
});
