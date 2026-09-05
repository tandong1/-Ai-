var app = getApp();

Page({
  data: {
    users: []
  },

  onLoad: function() {
    this.loadUsers();
  },

  loadUsers: function() {
    var users = wx.getStorageSync('users') || [
      {
        id: 1,
        name: '郭坤铭',
        avatar: '/images/avatar1.png',
        points: 0
      },
      {
        id: 2,
        name: '郭坤源',
        avatar: '/images/avatar2.png',
        points: 0
      }
    ];

    this.setData({ users: users });
  },

  selectUser: function(e) {
    var userId = e.currentTarget.dataset.id;
    var user = this.data.users.find(function(u) {
      return u.id === userId;
    });

    if (user) {
      app.globalData.currentUser = user;
      app.globalData.totalPoints = user.points;
      wx.setStorageSync('currentUserId', userId);

      wx.switchTab({
        url: '/pages/daily-challenge/daily-challenge'
      });
    }
  },

  addUser: function() {
    var that = this;
    wx.showModal({
      title: '添加用户',
      editable: true,
      placeholderText: '请输入用户名',
      success: function(res) {
        if (res.confirm && res.content) {
          var users = that.data.users;
          var newUser = {
            id: Date.now(),
            name: res.content,
            avatar: '/images/avatar' + ((users.length % 4) + 1) + '.png',
            points: 0
          };
          users.push(newUser);
          wx.setStorageSync('users', users);
          that.setData({ users: users });
        }
      }
    });
  }
});
