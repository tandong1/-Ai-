const http = require('../../utils/http.js');
const app = getApp();

Page({
  data: {
    users: []
  },

  onLoad: function() {
    this.loadUsers();
  },

  loadUsers: function() {
    const that = this;

    wx.showLoading({
      title: '加载中...'
    });

    // 从后端获取用户列表
    http.get('/users')
      .then(users => {
        wx.hideLoading();
        that.setData({ users: users });
      })
      .catch(err => {
        wx.hideLoading();
        console.error('加载用户失败:', err);

        // 如果后端连接失败，使用本地数据
        wx.showModal({
          title: '提示',
          content: '无法连接服务器，是否使用本地模式？',
          success: function(res) {
            if (res.confirm) {
              const localUsers = [
                {
                  id: 1,
                  name: '郭坤铭',
                  avatar: '/images/avatar1.png',
                  totalPoints: 0
                },
                {
                  id: 2,
                  name: '郭坤源',
                  avatar: '/images/avatar2.png',
                  totalPoints: 0
                }
              ];
              that.setData({ users: localUsers });
            }
          }
        });
      });
  },

  selectUser: function(e) {
    const userId = e.currentTarget.dataset.id;
    const user = this.data.users.find(u => u.id === userId);

    if (!user) {
      wx.showToast({
        title: '用户不存在',
        icon: 'none'
      });
      return;
    }

    wx.showLoading({
      title: '登录中...'
    });

    // 调用后端登录接口
    http.post('/users/login', { userId: userId })
      .then(data => {
        wx.hideLoading();

        // 保存token和用户信息
        wx.setStorageSync('token', data.token);
        wx.setStorageSync('currentUserId', userId);
        wx.setStorageSync('currentUser', data.user);

        // 更新全局数据
        app.globalData.currentUser = data.user;
        app.globalData.totalPoints = data.user.totalPoints || 0;

        wx.showToast({
          title: '登录成功',
          icon: 'success'
        });

        // 跳转到首页
        setTimeout(() => {
          wx.switchTab({
            url: '/pages/daily-challenge/daily-challenge'
          });
        }, 500);
      })
      .catch(err => {
        wx.hideLoading();
        console.error('登录失败:', err);
        wx.showToast({
          title: '登录失败',
          icon: 'none'
        });
      });
  },

  addUser: function() {
    wx.showToast({
      title: '请联系管理员添加用户',
      icon: 'none'
    });
  }
});
