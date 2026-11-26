const http = require('../../utils/http.js');
var app = getApp();

Page({
  data: {
    currentUser: {},
    totalPoints: 0,
    gifts: [],
    selectedGift: null,
    exchangeForm: {},
    exchangeRequestId: '',
    submitting: false
  },

  onLoad: function() {
    this.loadUserData();
    this.loadGifts();
  },

  onShow: function() {
    this.loadUserData();
    this.refreshBalance();
    // 每次显示时重新加载礼物列表（以防库存变化）
    this.loadGifts();
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
          images: gift.images || [],
          stock: gift.stock,
          giftType: gift.giftType || 'physical'
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

  refreshBalance: function() {
    const id = this.data.currentUser.id;
    if (!id) return;
    const version = this.balanceVersion || 0;
    http.get('/gifts/balance').then(user => {
      if (version !== (this.balanceVersion || 0)) return;
      const currentUser = { ...this.data.currentUser, totalPoints: user.totalPoints };
      wx.setStorageSync('currentUser', currentUser);
      app.globalData.currentUser = currentUser;
      app.globalData.totalPoints = user.totalPoints;
      this.setData({ currentUser, totalPoints: user.totalPoints });
    }).catch(() => {});
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

    if (gift.stock === 0) {
      wx.showToast({ title: '已兑完', icon: 'none' });
      return;
    }
    this.setData({ selectedGift: gift, exchangeForm: {}, exchangeRequestId: Date.now() + '-' + Math.random().toString(36).slice(2) });
  },

  updateExchangeForm: function(e) {
    this.setData({ ['exchangeForm.' + e.currentTarget.dataset.field]: e.detail.value });
  },

  closeExchange: function() {
    if (!this.data.submitting) this.setData({ selectedGift: null, exchangeForm: {}, exchangeRequestId: '' });
  },

  exchangeGift: function() {
    if (this.data.submitting || !this.data.selectedGift) return;
    const gift = this.data.selectedGift;
    const form = this.data.exchangeForm;
    const required = gift.giftType === 'virtual'
      ? [['account', '账号'], ['gameName', '游戏名或平台名']]
      : [['recipient', '收货人'], ['phone', '联系电话'], ['address', '收货地址']];
    for (const [key, label] of required) {
      if (!String(form[key] || '').trim()) {
        wx.showToast({ title: '请填写' + label, icon: 'none' });
        return;
      }
    }
    this.balanceVersion = (this.balanceVersion || 0) + 1;
    this.setData({ submitting: true });
    http.post('/gifts/exchange', { ...form, giftId: gift.id, requestId: this.data.exchangeRequestId })
      .then(result => {
        const currentUser = { ...this.data.currentUser, totalPoints: result.totalPoints };
        app.globalData.currentUser = currentUser;
        app.globalData.totalPoints = result.totalPoints;
        wx.setStorageSync('currentUser', currentUser);
        this.setData({ totalPoints: result.totalPoints, currentUser, selectedGift: null, exchangeForm: {}, exchangeRequestId: '' });
        wx.showToast({ title: '兑换成功', icon: 'success' });
        this.loadGifts();
      })
      .catch(() => {})
      .finally(() => this.setData({ submitting: false }));
  },

  viewRecords: function() {
    wx.navigateTo({
      url: '/pages/exchange-records/exchange-records'
    });
  }
});
