const app = getApp();
const http = require('../../utils/http.js');

Page({
  data: {
    currentUser: {},
    totalPoints: 0,
    levelInfo: {},
    levelProgress: 0,
    achievements: [],
    unlockedCount: 0,
    pointsHistory: [],

    // 等级配置
    levels: [
      { min: 0, max: 49, name: '初学者', icon: '🌱', desc: '刚刚开始的小萌新' },
      { min: 50, max: 149, name: '学习者', icon: '📚', desc: '正在努力学习中' },
      { min: 150, max: 299, name: '进步者', icon: '⭐', desc: '进步很快呢' },
      { min: 300, max: 499, name: '优秀者', icon: '🎯', desc: '已经很优秀了' },
      { min: 500, max: 999, name: '学霸', icon: '👑', desc: '名副其实的学霸' },
      { min: 1000, max: 9999, name: '学神', icon: '🏆', desc: '无所不能的学神' }
    ]
  },

  onLoad() {
    this.loadData();
  },

  onShow() {
    this.loadData();
  },

  loadData() {
    const currentUser = app.globalData.currentUser || { name: '小朋友', avatar: '' };
    const totalPoints = app.globalData.totalPoints || 0;

    // 计算等级
    const levelInfo = this.calculateLevel(totalPoints);
    const levelProgress = this.calculateProgress(totalPoints, levelInfo);

    // 生成积分历史
    // 说明：这里读的本地 history 已经没有任何写入方（详见 docs/SPEC_ACHIEVEMENTS_PROGRESS.md §1.1），
    // 所以积分明细目前恒为空，需要单独用 points_records 表补，不在本次范围内
    const history = wx.getStorageSync('history') || [];
    const pointsHistory = this.generatePointsHistory(history);

    this.setData({
      currentUser,
      totalPoints,
      levelInfo,
      levelProgress,
      pointsHistory
    });

    // 成就由后端判定
    this.loadAchievements();
  },

  loadAchievements() {
    const that = this;

    http.get('/users/achievements', {})
      .then(data => {
        const currentStreak = data.currentStreak || 0;

        const achievements = (data.achievements || []).map(item => ({
          ...item,
          // 解锁时间只取日期部分
          unlockedDate: item.unlockedAt ? item.unlockedAt.split('T')[0] : '',
          // 未解锁的连续类成就展示当前进度，给孩子一个够得着的目标感
          progressText: !item.unlocked && item.type.indexOf('streak_') === 0
            ? `已连续${currentStreak}天`
            : ''
        }));

        that.setData({
          achievements: achievements,
          unlockedCount: data.unlockedCount || 0
        });
      })
      .catch(err => {
        console.error('加载成就失败:', err);
        // 保留上一次的结果，不要把成就墙清空（孩子会以为成就丢了）
      });
  },

  switchUser() {
    wx.reLaunch({
      url: '/pages/user-select/user-select'
    });
  },

  goToShop() {
    wx.navigateTo({
      url: '/pages/shop/shop'
    });
  },

  calculateLevel(points) {
    for (let level of this.data.levels) {
      if (points >= level.min && points <= level.max) {
        return {
          ...level,
          nextPoints: level.max + 1
        };
      }
    }
    return this.data.levels[this.data.levels.length - 1];
  },

  calculateProgress(points, levelInfo) {
    const range = levelInfo.max - levelInfo.min + 1;
    const current = points - levelInfo.min;
    return Math.min((current / range) * 100, 100);
  },

  generatePointsHistory(history) {
    return history.slice(0, 10).map(item => ({
      icon: '✅',
      title: '完成每日挑战',
      time: item.date,
      points: item.earnedPoints,
      timestamp: item.timestamp
    }));
  }
});

