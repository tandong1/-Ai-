const http = require('../../utils/http.js');
var app = getApp();

Page({
  data: {
    currentUser: {},
    totalPoints: 0,
    gifts: []
  },

  onLoad: function() {
    this.loadUserData();
    this.loadGifts();
  },

  onShow: function() {
    this.loadUserData();
  },

  loadGifts: function() {
    const that = this;

    wx.showLoading({
      title: '加载中...'
    });

    // 从后端获取礼物列表
    http.get('/gifts')
      .then(gifts => {
        wx.hideLoading();

        // 格式化礼物数据
        const formattedGifts = gifts.map(gift => ({
          id: gift.id,
          name: gift.name,
          description: gift.description,
          points: gift.points,
          image: gift.imageUrl,
          stock: gift.stock
        }));

        that.setData({
          gifts: formattedGifts
        });
      })
      .catch(err => {
        wx.hideLoading();
        console.error('加载礼物列表失败:', err);

        wx.showToast({
          title: '加载失败',
          icon: 'none'
        });
      });
  },

  loadUserData: function() {
    // 从本地存储读取最新用户信息
    var currentUser = wx.getStorageSync('currentUser');

    if (currentUser) {
      this.setData({
        currentUser: currentUser,
        totalPoints: currentUser.totalPoints || 0
      });

      // 同步到全局数据
      app.globalData.currentUser = currentUser;
      app.globalData.totalPoints = currentUser.totalPoints || 0;
    } else {
      // 如果本地没有，尝试从全局数据获取
      var currentUser = app.globalData.currentUser || { name: '小朋友', avatar: '', totalPoints: 0 };
      var totalPoints = app.globalData.totalPoints || 0;

      this.setData({
        currentUser: currentUser,
        totalPoints: totalPoints
      });
    }
  },

  viewGiftDetail: function(e) {
    var giftId = e.currentTarget.dataset.id;
    var gift = null;

    for (var i = 0; i < this.data.gifts.length; i++) {
      if (this.data.gifts[i].id === giftId) {
        gift = this.data.gifts[i];
        break;
      }
    }

    if (!gift) return;

    if (this.data.totalPoints < gift.points) {
      wx.showToast({
        title: '积分不足，继续加油学习吧！',
        icon: 'none',
        duration: 2000
      });
      return;
    }

    var that = this;
    wx.showModal({
      title: '确认兑换',
      content: '确定要用 ' + gift.points + ' 积分兑换「' + gift.name + '」吗？',
      confirmText: '确定兑换',
      cancelText: '再想想',
      success: function(res) {
        if (res.confirm) {
          that.exchangeGift(gift);
        }
      }
    });
  },

  exchangeGift: function(gift) {
    var newPoints = this.data.totalPoints - gift.points;

    app.globalData.totalPoints = newPoints;
    if (app.globalData.currentUser) {
      app.globalData.currentUser.points = newPoints;
    }
    app.saveUserData();

    var record = {
      giftId: gift.id,
      giftName: gift.name,
      points: gift.points,
      time: Date.now(),
      userId: app.globalData.currentUser ? app.globalData.currentUser.id : 0
    };

    var records = wx.getStorageSync('exchange_records') || [];
    records.unshift(record);
    wx.setStorageSync('exchange_records', records);

    this.setData({ totalPoints: newPoints });

    wx.showToast({
      title: '兑换成功！',
      icon: 'success'
    });
  },

  viewRecords: function() {
    wx.navigateTo({
      url: '/pages/exchange-records/exchange-records'
    });
  }
});
