const app = getApp();

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
    ],

    // 成就配置
    achievementList: [
      { id: 1, name: '首战告捷', desc: '完成第一次挑战', icon: '🎯', condition: 'days', value: 1 },
      { id: 2, name: '坚持不懈', desc: '连续学习3天', icon: '🔥', condition: 'days', value: 3 },
      { id: 3, name: '学习达人', desc: '连续学习7天', icon: '💪', condition: 'days', value: 7 },
      { id: 4, name: '百题斩', desc: '累计答题100道', icon: '📝', condition: 'questions', value: 100 },
      { id: 5, name: '全对高手', desc: '单次全对', icon: '✨', condition: 'perfect', value: 1 },
      { id: 6, name: '积分大户', desc: '累计500积分', icon: '💰', condition: 'points', value: 500 }
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
    const history = wx.getStorageSync('history') || [];

    // 计算等级
    const levelInfo = this.calculateLevel(totalPoints);
    const levelProgress = this.calculateProgress(totalPoints, levelInfo);

    // 检查成就
    const achievements = this.checkAchievements(history, totalPoints);
    const unlockedCount = achievements.filter(a => a.unlocked).length;

    // 生成积分历史
    const pointsHistory = this.generatePointsHistory(history);

    this.setData({
      currentUser,
      totalPoints,
      levelInfo,
      levelProgress,
      achievements,
      unlockedCount,
      pointsHistory
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

  checkAchievements(history, totalPoints) {
    const totalDays = history.length;
    const totalQuestions = history.reduce((sum, item) => sum + item.totalQuestions, 0);
    const hasPerfect = history.some(item => item.accuracy === 100);

    return this.data.achievementList.map(achievement => {
      let unlocked = false;

      switch (achievement.condition) {
        case 'days':
          unlocked = totalDays >= achievement.value;
          break;
        case 'questions':
          unlocked = totalQuestions >= achievement.value;
          break;
        case 'perfect':
          unlocked = hasPerfect;
          break;
        case 'points':
          unlocked = totalPoints >= achievement.value;
          break;
      }

      return {
        ...achievement,
        unlocked
      };
    });
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

