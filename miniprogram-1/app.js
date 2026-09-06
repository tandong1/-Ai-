App({
  globalData: {
    userInfo: null,
    currentUser: null,
    totalPoints: 0,
    todayCompleted: false
  },

  onLaunch: function() {
    // 启动时加载用户数据，但不强制跳转
    this.loadUserData();
  },

  checkUserLogin: function() {
    var token = wx.getStorageSync('token');
    if (!token) {
      wx.reLaunch({
        url: '/pages/user-select/user-select'
      });
      return false;
    }
    return true;
  },

  loadUserData: function() {
    // 从本地存储读取用户信息（登录时已保存）
    var currentUser = wx.getStorageSync('currentUser');
    if (currentUser) {
      this.globalData.currentUser = currentUser;
      this.globalData.totalPoints = currentUser.totalPoints || 0;
    }
  },

  saveUserData: function() {
    if (!this.globalData.currentUser) return;

    // 更新本地存储
    this.globalData.currentUser.totalPoints = this.globalData.totalPoints;
    wx.setStorageSync('currentUser', this.globalData.currentUser);
  }
});
