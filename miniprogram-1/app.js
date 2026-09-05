App({
  globalData: {
    userInfo: null,
    currentUser: null,
    totalPoints: 0,
    todayCompleted: false
  },

  onLaunch: function() {
    this.checkUserLogin();
  },

  checkUserLogin: function() {
    var currentUserId = wx.getStorageSync('currentUserId');
    if (!currentUserId) {
      wx.reLaunch({
        url: '/pages/user-select/user-select'
      });
    } else {
      this.loadUserData();
    }
  },

  loadUserData: function() {
    var currentUserId = wx.getStorageSync('currentUserId');
    var users = wx.getStorageSync('users') || [];

    for (var i = 0; i < users.length; i++) {
      if (users[i].id === currentUserId) {
        this.globalData.currentUser = users[i];
        this.globalData.totalPoints = users[i].points || 0;
        break;
      }
    }
  },

  saveUserData: function() {
    if (!this.globalData.currentUser) return;

    var users = wx.getStorageSync('users') || [];
    for (var i = 0; i < users.length; i++) {
      if (users[i].id === this.globalData.currentUser.id) {
        users[i].points = this.globalData.totalPoints;
        break;
      }
    }

    wx.setStorageSync('users', users);
  }
});
